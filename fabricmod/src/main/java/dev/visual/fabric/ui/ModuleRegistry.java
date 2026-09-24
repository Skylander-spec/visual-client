package dev.visual.fabric.ui;

import dev.visual.fabric.FakePlayerManager;
import dev.visual.fabric.HudEditorScreen;
import dev.visual.fabric.MiniMe;
import dev.visual.fabric.MiniSkin;
import dev.visual.fabric.ModBrowserScreen;
import dev.visual.fabric.VConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Arm;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Baut die Modulliste für die Übersicht aus der Config. */
public final class ModuleRegistry {
    private static final String[] CROSSHAIR_STYLES =
            {"val.cross", "val.dot", "val.circle"};
    private static final String[] TIME_MODES =
            {"val.off", "val.day", "val.sunset", "val.night"};
    private static final int[] COLORS =
            {0x22D3EE, 0xF45568, 0x34D399, 0xFACC15, 0xC084FC, 0xFFFFFF};
    private static final String[] COLOR_NAMES =
            {"val.cyan", "val.red", "val.green", "val.yellow", "val.purple", "val.white"};
    private static final String[] MINI_POS =
            {"val.head", "val.leftshoulder", "val.rightshoulder", "val.follow"};
    private static final String[] PIECES =
            {"val.helmet", "val.chest", "val.legs", "val.boots"};

    private ModuleRegistry() {
    }

