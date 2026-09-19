package dev.visual.fabric.mixin;

import dev.visual.fabric.CapeManager;
import dev.visual.fabric.Compat;
import dev.visual.fabric.VConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wie die 1.21.9-Variante, aber updateRenderState nimmt hier noch
 * AbstractClientPlayerEntity (PlayerLikeEntity gibt es erst ab 1.21.9).
 */
@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("TAIL"))
    private void visualsfabric$stripArmor(AbstractClientPlayerEntity player,
                                          PlayerEntityRenderState state, float tickDelta,
                                          CallbackInfo ci) {
        // Eigenes Cape aus dem Launcher — nur beim eigenen Spieler, andere
        // sehen es ohnehin nicht (rein lokale Kosmetik).
        if (player == MinecraftClient.getInstance().player) {
            var cape = CapeManager.get(MinecraftClient.getInstance());
            if (cape != null) Compat.applyCape(state, cape);
        }

        boolean self = player == MinecraftClient.getInstance().player;
        boolean[] visible = self ? VConfig.get().armorSelf : VConfig.get().armorOthers;
        if (!visible[0]) state.equippedHeadStack = ItemStack.EMPTY;
        if (!visible[1]) state.equippedChestStack = ItemStack.EMPTY;
        if (!visible[2]) state.equippedLegsStack = ItemStack.EMPTY;
        if (!visible[3]) state.equippedFeetStack = ItemStack.EMPTY;
    }
}
