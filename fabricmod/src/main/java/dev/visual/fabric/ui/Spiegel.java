package dev.visual.fabric.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.client.util.DefaultSkinHelper;

/** Minecraft's original 3D preview, including a world-free skin model. */
public final class Spiegel {
    private PlayerSkinWidget skin;
    private Object profile;

    public void zeichne(DrawContext ctx, int left, int top, int right, int bottom,
                        int mouseX, int mouseY, float delta) {
        if (right <= left || bottom <= top) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            // Leave horizontal room for shoulder pets and the follower preview.
            float span = dev.visual.fabric.VConfig.get().miniMe ? 2.2f : 1.2f;
            int scale = Math.max(1, (int) Math.min((bottom - top) / 2.6f, (right - left) / span));
            int lookX = Math.max(left, Math.min(right, mouseX));
            int lookY = Math.max(top, Math.min(bottom, mouseY));
            dev.visual.fabric.MiniMe.beginPreview();
            try {
                InventoryScreen.drawEntity(ctx, left, top, right, bottom, scale, 0.0625f, lookX, lookY, mc.player);
            } finally { dev.visual.fabric.MiniMe.endPreview(); }
            return;
        }
        // The vanilla widget does not need a ClientWorld or fake server entity.
        if (skin == null || !mc.getGameProfile().equals(profile)) {
            profile = mc.getGameProfile();
            skin = dev.visual.fabric.Compat.spiegelWidget(right - left, bottom - top);
        }
        skin.setPosition(left, top);
        skin.setWidth(right - left);
        skin.setHeight(bottom - top);
        skin.render(ctx, mouseX, mouseY, delta);
    }
}
