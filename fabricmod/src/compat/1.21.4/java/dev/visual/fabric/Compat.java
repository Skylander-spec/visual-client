package dev.visual.fabric;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.InputUtil;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

/** Versionsabhängige Aufrufe für 1.21.4 (Kategorie ist noch ein String). */
public final class Compat {
    private Compat() {
    }

    public static KeyBinding registerMenuKey() {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.visualsfabric.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "key.categories.misc"));
    }

    /** 1.21.4: InputUtil erwartet das GLFW-Handle. */
    public static boolean isKeyDown(MinecraftClient client, int key) {
        return InputUtil.isKeyPressed(client.getWindow().getHandle(), key);
    }

    /** 1.21.4: Textur ohne Label-Supplier. */
    public static void registerTexture(MinecraftClient client, Identifier id, NativeImage img) {
        client.getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
    }

    /** 1.21.4: Textur skaliert zeichnen, ARGB als Tönung/Alpha. */
    public static void drawTex(DrawContext ctx, Identifier id, int x, int y, int w, int h,
                               int texW, int texH, int argb) {
        ctx.drawTexture(RenderLayer::getGuiTextured, id, x, y, 0f, 0f, w, h,
                texW, texH, texW, texH, argb);
    }

    /**
     * Nur einen Ausschnitt der Textur zeichnen. Fuer Umhaenge noetig:
     * das Blatt ist 64x32 gross, sichtbar ist davon nur die Vorderseite
     * bei (1,1) in 10x16 - wer das ganze Blatt malt, bekommt ein
     * winziges Bild mit Rueckseite und Raendern daneben.
     */
    public static void drawTexAusschnitt(DrawContext ctx, Identifier id, int x, int y,
                                        int w, int h, float u, float v,
                                        int regionW, int regionH,
                                        int texW, int texH, int argb) {
        ctx.drawTexture(RenderLayer::getGuiTextured, id, x, y, u, v, w, h,
                regionW, regionH, texW, texH, argb);
    }

    /** Partikel clientseitig erzeugen (Methodenname unterscheidet sich je Version). */
    public static void spawnParticle(World world, ParticleEffect effect,
                                     double x, double y, double z,
                                     double vx, double vy, double vz) {
        world.addParticle(effect, x, y, z, vx, vy, vz);
    }

    /** MatrixStack-API (bis 1.21.5). */
    public static void hudPush(DrawContext ctx) { ctx.getMatrices().push(); }

    public static void hudTranslate(DrawContext ctx, float x, float y) {
        ctx.getMatrices().translate(x, y, 0f);
    }

    public static void hudPop(DrawContext ctx) { ctx.getMatrices().pop(); }

    /** Bis 1.21.8: Cape-Textur ist ein Identifier im SkinTextures-Record. */
    public static void applyCape(net.minecraft.client.render.entity.state.PlayerEntityRenderState state,
                                 Identifier cape) {
        net.minecraft.client.util.SkinTextures s = state.skinTextures;
        if (s == null) return;
        state.skinTextures = new net.minecraft.client.util.SkinTextures(
                s.texture(), s.textureUrl(), cape, s.elytraTexture(), s.model(), s.secure());
        state.capeVisible = true;
    }

    /**
     * Skin des Render-Zustands austauschen. Der Mini-Me darf einen eigenen
     * tragen — gesetzt wird er nur fuer dessen Durchgang und danach wieder
     * zurueckgenommen. Bis 1.21.8 steht die Textur an erster Stelle des
     * SkinTextures-Records.
     */
    public static void applySkin(net.minecraft.client.render.entity.state.PlayerEntityRenderState state,
                                 Identifier skin) {
        net.minecraft.client.util.SkinTextures s = state.skinTextures;
        if (s == null) return;
        state.skinTextures = new net.minecraft.client.util.SkinTextures(
                skin, s.textureUrl(), s.capeTexture(), s.elytraTexture(), s.model(), s.secure());
    }
    /** Gegenstueck zu applySkin: den gemerkten Skin zurueckgeben. */
    public static void restoreSkin(net.minecraft.client.render.entity.state.PlayerEntityRenderState state,
                                   Object vorher) {
        if (vorher instanceof net.minecraft.client.util.SkinTextures s) state.skinTextures = s;
    }

    /**
     * Skin-Textur des eigenen Spielers, auch ohne Welt. Im Spiel direkt vom
     * Spieler, auf dem Startbildschirm ueber den Skin-Dienst - der Abruf
     * laeuft nebenher, bis er fertig ist liefert die Methode null.
     */
    // Der Rueckgabetyp wechselt mitten in der Reihe: bis 1.21.3 ein
    // CompletableFuture<SkinTextures>, ab 1.21.4 eines mit Optional
    // darin. Derselbe Compat-Ordner bedient aber alle drei, darum wird
    // hier roh gehalten und beim Lesen unterschieden.
    private static Object hautAbruf;
    private static Identifier hautGemerkt;

    public static Identifier spielerHaut() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) return mc.player.getSkinTextures().texture();
            if (hautGemerkt != null) return hautGemerkt;
            if (hautAbruf == null) {
                hautAbruf = mc.getSkinProvider().fetchSkinTextures(mc.getGameProfile());
            }
            if (hautAbruf instanceof java.util.concurrent.CompletableFuture<?> f
                    && f.isDone()) {
                Object o = f.getNow(null);
                if (o instanceof java.util.Optional<?> opt) o = opt.orElse(null);
                if (o instanceof net.minecraft.client.util.SkinTextures st) {
                    hautGemerkt = st.texture();
                }
            }
            return hautGemerkt;
        } catch (Throwable t) {
            return null;
        }
    }
}