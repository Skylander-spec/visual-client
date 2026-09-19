package dev.visual.fabric.mixin;

import dev.visual.fabric.VConfig;
import dev.visual.fabric.VisualsExtras;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Kamera-Anpassungen: Freelook (Blick lösen, Laufrichtung bleibt) und
 * Screen-Shake bei Schaden. Die Signatur von {@code update} unterscheidet sich je Version —
 * deshalb liegt dieser Mixin in der Kompatibilitätsfamilie.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Invoker("setRotation")
    abstract void visualsfabric$setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void visualsfabric$adjust(BlockView area, Entity focused, boolean thirdPerson,
                                      boolean inverseView, float tickDelta, CallbackInfo ci) {
        VConfig c = VConfig.get();
        float yaw = Float.NaN;
        float pitch = 0f;

        if (c.freelook && VisualsExtras.isFreelookActive()) {
            yaw = VisualsExtras.freeYaw();
            pitch = VisualsExtras.freePitch();
        }

        if (c.screenShake) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null && mc.player.hurtTime > 0 && mc.player.maxHurtTime > 0) {
                float strength = mc.player.hurtTime / (float) mc.player.maxHurtTime;
                float amp = strength * strength * 3.5f;
                float baseYaw = Float.isNaN(yaw) ? focused.getYaw() : yaw;
                float basePitch = Float.isNaN(yaw) ? focused.getPitch() : pitch;
                // deterministisches Wackeln über die Zeit, kein Zufalls-Zittern
                long t = System.currentTimeMillis();
                yaw = baseYaw + (float) Math.sin(t / 22.0) * amp;
                pitch = basePitch + (float) Math.cos(t / 17.0) * amp * 0.6f;
            }
        }

        if (!Float.isNaN(yaw)) {
            visualsfabric$setRotation(yaw, pitch);
        }
    }
}
