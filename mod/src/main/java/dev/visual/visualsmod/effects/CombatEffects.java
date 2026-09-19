package dev.visual.visualsmod.effects;

import dev.visual.visualsmod.config.VisualsConfig;
import dev.visual.visualsmod.sounds.Sounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Random;

/**
 * Client-seitige Kampf-Effekte: Hit-Partikel beim eigenen Angriff und
 * Kill-Effekt (Partikel-Burst + Sound), wenn das zuletzt getroffene
 * Ziel stirbt. Funktioniert ohne Server-Unterstützung.
 */
public class CombatEffects {
    private final Random random = new Random();
    private LivingEntity lastTarget;
    private long lastHitMs;

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!event.getEntity().level().isClientSide) return;
        Minecraft mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player) return;
        if (!(event.getTarget() instanceof LivingEntity living)) return;

        VisualsConfig c = VisualsConfig.INSTANCE;
        lastTarget = living;
        lastHitMs = System.currentTimeMillis();

        if (c.hitParticles && mc.level != null) {
            double px = living.getX();
            double py = living.getY() + living.getBbHeight() * 0.6;
            double pz = living.getZ();
            for (int i = 0; i < Math.min(c.particleCount, 16); i++) {
                mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        px + spread(0.4), py + spread(0.4), pz + spread(0.4),
                        spread(0.08), 0.05 + random.nextDouble() * 0.08, spread(0.08));
            }
        }
        if (c.hitSound) {
            Sounds.playUi(Sounds.HIT, 0.95f + random.nextFloat() * 0.1f);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || lastTarget == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            lastTarget = null;
            return;
        }
        // Kill nur werten, wenn der letzte eigene Treffer < 3s her ist
        if (System.currentTimeMillis() - lastHitMs > 3000) {
            lastTarget = null;
            return;
        }
        if (lastTarget.isDeadOrDying() || lastTarget.isRemoved()) {
            VisualsConfig c = VisualsConfig.INSTANCE;
            if (c.killEffect) {
                double px = lastTarget.getX();
                double py = lastTarget.getY() + lastTarget.getBbHeight() * 0.5;
                double pz = lastTarget.getZ();
                int n = Math.min(c.particleCount * 3, 40);
                for (int i = 0; i < n; i++) {
                    mc.level.addParticle(ParticleTypes.END_ROD,
                            px, py, pz,
                            spread(0.35), random.nextDouble() * 0.4, spread(0.35));
                }
                for (int i = 0; i < n / 2; i++) {
                    mc.level.addParticle(ParticleTypes.CRIT,
                            px + spread(0.5), py + spread(0.5), pz + spread(0.5),
                            0, 0.1, 0);
                }
            }
            if (c.killSound) {
                Sounds.playUi(Sounds.KILL, 1.0f);
            }
            lastTarget = null;
        }
    }

    private double spread(double amount) {
        return (random.nextDouble() - 0.5) * 2 * amount;
    }
}
