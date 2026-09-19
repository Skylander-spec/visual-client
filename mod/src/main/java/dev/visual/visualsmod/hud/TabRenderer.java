package dev.visual.visualsmod.hud;

import dev.visual.visualsmod.config.VisualsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Kompakte Tabliste: dunkle Panels, Ping als Balken statt Zahl,
 * max. 4 Spalten, alphabetisch sortiert.
 */
public class TabRenderer {
    private static final int BG = 0xC0060A0F;
    private static final int BG_HEAD = 0xE00B1620;
    private static final int COL_W = 110;
    private static final int ROW_H = 11;

    @SubscribeEvent
    public void onOverlay(RenderGuiOverlayEvent.Pre event) {
        VisualsConfig c = VisualsConfig.INSTANCE;
        if (!c.tabRestyle) return;
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_LIST.id())) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        if (!mc.options.keyPlayerList.isDown() || mc.isLocalServer()) return;
        event.setCanceled(true);
        draw(event.getGuiGraphics(), mc);
    }

    private void draw(GuiGraphics g, Minecraft mc) {
        List<PlayerInfo> players = new ArrayList<>(mc.getConnection().getListedOnlinePlayers());
        players.sort(Comparator.comparing(p -> p.getProfile().getName().toLowerCase()));

        int cols = Math.min(4, 1 + (players.size() - 1) / 20);
        int rows = (int) Math.ceil(players.size() / (double) cols);
        int width = cols * COL_W + (cols - 1) * 2;
        int x0 = (g.guiWidth() - width) / 2;
        int y0 = 10;

        String header = mc.getCurrentServer() != null
                ? mc.getCurrentServer().name + "  ·  " + players.size() + " Spieler"
                : players.size() + " Spieler";
        g.fill(x0, y0, x0 + width, y0 + 14, BG_HEAD);
        g.fill(x0, y0 + 13, x0 + width, y0 + 14, 0xFF22D3EE);
        g.drawCenteredString(mc.font, header, x0 + width / 2, y0 + 3, 0xFF7DEBFA);

        for (int i = 0; i < players.size(); i++) {
            PlayerInfo p = players.get(i);
            int col = i / rows;
            int row = i % rows;
            int x = x0 + col * (COL_W + 2);
            int y = y0 + 16 + row * ROW_H;
            g.fill(x, y - 1, x + COL_W, y + ROW_H - 1, BG);
            String name = p.getProfile().getName();
            boolean self = mc.player != null && name.equals(mc.player.getGameProfile().getName());
            g.drawString(mc.font, name, x + 3, y + 1, self ? 0xFF22D3EE : 0xFFE2E8F0, false);
            drawPing(g, x + COL_W - 13, y, p.getLatency());
        }
    }

    private void drawPing(GuiGraphics g, int x, int y, int latency) {
        int bars = latency < 0 ? 0 : latency < 60 ? 4 : latency < 120 ? 3 : latency < 250 ? 2 : 1;
        int col = bars >= 3 ? 0xFF34D399 : bars == 2 ? 0xFFFACC15 : 0xFFF45568;
        for (int b = 0; b < 4; b++) {
            int h = 2 + b * 2;
            int c = b < bars ? col : 0xFF2A3644;
            g.fill(x + b * 3, y + 8 - h, x + b * 3 + 2, y + 8, c);
        }
    }
}
