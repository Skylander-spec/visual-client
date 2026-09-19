package dev.visual.fabric;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/**
 * Mini-Me: eine verkleinerte Ausgabe des eigenen Spielers, die auf dem Kopf
 * oder einer Schulter mitreitet.
 *
 * Gezeichnet wird nicht etwa ein nachgebautes Modell, sondern derselbe
 * Renderer noch einmal — mit verschobener und verkleinerter Matrix. Dadurch
 * bekommt der Kleine automatisch Skin, Cape, Rüstung, Gegenstand in der Hand
 * und dieselbe Pose wie das Original, ohne dass hier irgendetwas davon
 * nachgepflegt werden müsste. Der Preis ist die Rekursion, gegen die
 * {@link #beginnen} sichert.
 *
 * Für den Durchgang des Kleinen wird der Render-Zustand kurz umgestellt:
 * eigener Skin, Hut auf dem Kopf, Elytren als Flügel. Danach stellt
 * {@link #ausziehen} alles zurück, sonst trüge das Original die Sachen mit.
 *
 * Nur der eigene Spieler bekommt ihn: es ist eine lokale Kosmetik, andere
 * sehen sie ohnehin nicht.
 */
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
            "val.hat.cake", "val.hat.flower", "val.hat.dragon"
    };

    private static boolean zeichnet = false;
    private static Object eigenerZustand = null;

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

    private static double folgeX;
    private static double folgeY;
    private static double folgeZ;
    private static float folgeGier;
    private static boolean folgeBereit = false;

    private MiniMe() {
    }

    /** Beim Befüllen des Render-Zustands aufrufen. */
    public static void merken(Object zustand, boolean selbst) {
        if (selbst) eigenerZustand = zustand;
    }

    /** Gilt dieser Zustand als der eigene Spieler? */
    public static boolean istEigener(Object zustand) {
        return zustand != null && zustand == eigenerZustand;
    }

    /** Läuft gerade der Durchgang des Kleinen? Die Pose fragt das ab. */
    public static boolean zeichnetGerade() {
        return zeichnet;
    }

    public static boolean sitzt() {
        return VConfig.get().miniMeSitzt && VConfig.get().miniMePos != 3;
    }

    /**
     * Darf jetzt ein Mini gezeichnet werden? Liefert nur beim äußeren
     * Durchlauf {@code true}; der Mini selbst bekommt keinen zweiten.
     */
    public static boolean beginnen(Object zustand) {
        if (zeichnet) return false;
        if (!VConfig.get().miniMe) return false;
        if (!istEigener(zustand)) return false;
        zeichnet = true;
        return true;
    }

    public static void beenden() {
        zeichnet = false;
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
        VConfig c = VConfig.get();
        try {
            Identifier skin = MiniSkin.textur();
            if (skin != null) {
                skinVorher = zustand.skinTextures;
                Compat.applySkin(zustand, skin);
            }
            Item hut = c.miniMeHut > 0 && c.miniMeHut < HUETE.length ? HUETE[c.miniMeHut] : null;
            if (hut != null) {
                kopfVorher = zustand.equippedHeadStack;
                zustand.equippedHeadStack = new ItemStack(hut);
            }
            if (c.miniMeFluegel) {
                brustVorher = zustand.equippedChestStack;
                zustand.equippedChestStack = new ItemStack(Items.ELYTRA);
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
        VConfig c = VConfig.get();
        float faktor = Math.max(0.1f, Math.min(0.9f, c.miniMeSize / 100f));
        boolean sitzt = c.miniMeSitzt && c.miniMePos != 3;
        float sitzAbzug = sitzt ? 0.55f * faktor : 0f;

        matrizen.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-koerperDrehung));
        switch (c.miniMePos) {
            // Auf dem Kopf: Füße auf dem Scheitel (Kopf endet bei 1,85)
            case 0 -> matrizen.translate(0.0f, 1.84f + sitzAbzug, 0.0f);
            // Auf der Schulter: Füße auf der Oberkante des Arms (1,35) und so
            // weit nach außen, dass der Kleine den Kopf nicht schneidet — der
            // Kopf ist 0,25 breit, der Kleine bei 35 % rund 0,18.
            case 1 -> matrizen.translate(SEITE, 1.35f + sitzAbzug, 0.0f);
            case 2 -> matrizen.translate(-SEITE, 1.35f + sitzAbzug, 0.0f);
            // Begleiter: an seiner eigenen, nachgefuehrten Stelle am Boden.
            // Die Matrix steht am Fusspunkt des Spielers, also genuegt die
            // Differenz in Weltkoordinaten.
            default -> {
                var spieler = net.minecraft.client.MinecraftClient.getInstance().player;
                if (spieler != null && folgeBereit) {
                    matrizen.translate(folgeX - spieler.getX(),
                            folgeY - spieler.getY(),
                            folgeZ - spieler.getZ());
                    // Der Kleine bekommt vom Renderer die Drehung des Spielers.
                    // Diese Vordrehung schiebt sie auf seine eigene um.
                    matrizen.multiply(RotationAxis.POSITIVE_Y
                            .rotationDegrees(folgeGier - koerperDrehung));
                }
            }
        }
        matrizen.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(koerperDrehung));
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
        VConfig c = VConfig.get();
        if (client == null || client.player == null || !c.miniMe || c.miniMePos != 3) {
            folgeBereit = false;
            return;
        }
        var spieler = client.player;
        double bogen = Math.toRadians(spieler.getBodyYaw());
        // Blickrichtung ist (-sin, cos) — hinter ihm also genau andersherum
        double zielX = spieler.getX() + Math.sin(bogen) * ABSTAND;
        double zielZ = spieler.getZ() - Math.cos(bogen) * ABSTAND;
        double zielY = spieler.getY();

        if (!folgeBereit
                || spieler.squaredDistanceTo(folgeX, folgeY, folgeZ) > ZU_WEIT * ZU_WEIT) {
            folgeX = zielX;
            folgeY = zielY;
            folgeZ = zielZ;
            folgeGier = spieler.getBodyYaw();
            folgeBereit = true;
            return;
        }
        folgeX += (zielX - folgeX) * TEMPO;
        folgeY += (zielY - folgeY) * TEMPO;
        folgeZ += (zielZ - folgeZ) * TEMPO;
        // Blickrichtung auf die eigene Laufrichtung drehen, kuerzester Weg
        float diff = net.minecraft.util.math.MathHelper.wrapDegrees(
                spieler.getBodyYaw() - folgeGier);
        folgeGier += diff * (float) TEMPO;
    }
}