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

/** 1.21.5: alte Keybinding-/Tasten-API, aber schon neue Textur- und Partikel-API. */
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
        client.getTextureManager().registerTexture(id, new NativeImageBackedTexture(id::toString, img));
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
        world.addParticleClient(effect, x, y, z, vx, vy, vz);
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
    private static java.util.concurrent.CompletableFuture<java.util.Optional<net.minecraft.client.util.SkinTextures>> hautAbruf;
    private static Identifier hautGemerkt;

    public static Identifier spielerHaut() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) return mc.player.getSkinTextures().texture();
            if (hautGemerkt != null) return hautGemerkt;
            if (hautAbruf == null) {
                hautAbruf = mc.getSkinProvider().fetchSkinTextures(mc.getGameProfile());
            }
            if (hautAbruf.isDone()) {
                java.util.Optional<net.minecraft.client.util.SkinTextures> o =
                        hautAbruf.getNow(java.util.Optional.empty());
                if (o.isPresent()) hautGemerkt = o.get().texture();
            }
            return hautGemerkt;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Eigener Rahmen auf dem Matrix-Stapel.
     *
     * Bis 1.21.5 heisst es push()/pop() auf einem MatrixStack, ab 1.21.6
     * pushMatrix()/popMatrix() auf einem Matrix3x2fStack. Wer nach
     * Compose zeichnet, braucht das: deren Renderer laesst eine
     * Transformation stehen, und ohne eigenen Rahmen landet alles
     * Folgende darin.
     */
    public static void matrixAuf(net.minecraft.client.gui.DrawContext ctx) {
        ctx.getMatrices().push();
    }

    public static void matrixZu(net.minecraft.client.gui.DrawContext ctx) {
        ctx.getMatrices().pop();
    }
    public static void foreground(DrawContext ctx) { ctx.draw(); }

    /**
     * Minecrafts eigene 3D-Skin-Vorschau bauen.
     *
     * Der dritte Parameter von PlayerSkinWidget heisst je nach Fassung
     * anders: bis 1.21.3 ein EntityModelLoader ueber
     * getEntityModelLoader(), ab 1.21.4 LoadedEntityModels ueber
     * getLoadedEntityModels(). Deshalb steht der Aufruf hier und nicht
     * im gemeinsamen Quelltext - dort liess er den Bau fuer 1.21.2 und
     * 1.21.3 scheitern.
     */
    public static net.minecraft.client.gui.widget.PlayerSkinWidget spiegelWidget(int breite, int hoehe) {
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        var fallback = net.minecraft.client.util.DefaultSkinHelper.getSkinTextures(mc.getGameProfile());
        var request = mc.getSkinProvider().fetchSkinTextures(mc.getGameProfile()).exceptionally(error -> java.util.Optional.empty());
        var skin = (java.util.function.Supplier<net.minecraft.client.util.SkinTextures>) () -> request.getNow(java.util.Optional.empty()).orElse(fallback);
        return new net.minecraft.client.gui.widget.PlayerSkinWidget(breite, hoehe, mc.getLoadedEntityModels(), skin);
    }

    /** OneConfig 1.0.x's avatar worker must find an already uploaded texture. */
    public static void prepareMenuSkin() {
        var mc = MinecraftClient.getInstance();
        var fallback = net.minecraft.client.util.DefaultSkinHelper.getSkinTextures(mc.getGameProfile()).texture();
        mc.getTextureManager().getTexture(fallback);
        var selected = spielerHaut();
        if (selected != null) mc.getTextureManager().getTexture(selected);
    }
}