    public static List<VModule> all(Screen parent) {
        VConfig c = VConfig.get();
        List<VModule> list = new ArrayList<>();

        list.add(new VModule("crosshair", "✛", VText.t("mod.crosshair"),
                VText.t("mod.crosshair.d"),
                () -> c.crosshair, v -> set(() -> c.crosshair = v))
                .add(new VSetting.Stepper(VText.t("set.style"),
                        () -> VText.t(CROSSHAIR_STYLES[c.crosshairStyle]),
                        () -> set(() -> c.crosshairStyle =
                                (c.crosshairStyle + CROSSHAIR_STYLES.length - 1) % CROSSHAIR_STYLES.length),
                        () -> set(() -> c.crosshairStyle =
                                (c.crosshairStyle + 1) % CROSSHAIR_STYLES.length)))
                .add(new VSetting.Stepper(VText.t("set.size"), () -> String.valueOf(c.crosshairSize),
                        () -> set(() -> c.crosshairSize = Math.max(1, c.crosshairSize - 1)),
                        () -> set(() -> c.crosshairSize = Math.min(12, c.crosshairSize + 1))))
                .add(new VSetting.Stepper(VText.t("set.gap"), () -> String.valueOf(c.crosshairGap),
                        () -> set(() -> c.crosshairGap = Math.max(0, c.crosshairGap - 1)),
                        () -> set(() -> c.crosshairGap = Math.min(8, c.crosshairGap + 1))))
                .add(new VSetting.Stepper(VText.t("set.color"),
                        () -> VText.t(COLOR_NAMES[colorIndex(c.crosshairColor)]),
                        () -> set(() -> c.crosshairColor =
                                COLORS[(colorIndex(c.crosshairColor) + COLORS.length - 1) % COLORS.length]),
                        () -> set(() -> c.crosshairColor =
                                COLORS[(colorIndex(c.crosshairColor) + 1) % COLORS.length])))
                .add(new VSetting.Toggle(VText.t("set.outline"), () -> c.crosshairOutline,
                        v -> set(() -> c.crosshairOutline = v))));

        list.add(new VModule("armor", "⛨", VText.t("mod.armor"),
                VText.t("mod.armor.d"),
                null, null)
                .add(new VSetting.Toggle(VText.t("set.armor.self", VText.t(PIECES[0])), () -> c.armorSelf[0],
                        v -> set(() -> c.armorSelf[0] = v)))
                .add(new VSetting.Toggle(VText.t("set.armor.self", VText.t(PIECES[1])), () -> c.armorSelf[1],
                        v -> set(() -> c.armorSelf[1] = v)))
                .add(new VSetting.Toggle(VText.t("set.armor.self", VText.t(PIECES[2])), () -> c.armorSelf[2],
                        v -> set(() -> c.armorSelf[2] = v)))
                .add(new VSetting.Toggle(VText.t("set.armor.self", VText.t(PIECES[3])), () -> c.armorSelf[3],
                        v -> set(() -> c.armorSelf[3] = v)))
                .add(new VSetting.Toggle(VText.t("set.armor.others", VText.t(PIECES[0])), () -> c.armorOthers[0],
                        v -> set(() -> c.armorOthers[0] = v)))
                .add(new VSetting.Toggle(VText.t("set.armor.others", VText.t(PIECES[1])), () -> c.armorOthers[1],
                        v -> set(() -> c.armorOthers[1] = v)))
                .add(new VSetting.Toggle(VText.t("set.armor.others", VText.t(PIECES[2])), () -> c.armorOthers[2],
                        v -> set(() -> c.armorOthers[2] = v)))
                .add(new VSetting.Toggle(VText.t("set.armor.others", VText.t(PIECES[3])), () -> c.armorOthers[3],
                        v -> set(() -> c.armorOthers[3] = v))));

        list.add(new VModule("hands", "✋", VText.t("mod.hands"),
                VText.t("mod.hands.d"),
                null, null)
                .add(new VSetting.Toggle(VText.t("set.hands.mirror"),
                        () -> arm() == Arm.LEFT, v -> setArm(v ? Arm.LEFT : Arm.RIGHT)))
                .add(new VSetting.Stepper(VText.t("set.mainhand.x"), () -> fmt(c.mainHandX),
                        () -> set(() -> c.mainHandX = dec(c.mainHandX)),
                        () -> set(() -> c.mainHandX = inc(c.mainHandX))))
                .add(new VSetting.Stepper(VText.t("set.mainhand.y"), () -> fmt(c.mainHandY),
                        () -> set(() -> c.mainHandY = dec(c.mainHandY)),
                        () -> set(() -> c.mainHandY = inc(c.mainHandY))))
                .add(new VSetting.Stepper(VText.t("set.mainhand.z"), () -> fmt(c.mainHandZ),
                        () -> set(() -> c.mainHandZ = dec(c.mainHandZ)),
                        () -> set(() -> c.mainHandZ = inc(c.mainHandZ))))
                .add(new VSetting.Stepper(VText.t("set.mainhand.scale"), () -> fmt(c.mainHandScale),
                        () -> set(() -> c.mainHandScale = decS(c.mainHandScale)),
                        () -> set(() -> c.mainHandScale = incS(c.mainHandScale))))
                .add(new VSetting.Stepper(VText.t("set.offhand.x"), () -> fmt(c.offHandX),
                        () -> set(() -> c.offHandX = dec(c.offHandX)),
                        () -> set(() -> c.offHandX = inc(c.offHandX))))
                .add(new VSetting.Stepper(VText.t("set.offhand.y"), () -> fmt(c.offHandY),
                        () -> set(() -> c.offHandY = dec(c.offHandY)),
                        () -> set(() -> c.offHandY = inc(c.offHandY))))
                .add(new VSetting.Stepper(VText.t("set.offhand.z"), () -> fmt(c.offHandZ),
                        () -> set(() -> c.offHandZ = dec(c.offHandZ)),
                        () -> set(() -> c.offHandZ = inc(c.offHandZ))))
                .add(new VSetting.Stepper(VText.t("set.offhand.scale"), () -> fmt(c.offHandScale),
                        () -> set(() -> c.offHandScale = decS(c.offHandScale)),
                        () -> set(() -> c.offHandScale = incS(c.offHandScale))))
                .add(new VSetting.Action(VText.t("set.reset"), () -> set(() -> {
                    c.mainHandX = c.mainHandY = c.mainHandZ = 0f;
                    c.offHandX = c.offHandY = c.offHandZ = 0f;
                    c.mainHandScale = c.offHandScale = 1f;
                }), () -> VStyle.DANGER)));

        list.add(new VModule("hud", "▤", VText.t("mod.hud"),
                VText.t("mod.hud.d"),
                null, null)
                .onOpen(() -> MinecraftClient.getInstance()
                        .setScreen(new HudEditorScreen(parent))));

        list.add(new VModule("vanillahud", "▥", VText.t("mod.vanillahud"),
                VText.t("mod.vanillahud.d"),
                () -> c.vanillaHudMove, v -> set(() -> c.vanillaHudMove = v)));

        list.add(new VModule("radar", "◎", VText.t("mod.radar"),
                VText.t("mod.radar.d"),
                () -> c.radar, v -> set(() -> c.radar = v))
                .add(new VSetting.Stepper(VText.t("set.range"),
                        () -> c.radarRange + VText.t("val.meters"),
                        () -> set(() -> c.radarRange = Math.max(16, c.radarRange - 16)),
                        () -> set(() -> c.radarRange = Math.min(256, c.radarRange + 16))))
                .add(new VSetting.Stepper(VText.t("set.size"), () -> String.valueOf(c.radarSize),
                        () -> set(() -> c.radarSize = Math.max(24, c.radarSize - 10)),
                        () -> set(() -> c.radarSize = Math.min(90, c.radarSize + 10))))
                .add(new VSetting.Toggle(VText.t("set.radar.names"), () -> c.radarNames,
                        v -> set(() -> c.radarNames = v)))
                .add(new VSetting.Toggle(VText.t("set.radar.north"), () -> c.radarNorthUp,
                        v -> set(() -> c.radarNorthUp = v))));

        list.add(new VModule("combattimer", "⏱", VText.t("mod.combattimer"),
                VText.t("mod.combattimer.d"),
                () -> c.combatTimer, v -> set(() -> c.combatTimer = v))
                .add(new VSetting.Stepper(VText.t("set.combatdauer"),
                        () -> c.combatDauer + "s",
                        () -> set(() -> c.combatDauer = Math.max(5, c.combatDauer - 5)),
                        () -> set(() -> c.combatDauer = Math.min(60, c.combatDauer + 5)))));

        list.add(new VModule("hitmarker", "✖", VText.t("mod.hitmarker"),
                VText.t("mod.hitmarker.d"),
                () -> c.hitmarker, v -> set(() -> c.hitmarker = v))
                .add(new VSetting.Toggle(VText.t("set.sound"), () -> c.hitSound, v -> set(() -> c.hitSound = v)))
                .add(new VSetting.Toggle(VText.t("set.hit.particles"), () -> c.hitParticles,
                        v -> set(() -> c.hitParticles = v))));

        list.add(new VModule("damage", "❤", VText.t("mod.damage"),
                VText.t("mod.damage.d"),
                () -> c.damageTint, v -> set(() -> c.damageTint = v))
                .add(new VSetting.Toggle(VText.t("set.screenshake"), () -> c.screenShake,
                        v -> set(() -> c.screenShake = v))));

        list.add(new VModule("armorhud", "🛡", VText.t("mod.armorhud"),
                VText.t("mod.armorhud.d"),
                () -> c.armorHud, v -> set(() -> c.armorHud = v)));

        list.add(new VModule("potions", "✿", VText.t("mod.potions"),
                VText.t("mod.potions.d"),
                () -> c.potionHud, v -> set(() -> c.potionHud = v)));

        list.add(new VModule("scoreboard", "▦", VText.t("mod.scoreboard"),
                VText.t("mod.scoreboard.d"),
                () -> c.scoreboardClean, v -> set(() -> c.scoreboardClean = v)));

        list.add(new VModule("keystrokes", "⌨", VText.t("mod.keystrokes"),
                VText.t("mod.keystrokes.d"),
                () -> c.keystrokes, v -> set(() -> c.keystrokes = v)));

        list.add(new VModule("counters", "⏱", VText.t("mod.counters"),
                VText.t("mod.counters.d"),
                null, null)
                .add(new VSetting.Toggle("FPS", () -> c.fps, v -> set(() -> c.fps = v)))
                .add(new VSetting.Toggle("CPS", () -> c.cps, v -> set(() -> c.cps = v)))
                .add(new VSetting.Toggle(VText.t("set.coords"), () -> c.coords,
                        v -> set(() -> c.coords = v)))
                .add(new VSetting.Toggle(VText.t("set.ping"), () -> c.ping, v -> set(() -> c.ping = v))));

        list.add(new VModule("zoom", "⌖", VText.t("mod.zoom"),
                VText.t("mod.zoom.d"),
                () -> c.zoom, v -> set(() -> c.zoom = v))
                .add(new VSetting.Info(VText.t("set.key"), () -> "C")));

        list.add(new VModule("freelook", "↻", VText.t("mod.freelook"),
                VText.t("mod.freelook.d"),
                () -> c.freelook, v -> set(() -> c.freelook = v))
                .add(new VSetting.Info(VText.t("set.key"), () -> VText.t("val.leftalt")))
                .add(new VSetting.Toggle(VText.t("set.shouldercam"), () -> c.shoulderCam,
                        v -> set(() -> c.shoulderCam = v))));

        list.add(new VModule("fullbright", "☀", VText.t("mod.fullbright"),
                VText.t("mod.fullbright.d"),
                () -> c.fullbright, v -> set(() -> c.fullbright = v)));

        list.add(new VModule("sky", "☁", VText.t("mod.sky"),
                VText.t("mod.sky.d"),
                () -> c.timeMode != 0, v -> set(() -> c.timeMode = v ? 1 : 0))
                .add(new VSetting.Stepper(VText.t("set.mode"), () -> VText.t(TIME_MODES[c.timeMode]),
                        () -> set(() -> c.timeMode =
                                (c.timeMode + TIME_MODES.length - 1) % TIME_MODES.length),
                        () -> set(() -> c.timeMode = (c.timeMode + 1) % TIME_MODES.length))));

        list.add(new VModule("minime", "☗", VText.t("mod.minime"),
                VText.t("mod.minime.d"),
                () -> c.miniMe, v -> set(() -> c.miniMe = v))
                .add(new VSetting.Action(VText.t("set.companion"),
                        () -> MinecraftClient.getInstance().setScreen(CompanionPicker.open()), () -> 0xFFA6C8FF))
                .add(new VSetting.Stepper(VText.t("pet.travel"),
                        () -> VText.t(new String[]{"pet.auto", "pet.walk", "pet.fly"}[Math.floorMod(c.miniMeFlight, 3)]),
                        () -> set(() -> c.miniMeFlight = Math.floorMod(c.miniMeFlight - 1, 3)),
                        () -> set(() -> c.miniMeFlight = (c.miniMeFlight + 1) % 3)))
                .add(new VSetting.Stepper(VText.t("set.seat"),
                        () -> VText.t(MINI_POS[c.miniMePos]),
                        () -> set(() -> c.miniMePos =
                                (c.miniMePos + MINI_POS.length - 1) % MINI_POS.length),
                        () -> set(() -> c.miniMePos = (c.miniMePos + 1) % MINI_POS.length)))
                .add(new VSetting.Stepper(VText.t("set.size"), () -> c.miniMeSize + "%",
                        () -> set(() -> c.miniMeSize = Math.max(15, c.miniMeSize - 5)),
                        () -> set(() -> c.miniMeSize = Math.min(80, c.miniMeSize + 5))))
                .add(new VSetting.Toggle(VText.t("set.sitting"), () -> c.miniMeSitzt,
                        v -> set(() -> c.miniMeSitzt = v)))
                .add(new VSetting.Stepper(VText.t("set.hat"),
                        () -> VText.t(MiniMe.HUT_NAMEN[c.miniMeHut]),
                        () -> set(() -> c.miniMeHut =
                                (c.miniMeHut + MiniMe.HUT_NAMEN.length - 1)
                                        % MiniMe.HUT_NAMEN.length),
                        () -> set(() -> c.miniMeHut =
                                (c.miniMeHut + 1) % MiniMe.HUT_NAMEN.length)))
                .add(new VSetting.Stepper(VText.t("pet.scarf"),
                        () -> VText.t(new String[]{"val.nohat", "pet.pink", "pet.blue", "pet.mint"}[Math.floorMod(c.miniMeScarf,4)]),
                        () -> set(() -> c.miniMeScarf = Math.floorMod(c.miniMeScarf-1,4)),
                        () -> set(() -> c.miniMeScarf = (c.miniMeScarf+1)%4)))
                .add(new VSetting.Stepper(VText.t("pet.shoes"),
                        () -> VText.t(new String[]{"val.nohat", "pet.pink", "pet.blue", "pet.bunnyslippers"}[Math.floorMod(c.miniMeShoes,4)]),
                        () -> set(() -> c.miniMeShoes = Math.floorMod(c.miniMeShoes-1,4)),
                        () -> set(() -> c.miniMeShoes = (c.miniMeShoes+1)%4)))
                .add(new VSetting.Action(VText.t("pet.importskin"), dev.visual.fabric.MiniSkin::importFile, () -> 0xFFA6C8FF))
                .add(new VSetting.Info(VText.t("pet.importstatus"), dev.visual.fabric.MiniSkin::importStatus))
                .add(new VSetting.Toggle(VText.t("set.wings"), () -> c.miniMeFluegel,
                        v -> set(() -> c.miniMeFluegel = v)))
                .add(new VSetting.Stepper(VText.t("set.ownskin"),
                        () -> c.miniMeSkin.isEmpty() ? VText.t("val.ownskin") : c.miniMeSkin,
                        () -> set(() -> c.miniMeSkin = skinWechseln(c.miniMeSkin, -1)),
                        () -> set(() -> c.miniMeSkin = skinWechseln(c.miniMeSkin, 1)))));

        list.add(new VModule("fakeplayer", "☻", VText.t("mod.fakeplayer"),
                VText.t("mod.fakeplayer.d"),
                () -> FakePlayerManager.isActive(),
                v -> FakePlayerManager.toggle(MinecraftClient.getInstance())));

        list.add(new VModule("titlescreen", "▤", VText.t("mod.titlescreen"),
                VText.t("mod.titlescreen.d"),
                () -> c.customTitle, v -> set(() -> c.customTitle = v)));

        list.add(new VModule("menukey", "⌨", VText.t("mod.menukey"),
                VText.t("mod.menukey.d"),
                () -> c.menuKey, v -> set(() -> c.menuKey = v)));

        list.add(new VModule("watermark", "◈", VText.t("mod.watermark"),
                VText.t("mod.watermark.d"),
                () -> c.watermark, v -> set(() -> c.watermark = v)));

        list.add(new VModule("clock", "◷", VText.t("mod.clock"),
                VText.t("mod.clock.d"),
                () -> c.clock, v -> set(() -> c.clock = v)));

        list.add(new VModule("target", "◉", VText.t("mod.target"),
                VText.t("mod.target.d"),
                () -> c.targetHud, v -> set(() -> c.targetHud = v)));

        list.add(new VModule("items", "▣", VText.t("mod.items"),
                VText.t("mod.items.d"),
                () -> c.itemCounter, v -> set(() -> c.itemCounter = v)));

        list.add(new VModule("mods", "⬇", VText.t("mod.mods"),
                VText.t("mod.mods.d"),
                null, null)
                .onOpen(() -> MinecraftClient.getInstance()
                        .setScreen(new ModBrowserScreen(parent, "mod"))));

        list.add(new VModule("cosmetics", "✦", VText.t("mod.cosmetics"),
                VText.t("mod.cosmetics.d"),
                null, null)
                .onOpen(() -> MinecraftClient.getInstance()
                        .setScreen(VisualCosmeticsScreen.oeffnen())));

        list.add(new VModule("packs", "◫", VText.t("mod.packs"),
                VText.t("mod.packs.d"),
                null, null)
                .onOpen(() -> MinecraftClient.getInstance()
                        .setScreen(new ModBrowserScreen(parent, "resourcepack"))));

        return list;
    }

