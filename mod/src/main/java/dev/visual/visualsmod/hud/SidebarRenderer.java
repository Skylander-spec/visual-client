package dev.visual.visualsmod.hud;

import dev.visual.visualsmod.config.VisualsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ersetzt die Vanilla-Sidebar durch eine kompakte, transparente Version —
 * optional ohne die roten Punktzahlen.
 */
public class SidebarRenderer {
    private static final int BG = 0x90060A0F;
    private static final int BG_TITLE = 0xC00B1620;
    private static final int ACCENT = 0xFF22D3EE;

    @SubscribeEvent
    public void onOverlay(RenderGuiOverlayEvent.Pre event) {
        VisualsConfig c = VisualsConfig.INSTANCE;
        if (!c.sidebarRestyle) return;
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.SCOREBOARD.id())) return;
        event.setCanceled(true);

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Scoreboard sb = mc.level.getScoreboard();
        Objective obj = sb.getDisplayObjective(1);
        if (obj == null) return;
        draw(event.getGuiGraphics(), mc, sb, obj, c);
    }

    private void draw(GuiGraphics g, Minecraft mc, Scoreboard sb, Objective obj, VisualsConfig c) {
        List<Score> scores = new ArrayList<>(sb.getPlayerScores(obj));
        scores.removeIf(s -> s.getOwner() == null || s.getOwner().startsWith("#"));
        scores.sort(Comparator.comparingInt(Score::getScore).reversed());
        if (scores.size() > 15) scores = scores.subList(0, 15);

        Component title = obj.getDisplayName();
        int width = mc.font.width(title) + 12;
        List<Component> lines = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (Score s : scores) {
            PlayerTeam team = sb.getPlayersTeam(s.getOwner());
            Component line = PlayerTeam.formatNameForTeam(team, Component.literal(s.getOwner()));
            lines.add(line);
            values.add(String.valueOf(s.getScore()));
            int w = mc.font.width(line) + (c.hideSidebarNumbers ? 12 : mc.font.width(values.get(values.size() - 1)) + 18);
            width = Math.max(width, w);
        }

        int rowH = 11;
        int height = 14 + lines.size() * rowH;
        int x = g.guiWidth() - width - 4;
        int y = (g.guiHeight() - height) / 2;

        g.fill(x, y, x + width, y + 13, BG_TITLE);
        g.fill(x, y + 13, x + width, y + height, BG);
        g.fill(x - 1, y, x, y + height, ACCENT);
        g.drawString(mc.font, title, x + (width - mc.font.width(title)) / 2, y + 3, 0xFF7DEBFA, false);

        int ly = y + 15;
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(mc.font, lines.get(i), x + 4, ly, 0xFFE2E8F0, false);
            if (!c.hideSidebarNumbers) {
                String v = values.get(i);
                g.drawString(mc.font, v, x + width - mc.font.width(v) - 4, ly, ACCENT, false);
            }
            ly += rowH;
        }
    }
}
