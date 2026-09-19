package dev.visual.fabric;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.InputUtil;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

/** 1.21.5–1.21.8: alte Keybinding-/Tasten-API, aber schon neue Textur- und Partikel-API. */
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
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, w, h,
                texW, texH, texW, texH, argb);
    }

    /** Partikel clientseitig erzeugen (Methodenname unterscheidet sich je Version). */
    public static void spawnParticle(World world, ParticleEffect effect,
                                     double x, double y, double z,
                                     double vx, double vy, double vz) {
        world.addParticleClient(effect, x, y, z, vx, vy, vz);
    }

    /** Matrix3x2fStack-API (ab 1.21.6). */
    public static void hudPush(DrawContext ctx) { ctx.getMatrices().pushMatrix(); }

    public static void hudTranslate(DrawContext ctx, float x, float y) {
        ctx.getMatrices().translate(x, y);
    }

    public static void hudPop(DrawContext ctx) { ctx.getMatrices().popMatrix(); }

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
}