    /**
     * Einen Schritt durch die Skin-Liste. Sie wird bei jedem Klick neu
     * gelesen — wer im Launcher einen Skin ablegt, findet ihn sofort,
     * ohne das Spiel neu zu starten.
     */
    private static String skinWechseln(String aktuell, int richtung) {
        java.util.List<String> alle = MiniSkin.namen();
        int i = Math.max(0, alle.indexOf(aktuell));
        return alle.get((i + richtung + alle.size()) % alle.size());
    }

    private static void set(Runnable change) {
        change.run();
        VConfig.save();
    }

    private static Arm arm() {
        return MinecraftClient.getInstance().options.getMainArm().getValue();
    }

    private static void setArm(Arm value) {
        MinecraftClient.getInstance().options.getMainArm().setValue(value);
        MinecraftClient.getInstance().options.write();
    }

    private static int colorIndex(int color) {
        for (int i = 0; i < COLORS.length; i++) {
            if (COLORS[i] == color) return i;
        }
        return 0;
    }

    private static float inc(float v) {
        float n = Math.round((v + 0.02f) * 100f) / 100f;
        return n > 0.301f ? 0.30f : n;
    }

    private static float dec(float v) {
        float n = Math.round((v - 0.02f) * 100f) / 100f;
        return n < -0.301f ? -0.30f : n;
    }

    private static float incS(float v) {
        float n = Math.round((v + 0.05f) * 100f) / 100f;
        return n > 2.001f ? 2.00f : n;
    }

    private static float decS(float v) {
        float n = Math.round((v - 0.05f) * 100f) / 100f;
        return n < 0.499f ? 0.50f : n;
    }

    private static String fmt(float v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
