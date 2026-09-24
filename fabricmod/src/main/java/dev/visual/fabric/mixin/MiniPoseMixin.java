package dev.visual.fabric.mixin;

import dev.visual.fabric.MiniMe;
import dev.visual.fabric.ui.MiniPose;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public abstract class MiniPoseMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void sky$pose(PlayerEntityRenderState state, CallbackInfo ci) {
        if (!MiniMe.zeichnetGerade() || MiniMe.config().miniMeArt != 0) return;
        MiniPose.apply((PlayerEntityModel) (Object) this, MiniMe.sitzt(), state.age, MiniMe.schritt(), MiniMe.bewegung());
    }
}
