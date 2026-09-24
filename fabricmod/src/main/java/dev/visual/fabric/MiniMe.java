package dev.visual.fabric;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/** Cosmetic companions for the owning player, with independent pose and terrain navigation. */
public final class MiniMe {
    /** Höhe des Spielermodells in Blöcken — Bezug für alle Sitzplätze. */
    private static final float GROESSE = 1.8f;
    /** Seitlicher Versatz für die Schulterplätze, in Blöcken. */
    private static final float SEITE = 0.42f;

    /**
     * Auswahl an Hüten. Bewusst Minecrafts eigene Gegenstände: die zeichnet
     * das Spiel selbst auf den Kopf, ganz ohne eigenes Modell oder eigene
     * Textur — und es sieht auf jeder Version gleich richtig aus.
     */
    private static final Item[] HUETE = {
            null,
            Items.GOLDEN_HELMET,
            Items.TURTLE_HELMET,
            Items.CARVED_PUMPKIN,
            Items.CAKE,
            Items.POPPY,
            Items.DRAGON_HEAD
    };
    /** Sprachschlüssel je Hut, gleiche Reihenfolge. */
    public static final String[] HUT_NAMEN = {
            "val.nohat", "val.hat.gold", "val.hat.turtle", "val.hat.pumpkin",
            "val.hat.cake", "val.hat.flower", "val.hat.dragon", "pet.beanie", "pet.catears", "pet.bunnyears"
    };

    private static boolean zeichnet = false;
    private static int previewDepth;
    public static void beginPreview() { previewDepth++; }
    public static void endPreview() { previewDepth = Math.max(0, previewDepth - 1); }
    private static final java.util.Map<Object, Owner> OWNERS = new java.util.WeakHashMap<>();
    private static final java.util.Map<java.util.UUID, CompanionMotion> FOLLOW = new java.util.HashMap<>();
    private record Owner(java.util.UUID uuid, VConfig config, float delta) {}
    private static Owner owner;
    private static Object currentState;
    private static Object world;
    private static final java.util.Set<java.util.UUID> PRESENT = new java.util.HashSet<>();

    public static VConfig config() { return owner == null ? VConfig.get() : owner.config; }
    public static java.util.UUID ownerId() { return owner == null ? null : owner.uuid; }
    public static Identifier skin() {
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        return owner == null || (mc.player != null && owner.uuid.equals(mc.player.getUuid()))
                ? MiniSkin.textur() : CosmeticsSync.skin(owner.uuid);
    }
    public static float schritt() {
        CompanionMotion f = owner == null ? null : FOLLOW.get(owner.uuid);
        return f == null ? 0f : f.step;
    }
    public static float bewegung() {
        CompanionMotion f = owner == null ? null : FOLLOW.get(owner.uuid);
        return f == null || config().miniMePos != 3 ? 0f : f.movement;
    }


    /** Zwischenspeicher, um den Zustand nach dem Durchgang herzustellen. */
    private static Object skinVorher = null;
    private static ItemStack kopfVorher = null;
    private static ItemStack brustVorher = null;

    /** Abstand, den der Begleiter hinter dem Spieler haelt, in Bloecken. */
    private static final double ABSTAND = 0.9;
    /** Wie schnell er aufschliesst: 0 = gar nicht, 1 = sofort. */
    private static final double TEMPO = 0.18;
    /** Ab dieser Entfernung wird nicht mehr gelaufen, sondern aufgeholt. */
    private static final double ZU_WEIT = 8.0;

    private MiniMe() {
    }

    /** Called after a player renderer fills its state; works for local and remote players. */
    public static void merken(Object state, java.util.UUID uuid, float delta) {
        VConfig c = CosmeticsSync.config(uuid);
        if (c != null && c.miniMe) OWNERS.put(state, new Owner(uuid, c, Math.max(0f, Math.min(1f, delta))));
        else OWNERS.remove(state);
    }

    /** Läuft gerade der Durchgang des Kleinen? Die Pose fragt das ab. */
    public static boolean zeichnetGerade() {
        return zeichnet;
    }

    public static boolean fliegt(VConfig c) {
        return c.miniMeFlight == 2 || (c.miniMeFlight == 0 && Begleiter.flying(c.miniMeArt));
    }

    public static boolean sitzt() {
        return config().miniMeSitzt && config().miniMePos != 3;
    }

    /**
     * Darf jetzt ein Mini gezeichnet werden? Liefert nur beim äußeren
     * Durchlauf {@code true}; der Mini selbst bekommt keinen zweiten.
     */
    public static boolean beginnen(Object zustand) {
        if (zeichnet) return false;
        Owner candidate = OWNERS.get(zustand);
        if (candidate == null || !candidate.config.miniMe) return false;
        owner = candidate;
        currentState = zustand;
        zeichnet = true;
        return true;
    }

    public static void beenden() {
        zeichnet = false;
        owner = null;
        currentState = null;
    }

    /**
     * Den Zustand für den Durchgang des Kleinen umstellen: eigener Skin,
     * Hut, Flügel. Was ersetzt wird, liegt vorher im Zwischenspeicher.
     */
    public static void anziehen(Object roh) {
        skinVorher = null;
        kopfVorher = null;
        brustVorher = null;
        if (!(roh instanceof PlayerEntityRenderState zustand)) return;
        VConfig c = config();
        try {
            Identifier skin = skin();
            if (skin != null) {
                skinVorher = zustand.skinTextures;
                Compat.applySkin(zustand, skin);
            }

        } catch (Throwable t) {
            // Lieber ein Kleiner ohne Zubehör als ein zerrissenes Bild
        }
    }

