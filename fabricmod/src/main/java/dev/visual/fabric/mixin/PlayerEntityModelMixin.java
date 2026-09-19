package dev.visual.fabric.mixin;

import dev.visual.fabric.MiniMe;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Setzt den Mini-Me in eine sitzende Haltung.
 *
 * Angesetzt wird nach setAngles, weil Minecraft die Gliedmassen dort jedes
 * Bild neu stellt — alles, was vorher gesetzt wuerde, waere ueberschrieben.
 * Die Winkel sind Minecrafts eigene Reitwerte; damit sitzt der Kleine genau
 * so, wie ein Spieler in einem Boot sitzt.
 *
 * Das Modell ist zwischen Original und Mini dasselbe Objekt. Gestellt wird
 * hier deshalb nur, wenn gerade der Durchgang des Kleinen laeuft; fuer das
 * Original stellt der naechste setAngles-Aufruf ohnehin wieder alles um.
 */
@Mixin(PlayerEntityModel.class)
public class PlayerEntityModelMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V",
            at = @At("TAIL"))
    private void visualsfabric$sitzen(PlayerEntityRenderState zustand, CallbackInfo ci) {
        if (!MiniMe.zeichnetGerade() || !MiniMe.sitzt()) return;
        BipedEntityModel<?> m = (BipedEntityModel<?>) (Object) this;
        m.rightLeg.pitch = -1.4137167f;
        m.rightLeg.yaw = 0.31415927f;
        m.rightLeg.roll = 0.07853982f;
        m.leftLeg.pitch = -1.4137167f;
        m.leftLeg.yaw = -0.31415927f;
        m.leftLeg.roll = -0.07853982f;
        // Arme leicht nach vorn, damit er sich abzustuetzen scheint
        m.rightArm.pitch = -0.4f;
        m.leftArm.pitch = -0.4f;
    }
}