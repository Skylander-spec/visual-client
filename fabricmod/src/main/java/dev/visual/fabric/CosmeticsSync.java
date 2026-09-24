package dev.visual.fabric;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** Optional shared cosmetics. Minecraft access tokens only ever go to Mojang. */
public final class CosmeticsSync {
    private static final Gson JSON = new Gson();
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "Visual cosmetics sync");
        thread.setDaemon(true);
        return thread;
    });
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private static final Map<UUID, Remote> REMOTE = new HashMap<>();
    private static String token, authenticatedUuid, lastPublished;
    private static long nextPoll;
    private static volatile boolean busy;
    private static volatile String status = "ui.syncUnconfigured";
    private static String usedEndpoint;
    private static Object world;

    private record Remote(VConfig config, Identifier cape, Identifier skin, String signature) {}
    private record Download(UUID uuid, JsonObject appearance) {}
    private CosmeticsSync() {}

    public static boolean hasRemoteCompanions() {
        for (Remote remote : REMOTE.values()) if (remote.config.miniMe && remote.config.miniMePos == 3) return true;
        return false;
    }

    public static String status() { return status; }

    private static String endpoint() {
        String url = System.getProperty("visual.cosmetics.url",
                System.getenv().getOrDefault("VISUAL_COSMETICS_URL", "")).strip().replaceAll("/+$", "");
        if (url.isEmpty()) return "";
        URI uri = URI.create(url);
        boolean local = Set.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost());
        if ((!"https".equals(uri.getScheme()) && !(local && "http".equals(uri.getScheme())))
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("Cosmetics endpoint requires HTTPS");
        return url;
    }

    public static void tick(MinecraftClient mc) {
        if (world != mc.world) { clear(mc); world = mc.world; }
        if (mc.world == null || mc.player == null || busy || System.nanoTime() < nextPoll) return;
        nextPoll = System.nanoTime() + 5_000_000_000L;
        final String url;
        try { url = endpoint(); } catch (Exception error) { status = "ui.syncUnavailable"; return; }
        if (url.isEmpty()) { status = "ui.syncUnconfigured"; return; }
        // Snapshot game-owned state before starting background work.
        Object requestedWorld = mc.world;
        String uuid = mc.getSession().getUuidOrNull().toString().replace("-", "");
        String username = mc.getSession().getUsername();
        String accessToken = mc.getSession().getAccessToken();
        if (accessToken == null || accessToken.length() < 20) { status = "ui.syncLogin"; return; }
        List<UUID> nearby = mc.world.getPlayers().stream().map(p -> p.getUuid())
                .filter(id -> !id.equals(mc.player.getUuid())).limit(32).toList();
        REMOTE.keySet().removeIf(id -> {
            if (nearby.contains(id)) return false;
            release(mc, REMOTE.get(id)); return true;
        });
        JsonObject appearance = new JsonObject();
        VConfig config = VConfig.get();
        appearance.addProperty("miniMe", config.miniMe);
        appearance.addProperty("miniMeArt", config.miniMeArt);
        appearance.addProperty("miniMePos", config.miniMePos);
        appearance.addProperty("miniMeSize", config.miniMeSize);
        appearance.addProperty("miniMeSitzt", config.miniMeSitzt);
        appearance.addProperty("miniMeHut", config.miniMeHut);
        appearance.addProperty("miniMeFluegel", config.miniMeFluegel);
        appearance.addProperty("miniMeFlight", config.miniMeFlight);
        appearance.addProperty("miniMeScarf", config.miniMeScarf);
        appearance.addProperty("miniMeShoes", config.miniMeShoes);
        String skinName = config.miniMeSkin;
        busy = true;
        IO.execute(() -> {
            try {
                if (!url.equals(usedEndpoint) || !uuid.equals(authenticatedUuid)) {
                    token = null; lastPublished = null; usedEndpoint = url;
                }
                if (token == null) {
                    status = "ui.syncConnecting";
                    String challenge = request(url + "/challenge?uuid=" + uuid, "GET", null, null)
                            .get("serverId").getAsString();
                    JsonObject join = new JsonObject();
                    join.addProperty("accessToken", accessToken);
                    join.addProperty("selectedProfile", uuid);
                    join.addProperty("serverId", challenge);
                    request("https://sessionserver.mojang.com/session/minecraft/join", "POST", join, null);
                    JsonObject login = new JsonObject();
                    login.addProperty("uuid", uuid); login.addProperty("username", username);
                    login.addProperty("serverId", challenge);
                    token = request(url + "/session", "POST", login, null).get("token").getAsString();
                    authenticatedUuid = uuid;
                }
                var data = CapeManager.datenOrdner();
                if (data != null) addImage(appearance, "cape", data.resolve("cosmetics/cape.png"));
                if (skinName != null && !skinName.isBlank()) addImage(appearance, "skin", MiniSkin.datei(skinName));
                String signature = appearance.toString();
                if (!signature.equals(lastPublished)) {
                    request(url + "/appearance", "PUT", appearance, token);
                    lastPublished = signature;
                }
                List<Download> downloads = new ArrayList<>();
                if (!nearby.isEmpty()) {
                    String ids = String.join(",", nearby.stream().map(id -> id.toString().replace("-", "")).toList());
                    JsonObject found = request(url + "/appearances?ids=" + ids, "GET", null, null);
                    for (UUID id : nearby) {
                        var item = found.get(id.toString().replace("-", ""));
                        downloads.add(new Download(id, item != null && item.isJsonObject() ? item.getAsJsonObject() : null));
                    }
                }
                mc.execute(() -> {
                    if (mc.world != requestedWorld || !uuid.equals(mc.getSession().getUuidOrNull().toString().replace("-", ""))) return;
                    for (Download download : downloads) apply(mc, download);
                });
                status = "ui.syncConnected";
            } catch (Exception error) {
                status = "ui.syncUnavailable";
                // No access token or profile data in logs. Retrying a bad/expired session is safe.
            } finally { busy = false; }
        });
    }

    private static void addImage(JsonObject body, String key, java.nio.file.Path path) throws java.io.IOException {
        if (path != null && Files.isRegularFile(path) && Files.size(path) <= 256_000)
            body.addProperty(key, Base64.getEncoder().encodeToString(Files.readAllBytes(path)));
    }

    private static JsonObject request(String url, String method, JsonObject body, String bearer) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10));
        if (bearer != null) builder.header("Authorization", "Bearer " + bearer);
        if (body != null) builder.header("Content-Type", "application/json");
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body.toString()));
        HttpResponse<String> response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401) { token = null; lastPublished = null; }
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new java.io.IOException("Cosmetics HTTP " + response.statusCode());
        if (response.body().length() > 24_000_000) throw new java.io.IOException("Cosmetics response too large");
        return response.body().isBlank() ? new JsonObject() : JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static void apply(MinecraftClient mc, Download download) {
        Remote old = REMOTE.get(download.uuid);
        if (download.appearance == null) {
            release(mc, old); REMOTE.remove(download.uuid); return;
        }
        String signature = download.appearance.toString();
        if (old != null && signature.equals(old.signature)) return;
        try {
            VConfig config = JSON.fromJson(download.appearance, VConfig.class);
            if (config.miniMeArt < 0 || config.miniMeArt >= Begleiter.NAMEN.length || config.miniMePos < 0 || config.miniMePos > 3
                    || config.miniMeSize < 15 || config.miniMeSize > 80 || config.miniMeHut < 0 || config.miniMeHut >= MiniMe.HUT_NAMEN.length
                    || config.miniMeFlight < 0 || config.miniMeFlight > 2
                    || config.miniMeScarf < 0 || config.miniMeScarf > 3 || config.miniMeShoes < 0 || config.miniMeShoes > 3) return;
            release(mc, old);
            Identifier cape = texture(mc, download, "cape");
            Identifier skin = texture(mc, download, "skin");
            REMOTE.put(download.uuid, new Remote(config, cape, skin, signature));
        } catch (Exception error) { REMOTE.remove(download.uuid); }
    }

    private static Identifier texture(MinecraftClient mc, Download download, String key) throws Exception {
        var value = download.appearance.get(key);
        if (value == null || value.isJsonNull()) return null;
        String encoded = value.getAsString();
        if (encoded.length() > 342_000) return null;
        byte[] bytes = Base64.getDecoder().decode(encoded);
        if (bytes.length < 24) return null;
        java.nio.ByteBuffer header = java.nio.ByteBuffer.wrap(bytes);
        if (header.getLong() != 0x89504e470d0a1a0aL) return null;
        int w = header.getInt(16), h = header.getInt(20);
        if (w <= 0 || h <= 0 || w > 1024 || h > 4096 || (long) w * h > 1_048_576) return null;
        NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
        Identifier id = Identifier.of("visualsfabric", "remote/" + download.uuid + "/" + key);
        Compat.registerTexture(mc, id, image);
        return id;
    }

    public static VConfig config(UUID uuid) {
        var mc = MinecraftClient.getInstance();
        if (mc.player != null && uuid.equals(mc.player.getUuid())) return VConfig.get();
        Remote remote = REMOTE.get(uuid);
        return remote == null ? null : remote.config;
    }
    public static Identifier cape(UUID uuid) { Remote r = REMOTE.get(uuid); return r == null ? null : r.cape; }
    public static Identifier skin(UUID uuid) { Remote r = REMOTE.get(uuid); return r == null ? null : r.skin; }
    private static void release(MinecraftClient mc, Remote remote) {
        if (remote == null) return;
        if (remote.cape != null) mc.getTextureManager().destroyTexture(remote.cape);
        if (remote.skin != null) mc.getTextureManager().destroyTexture(remote.skin);
    }
    private static void clear(MinecraftClient mc) {
        REMOTE.values().forEach(r -> release(mc, r)); REMOTE.clear();
    }
}
