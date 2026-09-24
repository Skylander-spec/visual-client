package dev.visual.fabric;

import java.util.*;

/** Bounded cosmetic navigation; never moves an actual server entity. */
public final class CompanionMotion {
    public interface Terrain {
        /** A supported, clear standing position, or NaN. */
        double floor(double x, double z, double nearY, double up, double down);
        boolean clear(double x, double y, double z);
    }
    public double x, y, z, previousX, previousY, previousZ;
    public float yaw, previousYaw, step, movement;
    private double gravity;
    private int ticks;
    private boolean ready, wasFlying;
    private List<Point> route = List.of();
    private int routeIndex;
    private double goalX, goalZ, goalY;
    private record Point(double x, double y, double z) {}
    private record Cell(int x, int y, int z) {}
    private record Node(Point point, Node parent, double cost, double estimate) {}

    public void tick(Terrain terrain, double ownerX, double ownerY, double ownerZ, float ownerYaw, boolean flying) {
        double angle = Math.toRadians(ownerYaw);
        double targetX = ownerX + Math.sin(angle) * 1.15;
        double targetZ = ownerZ - Math.cos(angle) * 1.15;
        double targetY = flying ? ownerY + 1.1 : terrain.floor(targetX, targetZ, ownerY, 2, 6);
        if (!Double.isFinite(targetY)) {
            // A dug-out owner may have no space directly behind them. Try the sides.
            for (int side : new int[]{1, -1}) {
                double sx = ownerX + Math.cos(angle) * side * .7;
                double sz = ownerZ + Math.sin(angle) * side * .7;
                double sy = terrain.floor(sx, sz, ownerY, 1, 3);
                if (Double.isFinite(sy)) { targetX = sx; targetZ = sz; targetY = sy; break; }
            }
        }
        if (!Double.isFinite(targetY)) {
            double besideOwner = terrain.floor(ownerX, ownerZ, ownerY, .5, 3);
            if (Double.isFinite(besideOwner)) { targetX = ownerX; targetZ = ownerZ; targetY = besideOwner; }
        }
        if (!ready || distance(x, y, z, ownerX, ownerY, ownerZ) > 12) {
            if (!Double.isFinite(targetY) || !terrain.clear(targetX, targetY, targetZ)) { ready = false; return; }
            x = previousX = targetX; y = previousY = targetY; z = previousZ = targetZ;
            yaw = previousYaw = ownerYaw;
            ready = true; wasFlying = flying; route = List.of(); movement = 0; return;
        }
        previousX = x; previousY = y; previousZ = z; previousYaw = yaw;
        ticks++;
        if (flying) {
            route = List.of(); gravity = 0;
            double dx = targetX - x, dy = targetY - y, dz = targetZ - z;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double speed = Math.min(.24, length * .18);
            if (length > .025) {
                double nx = x + dx / length * speed, ny = y + dy / length * speed, nz = z + dz / length * speed;
                if (terrain.clear(nx, ny, nz)) { x = nx; y = ny; z = nz; }
                else if (terrain.clear(x, y + .15, z) && y < ownerY + 3) y += .15;
            }
        } else {
            if (Double.isFinite(targetY) && (wasFlying
                    || (ticks % 12 == 0 && distance(x, y, z, targetX, targetY, targetZ) > .2)
                    || Math.hypot(goalX - targetX, goalZ - targetZ) > .65
                    || Math.abs(goalY - targetY) > .6)) {
                goalX = targetX; goalZ = targetZ; goalY = targetY;
                route = findRoute(terrain, new Point(x, y, z), new Point(targetX, targetY, targetZ));
                routeIndex = 0;
            }
            Point next = routeIndex < route.size() ? route.get(routeIndex) : null;
            if (next != null) {
                double dx = next.x - x, dz = next.z - z, length = Math.hypot(dx, dz);
                double speed = Math.min(.16, length);
                if (length < .08) routeIndex++;
                else {
                    double nx = x + dx / length * speed, nz = z + dz / length * speed;
                    double floor = terrain.floor(nx, nz, y, 1.01, 3);
                    double ny = Double.isFinite(floor) && floor > y ? floor : y;
                    if (Double.isFinite(floor) && terrain.clear(nx, ny, nz)) { x = nx; y = ny; z = nz; }
                    else route = List.of();
                }
            }
            double floor = terrain.floor(x, z, y, .05, 8);
            if (!Double.isFinite(floor) || y > floor + .01) {
                gravity = Math.min(.6, gravity + .08);
                double ny = Double.isFinite(floor) ? Math.max(floor, y - gravity) : y - gravity;
                if (terrain.clear(x, ny, z)) y = ny;
            } else gravity = 0;
        }
        wasFlying = flying;
        double dx = x - previousX, dz = z - previousZ, length = Math.hypot(dx, dz);
        movement = (float) Math.min(.6, length * 5);
        step += (float) (length * 8);
        float targetYaw = length > .003 ? (float) Math.toDegrees(Math.atan2(-dx, dz)) : ownerYaw;
        float change = (targetYaw - yaw + 540) % 360 - 180;
        yaw += change * .25f;
    }

