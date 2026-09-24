package dev.visual.fabric.ui;

import dev.visual.fabric.OneConfigBruecke;
import net.minecraft.client.gui.screen.Screen;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** OneConfig 1.0.x disposes its Compose scene permanently when a screen is removed. */
public final class ScreenLifecycle {
    private static final Set<Screen> removed = Collections.newSetFromMap(new WeakHashMap<>());

    private ScreenLifecycle() {}

    public static Screen prepare(Screen current, Screen requested) {
        if (current == requested) return requested;
        if (managed(current)) removed.add(current);
        if (requested == null || !removed.contains(requested)) return requested;
        return switch (requested.getClass().getName()) {
            case "dev.visual.fabric.ui.VTitleScreen" -> VisualTitleScreen.oeffnen();
            case "dev.visual.fabric.ui.VModulesScreen" -> VisualHomeScreen.eigeneModule();
            case "dev.visual.fabric.ui.VCosmeticsScreen" -> VisualCosmeticsScreen.oeffnen();
            case "org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen" -> {
                Screen fresh = OneConfigBruecke.bildschirm();
                yield fresh != null ? fresh : VisualHomeScreen.eigeneModule();
            }
            default -> requested;
        };
    }

    private static boolean managed(Screen screen) {
        if (screen == null) return false;
        return switch (screen.getClass().getName()) {
            case "dev.visual.fabric.ui.VTitleScreen", "dev.visual.fabric.ui.VModulesScreen",
                 "dev.visual.fabric.ui.VCosmeticsScreen",
                 "org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen" -> true;
            default -> false;
        };
    }
}
