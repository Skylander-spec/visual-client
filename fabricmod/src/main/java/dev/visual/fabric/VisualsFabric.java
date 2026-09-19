package dev.visual.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;

public class VisualsFabric implements ClientModInitializer {
    public static KeyBinding MENU_KEY;
    private boolean menuKeyDown = false;

    @Override
    public void onInitializeClient() {
        VConfig.load();
        // Registrierung liegt in Compat — die KeyBinding-Kategorie unterscheidet
        // sich zwischen 1.21.4 (String) und 1.21.9+ (KeyBinding.Category).
        // Sie dient nur der Anzeige in den Steuerungs-Einstellungen; abgefragt
        // wird die Taste direkt (s. u.).
        MENU_KEY = Compat.registerMenuKey();

        // Hitmarker: Fabric-Event statt Mixin — feuert clientseitig beim Angriff.
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (player == mc.player) {
                CombatOverlay.onAttack(mc);
                VisualsExtras.hitParticles(mc, entity);
            }
            return ActionResult.PASS;
        });

        // Knopf im Pause-Menü — so bleibt das Client-Menü erreichbar, auch wenn
        // Rechts-Shift abgeschaltet ist. Über das Fabric-Screen-Event, damit
        // kein weiterer Mixin nötig ist.
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof GameMenuScreen)) return;
            var knoepfe = Screens.getButtons(screen);
            // Unter allem einsortieren, was schon da ist. Eine feste Höhe lag
            // genau auf „Speichern und Titelbildschirm" — und wer andere Mods
            // im Pause-Menü hat, bekommt dort noch mehr Reihen.
            int unten = knoepfe.stream()
                    .mapToInt(k -> k.getY() + k.getHeight())
                    .max().orElse(h / 4 + 120);
            int y = Math.min(unten + 6, h - 28);
            knoepfe.add(ButtonWidget
                    .builder(Text.literal("✦ Visual Client"), b ->
                            client.setScreen(new dev.visual.fabric.ui.VisualHomeScreen()))
                    .dimensions(w / 2 - 102, y, 204, 20)
                    .build());
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            handleMenuKey(client);
            FakePlayerManager.tick(client);
            ClickTracker.tick(client);
            VisualsExtras.tick(client);
            applyFullbright(client);
        });
    }

    /**
     * Taste direkt über GLFW abfragen statt über KeyBinding.wasPressed():
     * so stört eine doppelte Belegung durch andere Mods nicht, und das Menü
     * lässt sich auch im Titelbildschirm und im Pause-Menü öffnen (dort
     * liefert das KeyBinding-System gar keine Ereignisse).
     */
    private void handleMenuKey(MinecraftClient client) {
        if (client.getWindow() == null) return;
        int key = KeyBindingHelper.getBoundKeyOf(MENU_KEY).getCode();
        if (key < 0) return; // nicht belegt
        boolean down = Compat.isKeyDown(client, key);
        boolean pressed = down && !menuKeyDown;
        menuKeyDown = down;
        if (!pressed || !VConfig.get().menuKey) return;
        if (client.currentScreen instanceof dev.visual.fabric.ui.VisualHomeScreen) {
            client.setScreen(null);
        } else if (client.currentScreen == null
                || client.currentScreen instanceof TitleScreen
                || client.currentScreen instanceof dev.visual.fabric.ui.VisualTitleScreen
                || client.currentScreen instanceof GameMenuScreen) {
            client.setScreen(new dev.visual.fabric.ui.VisualHomeScreen());
        }
    }

    private double savedGamma = -1;

    private void applyFullbright(net.minecraft.client.MinecraftClient client) {
        var gamma = client.options.getGamma();
        if (VConfig.get().fullbright) {
            if (savedGamma < 0) savedGamma = gamma.getValue();
            if (gamma.getValue() < 1.0) gamma.setValue(1.0);
        } else if (savedGamma >= 0) {
            gamma.setValue(savedGamma);
            savedGamma = -1;
        }
    }
}
