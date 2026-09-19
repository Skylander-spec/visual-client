package dev.visual.fabric;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;

/**
 * Clientseitiger Fake Player: ein Klon des eigenen Spielers (Skin + Name),
 * nur lokal sichtbar. Praktisch zum Posieren/Testen — Server sehen ihn nicht.
 */
public class FakePlayerManager {
    private static OtherClientPlayerEntity fake;

    public static boolean isActive() {
        return fake != null;
    }

    public static void toggle(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (fake != null) {
            client.world.removeEntity(fake.getId(), Entity.RemovalReason.DISCARDED);
            fake = null;
            return;
        }
        OtherClientPlayerEntity clone =
                new OtherClientPlayerEntity(client.world, client.player.getGameProfile());
        clone.copyPositionAndRotation(client.player);
        clone.setHeadYaw(client.player.getHeadYaw());
        clone.setId(-2000000 - client.player.getRandom().nextInt(1000000));
        client.world.addEntity(clone);
        fake = clone;
    }

    public static void tick(MinecraftClient client) {
        if (fake != null && (client.world == null || fake.isRemoved())) {
            fake = null;
        }
    }
}