    public boolean ready() { return ready; }

    private static List<Point> findRoute(Terrain world, Point start, Point goal) {
        if (Math.hypot(start.x - goal.x, start.z - goal.z) < .06) return List.of();
        if (line(world, start, goal)) return List.of(goal);
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::estimate));
        Map<Cell, Double> best = new HashMap<>();
        Node first = new Node(start, null, 0, distance(start, goal));
        open.add(first);
        best.put(cell(start), 0d);
        Node closest = first;
        for (int count = 0; count < 96 && !open.isEmpty(); count++) {
            Node node = open.poll();
            if (distance(node.point, goal) < distance(closest.point, goal)) closest = node;
            if (distance(node.point, goal) < 1.5 && line(world, node.point, goal))
                return path(new Node(goal, node, 0, 0));
            for (int[] offset : DIRECTIONS) {
                double nx = Math.floor(node.point.x) + .5 + offset[0];
                double nz = Math.floor(node.point.z) + .5 + offset[1];
                if (Math.hypot(nx - start.x, nz - start.z) > 7) continue;
                double ny = world.floor(nx, nz, node.point.y, 1.01, 3);
                if (!Double.isFinite(ny)) continue;
                Point p = new Point(nx, ny, nz);
                if (!line(world, node.point, p)) continue;
                double cost = node.cost + distance(node.point, p);
                Cell key = cell(p);
                if (cost >= best.getOrDefault(key, Double.POSITIVE_INFINITY)) continue;
                best.put(key, cost);
                open.add(new Node(p, node, cost, cost + distance(p, goal)));
            }
        }
        return closest == first ? List.of() : path(closest);
    }

    private static final int[][] DIRECTIONS = {{1,0},{-1,0},{0,1},{0,-1}};
    private static boolean line(Terrain world, Point a, Point b) {
        double length = Math.hypot(b.x - a.x, b.z - a.z), lastY = a.y;
        int samples = Math.max(1, (int) Math.ceil(length / .2));
        for (int i = 1; i <= samples; i++) {
            double x = a.x + (b.x - a.x) * i / samples, z = a.z + (b.z - a.z) * i / samples;
            double y = world.floor(x, z, lastY, 1.01, 3);
            if (!Double.isFinite(y) || !world.clear(x, Math.max(lastY, y), z)) return false;
            lastY = y;
        }
        return Math.abs(lastY - b.y) < .15;
    }
    private static List<Point> path(Node node) {
        ArrayList<Point> result = new ArrayList<>();
        while (node.parent != null) { result.add(node.point); node = node.parent; }
        Collections.reverse(result); return result;
    }
    private static Cell cell(Point p) { return new Cell((int) Math.floor(p.x), (int) Math.floor(p.y * 2), (int) Math.floor(p.z)); }
    private static double distance(Point a, Point b) { return distance(a.x, a.y, a.z, b.x, b.y, b.z); }
    private static double distance(double x, double y, double z, double a, double b, double c) {
        return Math.sqrt((x-a)*(x-a)+(y-b)*(y-b)+(z-c)*(z-c));
    }
}
