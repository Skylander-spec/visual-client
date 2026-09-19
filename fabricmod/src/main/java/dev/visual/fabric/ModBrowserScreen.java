package dev.visual.fabric;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.visual.fabric.ui.VButton;
import dev.visual.fabric.ui.VStyle;
import dev.visual.fabric.ui.VText;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * In-Game-Modrinth-Browser im Visual-Client-Look: Mods & Resource Packs
 * direkt im Spiel suchen und installieren.
 */
public class ModBrowserScreen extends Screen {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String API = "https://api.modrinth.com/v2";

    private static final int PANEL_W = 440;
    private static final int PANEL_H = 262;

    private final Screen parent;
    private final String projectType;

    private TextFieldWidget searchField;
    private final List<Entry> results = new ArrayList<>();
    private String status = "";
    private int px;
    private int py;

    private record Entry(String projectId, String title, String downloads, String iconUrl) {
    }

    public ModBrowserScreen(Screen parent, String projectType) {
        super(Text.literal(VText.t("ui.modbrowser")));
        this.parent = parent;
        this.projectType = projectType;
    }

    @Override
    protected void init() {
        px = (this.width - PANEL_W) / 2;
        py = (this.height - PANEL_H) / 2;

        searchField = new TextFieldWidget(this.textRenderer, px + 14, py + 30, PANEL_W - 108, 18,
                Text.literal("Suche"));
        searchField.setMaxLength(64);
        searchField.setDrawsBackground(false);
        searchField.setX(px + 18);
        searchField.setY(py + 35);
        addDrawableChild(searchField);
        setInitialFocus(searchField);

        addDrawableChild(new VButton(px + PANEL_W - 82, py + 30, 68, 18,
                Text.literal("Suchen"), this::search).primary());

        int y = py + 60;
        for (int i = 0; i < Math.min(results.size(), 7); i++) {
            final Entry e = results.get(i);
            addDrawableChild(new VButton(px + PANEL_W - 90, y + i * 25 + 2, 76, 18,
                    Text.literal("Installieren"), () -> install(e)));
        }

        addDrawableChild(new VButton(px + PANEL_W - 68, py + 8, 60, 16,
                Text.literal(VText.t("ui.back")), () -> MinecraftClient.getInstance().setScreen(parent)));

        if (results.isEmpty() && status.isEmpty()) search();
    }

