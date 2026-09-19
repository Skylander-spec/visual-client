package dev.visual.legacy;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Visual Client fuer Minecraft 1.8.9.
 *
 * Bewusst eigenstaendig gehalten: 1.8.9 hat weder DrawContext noch die
 * modernen Render-States, deshalb teilt sich diese Fassung mit dem 1.21er
 * Mod nur das Aussehen, nicht den Quelltext.
 */
public class VisualsLegacy implements ClientModInitializer {
    public static final List<HudElement> ELEMENTS = new ArrayList<HudElement>();

    @Override
    public void onInitializeClient() {
        VConfig189.load();
        registerElements();
    }

    private static void registerElements() {
        ELEMENTS.add(new HudElement("fps", "hud.fps", 12, 20, new HudElement.TextSource() {
            public String get() {
                return MinecraftClient.getCurrentFps() + " FPS";
            }
        }));

        ELEMENTS.add(new HudElement("cps", "hud.cps", 12, 60, new HudElement.TextSource() {
            public String get() {
                return ClickTracker189.left() + " | " + ClickTracker189.right() + " CPS";
            }
        }));

        ELEMENTS.add(new HudElement("koordinaten", "hud.coords", 12, 100, new HudElement.TextSource() {
            public String get() {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null) return "—";
                return String.format("%.0f, %.0f, %.0f", mc.player.x, mc.player.y, mc.player.z);
            }
        }));

        ELEMENTS.add(new HudElement("ping", "hud.ping", 12, 140, new HudElement.TextSource() {
            public String get() {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null || mc.player.networkHandler == null) return "—";
                PlayerListEntry e = mc.player.networkHandler
                        .getPlayerListEntry(mc.player.getUuid());
                return e == null ? "— ms" : e.getLatency() + " ms";
            }
        }));
    }

    /** Baustein per Kennung finden (fuer die Tastenanzeige). */
    public static HudElement byId(String id) {
        for (HudElement e : ELEMENTS) {
            if (e.id.equals(id)) return e;
        }
        return null;
    }
}
