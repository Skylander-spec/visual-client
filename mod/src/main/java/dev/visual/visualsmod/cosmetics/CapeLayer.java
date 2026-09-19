package dev.visual.visualsmod.cosmetics;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.visual.visualsmod.config.VisualsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Rendert ein lokales Custom-Cape (64x32 PNG aus
 * %APPDATA%/.visualclient/cosmetics/cape.png) auf den eigenen Spieler.
 * Nur clientseitig sichtbar — kein Server-Sync.
 */
public class CapeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static ResourceLocation capeTexture;
    private static boolean loadAttempted;

    public CapeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    private static ResourceLocation texture() {
        if (!loadAttempted) {
            loadAttempted = true;
            File f = new File(VisualsConfig.cosmeticsDir(), "cape.png");
            if (f.exists()) {
                try (InputStream in = new FileInputStream(f)) {
                    NativeImage img = NativeImage.read(in);
                    capeTexture = Minecraft.getInstance().getTextureManager()
                            .register("visualsmod_cape", new DynamicTexture(img));
                } catch (IOException ignored) {
                }
            }
        }
        return capeTexture;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTicks, float age, float headYaw, float headPitch) {
        if (!VisualsConfig.INSTANCE.capeEnabled) return;
        if (player != Minecraft.getInstance().player) return;
        if (player.isInvisible() || !player.isModelPartShown(PlayerModelPart.CAPE)) return;
        if (player.getSkinTextureLocation() == null) return;
        ItemStack chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (chest.is(Items.ELYTRA)) return;
        ResourceLocation tex = texture();
        if (tex == null) return;

        pose.pushPose();
        pose.translate(0.0F, 0.0F, 0.125F);
        double d0 = Mth.lerp(partialTicks, player.xCloakO, player.xCloak)
                - Mth.lerp(partialTicks, player.xo, player.getX());
        double d1 = Mth.lerp(partialTicks, player.yCloakO, player.yCloak)
                - Mth.lerp(partialTicks, player.yo, player.getY());
        double d2 = Mth.lerp(partialTicks, player.zCloakO, player.zCloak)
                - Mth.lerp(partialTicks, player.zo, player.getZ());
        float yaw = Mth.rotLerp(partialTicks, player.yBodyRotO, player.yBodyRot);
        double sin = Mth.sin(yaw * ((float) Math.PI / 180F));
        double cos = -Mth.cos(yaw * ((float) Math.PI / 180F));
        float f1 = Mth.clamp((float) d1 * 10.0F, -6.0F, 32.0F);
        float f2 = Mth.clamp((float) (d0 * sin + d2 * cos) * 100.0F, 0.0F, 150.0F);
        float f3 = Mth.clamp((float) (d0 * cos - d2 * sin) * 100.0F, -20.0F, 20.0F);
        float bob = Mth.lerp(partialTicks, player.oBob, player.bob);
        f1 += Mth.sin(Mth.lerp(partialTicks, player.walkDistO, player.walkDist) * 6.0F) * 32.0F * bob;
        if (player.isCrouching()) f1 += 25.0F;

        pose.mulPose(Axis.XP.rotationDegrees(6.0F + f2 / 2.0F + f1));
        pose.mulPose(Axis.ZP.rotationDegrees(f3 / 2.0F));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - f3 / 2.0F));
        VertexConsumer vc = buffer.getBuffer(RenderType.entitySolid(tex));
        getParentModel().renderCloak(pose, vc, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