    private String mcVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("");
    }

    private void search() {
        String q = searchField == null ? "" : searchField.getText().trim();
        status = VText.t("ui.searching");
        CompletableFuture.runAsync(() -> {
            try {
                String facets = projectType.equals("mod")
                        ? "[[\"categories:fabric\"],[\"versions:" + mcVersion() + "\"],[\"project_type:mod\"]]"
                        : "[[\"versions:" + mcVersion() + "\"],[\"project_type:resourcepack\"]]";
                String url = API + "/search?query=" + URLEncoder.encode(q, StandardCharsets.UTF_8)
                        + "&facets=" + URLEncoder.encode(facets, StandardCharsets.UTF_8)
                        + "&limit=7&index=" + (q.isEmpty() ? "downloads" : "relevance");
                HttpResponse<String> res = HTTP.send(
                        HttpRequest.newBuilder(URI.create(url))
                                .header("User-Agent", "visual-client-ingame/1.0").build(),
                        HttpResponse.BodyHandlers.ofString());
                JsonArray hits = JsonParser.parseString(res.body()).getAsJsonObject()
                        .getAsJsonArray("hits");
                List<Entry> parsed = new ArrayList<>();
                for (int i = 0; i < hits.size(); i++) {
                    JsonObject h = hits.get(i).getAsJsonObject();
                    long dl = h.get("downloads").getAsLong();
                    String dls = dl >= 1_000_000 ? (dl / 1_000_000) + " M" : (dl / 1_000) + " k";
                    // icon_url fehlt bei Projekten ohne Bild und ist dann JsonNull
                    String bild = h.has("icon_url") && !h.get("icon_url").isJsonNull()
                            ? h.get("icon_url").getAsString() : null;
                    parsed.add(new Entry(h.get("project_id").getAsString(),
                            h.get("title").getAsString(), dls, bild));
                }
                MinecraftClient.getInstance().execute(() -> {
                    results.clear();
                    results.addAll(parsed);
                    status = parsed.isEmpty() ? VText.t("ui.nohits") : "";
                    clearAndInit();
                });
            } catch (Exception ex) {
                MinecraftClient.getInstance().execute(() -> status = VText.t("ui.modrinthdown"));
            }
        });
    }

    private void install(Entry e) {
        status = VText.t("ui.loadingname", e.title());
        CompletableFuture.runAsync(() -> {
            try {
                String url = API + "/project/" + e.projectId() + "/version?game_versions="
                        + URLEncoder.encode("[\"" + mcVersion() + "\"]", StandardCharsets.UTF_8)
                        + (projectType.equals("mod")
                        ? "&loaders=" + URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8)
                        : "");
                HttpResponse<String> res = HTTP.send(
                        HttpRequest.newBuilder(URI.create(url))
                                .header("User-Agent", "visual-client-ingame/1.0").build(),
                        HttpResponse.BodyHandlers.ofString());
                JsonArray versions = JsonParser.parseString(res.body()).getAsJsonArray();
                if (versions.isEmpty()) {
                    MinecraftClient.getInstance().execute(() ->
                            status = VText.t("ui.noversion", mcVersion()));
                    return;
                }
                JsonArray files = versions.get(0).getAsJsonObject().getAsJsonArray("files");
                JsonObject file = files.get(0).getAsJsonObject();
                for (int i = 0; i < files.size(); i++) {
                    if (files.get(i).getAsJsonObject().get("primary").getAsBoolean()) {
                        file = files.get(i).getAsJsonObject();
                        break;
                    }
                }
                String dlUrl = file.get("url").getAsString();
                String fileName = file.get("filename").getAsString();
                Path folder = FabricLoader.getInstance().getGameDir()
                        .resolve(projectType.equals("mod") ? "mods" : "resourcepacks");
                Files.createDirectories(folder);
                byte[] bytes = HTTP.send(
                        HttpRequest.newBuilder(URI.create(dlUrl))
                                .header("User-Agent", "visual-client-ingame/1.0").build(),
                        HttpResponse.BodyHandlers.ofByteArray()).body();
                Files.write(folder.resolve(fileName), bytes);
                MinecraftClient.getInstance().execute(() -> {
                    String nachsatz = projectType.equals("mod")
                            ? VText.t("ui.afterrestart")
                            : VText.t(einschalten(fileName) ? "ui.packactive" : "ui.inpacklist");
                    status = VText.t("ui.installedname", e.title()) + nachsatz;
                });
            } catch (Exception ex) {
                MinecraftClient.getInstance().execute(() ->
                        status = VText.t("ui.downloadfailed"));
            }
        });
    }

    /**
     * Ein frisch geladenes Paket gleich einschalten. Sonst läge es zwar im
     * Ordner, der Spieler müsste es aber in den Optionen erst heraussuchen —
     * und genau das soll der Browser abnehmen. Minecraft führt Pakete aus
     * dem Ordner unter {@code file/<Dateiname>}.
     *
     * {@code refreshResourcePacks} schreibt die Optionen und stößt selbst den
     * Neuaufbau der Ressourcen an; mehr ist nicht nötig.
     */
    private static boolean einschalten(String dateiName) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            var verwaltung = mc.getResourcePackManager();
            verwaltung.scanPacks();
            java.util.List<String> an = new java.util.ArrayList<>(verwaltung.getEnabledIds());
            String id = "file/" + dateiName;
            if (!an.contains(id)) an.add(id);
            verwaltung.setEnabledProfiles(an);
            mc.options.refreshResourcePacks(verwaltung);
            return true;
        } catch (Throwable t) {
            // Klappt es nicht, steht das Paket wenigstens zur Auswahl bereit
            return false;
        }
    }

    /** Ruhige, aber je Name andere Farbe für die Ersatzkachel. */
    private static int farbeAus(String name) {
        int h = name.hashCode();
        return 0xFF000000 | ((70 + Math.abs(h % 110)) << 16)
                | ((70 + Math.abs((h >> 8) % 110)) << 8) | (110 + Math.abs((h >> 16) % 100));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x99020606);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        VStyle.panel(context, px, py, PANEL_W, PANEL_H);

        // Kopfzeile
        context.drawTextWithShadow(this.textRenderer, "VISUAL", px + 12, py + 10, 0xFFFFFFFF);
        int wWidth = this.textRenderer.getWidth("VISUAL");
        context.drawTextWithShadow(this.textRenderer, "CLIENT", px + 12 + wWidth, py + 10, VStyle.ACCENT);
        context.drawTextWithShadow(this.textRenderer,
                "§8" + VText.t(projectType.equals("mod") ? "ui.head.mods" : "ui.head.packs")
                        + " · " + mcVersion(),
                px + 12 + wWidth + this.textRenderer.getWidth("CLIENT") + 8, py + 10, VStyle.TEXT_FAINT);

        // Suchfeld-Rahmen
        VStyle.card(context, px + 14, py + 30, PANEL_W - 108, 18, false);

        // Ergebnis-Karten
        int y = py + 60;
        for (int i = 0; i < Math.min(results.size(), 7); i++) {
            Entry e = results.get(i);
            boolean hov = mouseX >= px + 14 && mouseX <= px + PANEL_W - 14
                    && mouseY >= y + i * 25 && mouseY < y + i * 25 + 22;
            VStyle.card(context, px + 14, y + i * 25, PANEL_W - 28, 22, hov);

            // Vorschaubild links; solange keins da ist, eine Kachel aus dem
            // Anfangsbuchstaben — so springt die Zeile beim Nachladen nicht.
            ProjektBild.anfordern(e.projectId(), e.iconUrl());
            int bx = px + 17;
            int by = y + i * 25 + 3;
            var bild = ProjektBild.textur(e.projectId());
            if (bild != null) {
                Compat.drawTex(context, bild, bx, by, 16, 16, 16, 16, 0xFFFFFFFF);
            } else {
                VStyle.roundRect(context, bx, by, 16, 16, VStyle.R_XS, farbeAus(e.title()));
                String b = e.title().isEmpty() ? "?" : e.title().substring(0, 1).toUpperCase();
                context.drawText(this.textRenderer, b,
                        bx + (16 - this.textRenderer.getWidth(b)) / 2, by + 4, 0xFFFFFFFF, false);
            }

            String title = this.textRenderer.trimToWidth(e.title(), PANEL_W - 220);
            context.drawTextWithShadow(this.textRenderer, title, px + 38, y + i * 25 + 7,
                    VStyle.TEXT);
            context.drawTextWithShadow(this.textRenderer, "§8" + e.downloads(),
                    px + PANEL_W - 140, y + i * 25 + 7, VStyle.TEXT_FAINT);
        }

        super.render(context, mouseX, mouseY, delta);

        if (!status.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, status, px + 14,
                    py + PANEL_H - 16, VStyle.TEXT_DIM);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