    /** Alles zurücknehmen — sonst trägt das Original die Sachen weiter. */
    public static void ausziehen(Object roh) {
        if (!(roh instanceof PlayerEntityRenderState zustand)) return;
        try {
            // Der Typ von skinTextures unterscheidet sich je Version,
            // deshalb laeuft das Zurueckgeben ueber Compat.
            if (skinVorher != null) Compat.restoreSkin(zustand, skinVorher);
            if (kopfVorher != null) zustand.equippedHeadStack = kopfVorher;
            if (brustVorher != null) zustand.equippedChestStack = brustVorher;
        } catch (Throwable t) {
            // s. o.
        } finally {
            skinVorher = null;
            kopfVorher = null;
            brustVorher = null;
        }
    }

    /**
     * Setzt die Matrix auf den Sitzplatz. Aufgerufen wird sie am Ende des
     * Renderns, also am Fußpunkt des Spielers ohne dessen Drehung — deshalb
     * hier erst in die Blickrichtung drehen, dann seitlich versetzen und
     * wieder zurückdrehen, damit der Kleine seine eigene Drehung selbst
     * anwenden kann.
     *
     * Sitzt er, liegen die Füße tiefer: die angewinkelten Beine ragen nach
     * vorn statt nach unten, sonst schwebte er über der Schulter.
     */
    public static void platzieren(MatrixStack matrizen, float koerperDrehung) {
        VConfig c = config();
        float faktor = Math.max(0.1f, Math.min(0.9f, c.miniMeSize / 100f));
        boolean sitzt = c.miniMeArt == 0 && c.miniMeSitzt && c.miniMePos != 3;
        float sitzAbzug = sitzt ? 0.75f * faktor : 0f;

        if (c.miniMePos == 3) {
            var mc = net.minecraft.client.MinecraftClient.getInstance();
            // GUI previews must keep the companion inside the mirror.
            if (previewDepth > 0) {
                matrizen.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-koerperDrehung));
                matrizen.translate(0.7f, 0f, 0f);
                matrizen.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(koerperDrehung));
            } else if (owner != null && currentState instanceof PlayerEntityRenderState state) {
                CompanionMotion f = FOLLOW.get(owner.uuid);
                if (f != null && f.ready()) {
                    double x = f.previousX + (f.x - f.previousX) * owner.delta;
                    double y = f.previousY + (f.y - f.previousY) * owner.delta;
                    double z = f.previousZ + (f.z - f.previousZ) * owner.delta;
                    float yaw = f.previousYaw + net.minecraft.util.math.MathHelper.wrapDegrees(
                            f.yaw - f.previousYaw) * owner.delta;
                    matrizen.translate(x - state.x, y - state.y, z - state.z);
                    matrizen.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(koerperDrehung - yaw));
                }
            }
        } else {
            matrizen.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-koerperDrehung));
            switch (c.miniMePos) {
                case 0 -> matrizen.translate(0f, 1.84f - sitzAbzug, 0f);
                case 1 -> matrizen.translate(SEITE, 1.35f - sitzAbzug, 0f);
                default -> matrizen.translate(-SEITE, 1.35f - sitzAbzug, 0f);
            }
            matrizen.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(koerperDrehung));
        }
        if (c.miniMePos == 3) {
            matrizen.translate(0f, (fliegt(c) ? (0.06 + Math.sin(((PlayerEntityRenderState) currentState).age * .12) * .04) : MiniAnimation.hop(schritt(), bewegung()) * faktor), 0f);
        }
        matrizen.scale(faktor, faktor, faktor);
    }
    /**
     * Fuehrt den Begleiter nach. Jeden Tick rueckt er ein Stueck auf einen
     * Punkt hinter dem Spieler zu — dadurch laeuft er hinterher, statt starr
     * angeheftet zu sein, und schneidet Kurven wie ein echtes Haustier.
     *
     * Wird er abgehaengt (Teleport, Elytra), holt er in einem Schritt auf,
     * statt quer durch die Welt zu schweben.
     */
    public static void tick(net.minecraft.client.MinecraftClient client) {
        if (client.world != world) {
            world = client.world; FOLLOW.clear(); OWNERS.clear(); Begleiter.reset();
        }
        if (client.world == null) return;
        if (!CosmeticsSync.hasRemoteCompanions() && (!VConfig.get().miniMe || VConfig.get().miniMePos != 3)) {
            FOLLOW.clear();
            return;
        }
        java.util.Set<java.util.UUID> present = PRESENT;
        present.clear();
        for (var player : client.world.getPlayers()) {
            VConfig c = CosmeticsSync.config(player.getUuid());
            if (c == null || !c.miniMe || c.miniMePos != 3) continue;
            present.add(player.getUuid());
            CompanionMotion f = FOLLOW.computeIfAbsent(player.getUuid(), id -> new CompanionMotion());
            f.tick(new CompanionTerrain(client.world, c.miniMeSize / 100.0),
                    player.getX(), player.getY(), player.getZ(), player.getBodyYaw(), fliegt(c));
        }
        FOLLOW.keySet().retainAll(present);
    }
}
