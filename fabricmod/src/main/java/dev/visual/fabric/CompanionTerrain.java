package dev.visual.fabric;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/** Collision shapes support slabs, stairs and uneven ground without spawning mobs. */
public final class CompanionTerrain implements CompanionMotion.Terrain {
    private final ClientWorld world;
    private final double radius, height;
    private final BlockPos.Mutable pos = new BlockPos.Mutable();
    public CompanionTerrain(ClientWorld world, double scale) {
        this.world = world; radius = Math.max(.12, .3 * scale); height = Math.max(.35, 1.8 * scale);
    }
    @Override public boolean clear(double x, double y, double z) {
        return world.isSpaceEmpty(new Box(x-radius, y+.001, z-radius, x+radius, y+height, z+radius));
    }
    @Override public double floor(double x, double z, double nearY, double up, double down) {
        double highest = nearY + up;
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        for (int by = (int) Math.floor(highest); by >= Math.floor(nearY-down)-1; by--) {
            pos.set(bx, by, bz);
            if (!world.getBlockState(pos).getFluidState().isEmpty()) continue;
            for (Box shape : world.getBlockState(pos).getCollisionShape(world, pos).getBoundingBoxes()) {
                double y = by + shape.maxY;
                if (y > highest + .001 || y < nearY-down || x < bx+shape.minX || x > bx+shape.maxX
                        || z < bz+shape.minZ || z > bz+shape.maxZ) continue;
                if (clear(x, y, z)) return y;
            }
        }
        return Double.NaN;
    }
}
