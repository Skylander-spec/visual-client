package dev.visual.fabric;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.*;

/** Local cosmetic models. Never inserted into the world or sent to a server. */
public final class Begleiter {
    public static final String[] NAMEN = {"val.ownskin", "entity.minecraft.cat",
            "entity.minecraft.wolf", "entity.minecraft.fox", "entity.minecraft.rabbit",
            "entity.minecraft.parrot", "entity.minecraft.chicken", "vc.val.crystaldragon", "vc.val.cloudspirit",
            "vc.pet.panda", "vc.pet.redpanda", "vc.pet.penguin", "vc.pet.mochi",
            "entity.minecraft.allay", "entity.minecraft.bee", "entity.minecraft.axolotl", "entity.minecraft.turtle"};
    private static final LivingEntity[] entities = new LivingEntity[NAMEN.length];
    private static Object world;

    public static boolean custom(int kind) { return kind >= 7 && kind <= 12; }
    public static boolean flying(int kind) { return kind == 5 || kind == 7 || kind == 8 || kind == 13 || kind == 14; }
    private Begleiter() {}

    public static LivingEntity get() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int selected = Math.max(0, Math.min(NAMEN.length - 1, MiniMe.config().miniMeArt));
        if (mc.world == null || selected == 0 || custom(selected)) {
            return null;
        }
        if (world != mc.world) { reset(); world = mc.world; }
        if (entities[selected] == null) {
            entities[selected] = switch (selected) {
                case 1 -> new CatEntity(EntityType.CAT, mc.world);
                case 2 -> new WolfEntity(EntityType.WOLF, mc.world);
                case 3 -> new FoxEntity(EntityType.FOX, mc.world);
                case 4 -> new RabbitEntity(EntityType.RABBIT, mc.world);
                case 5 -> new ParrotEntity(EntityType.PARROT, mc.world);
                case 13 -> new AllayEntity(EntityType.ALLAY, mc.world);
                case 14 -> new BeeEntity(EntityType.BEE, mc.world);
                case 15 -> new AxolotlEntity(EntityType.AXOLOTL, mc.world);
                case 16 -> new TurtleEntity(EntityType.TURTLE, mc.world);
                default -> new ChickenEntity(EntityType.CHICKEN, mc.world);
            };
        }
        LivingEntity entity = entities[selected];
        var owner = MiniMe.ownerId() == null ? mc.player : mc.world.getPlayerByUuid(MiniMe.ownerId());
        if (owner != null) {
            entity.setPosition(owner.getX(), owner.getY(), owner.getZ());
            entity.age = owner.age;
            entity.setYaw(owner.getBodyYaw());
            entity.setBodyYaw(owner.getBodyYaw());
            entity.setHeadYaw(owner.getBodyYaw());
        }
        if (entity instanceof TameableEntity tameable) tameable.setInSittingPose(MiniMe.sitzt());
        if (entity instanceof FoxEntity fox) fox.setSitting(MiniMe.sitzt());
        return entity;
    }

    private static final java.util.Map<Object, net.minecraft.client.render.entity.state.EntityRenderState[]> states = new java.util.WeakHashMap<>();
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static net.minecraft.client.render.entity.state.EntityRenderState state(net.minecraft.client.render.entity.EntityRenderer renderer,
            LivingEntity pet, Object ownerState) {
        int kind = MiniMe.config().miniMeArt;
        var owned = states.computeIfAbsent(ownerState, key -> new net.minecraft.client.render.entity.state.EntityRenderState[NAMEN.length]);
        if (owned[kind] == null) owned[kind] = renderer.createRenderState();
        renderer.updateRenderState(pet, owned[kind], 1.0f);
        return owned[kind];
    }

    public static void reset() { java.util.Arrays.fill(entities, null); states.clear(); world = null; }
}
