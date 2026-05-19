package be.thespattt.zoneGuard.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ProtectedRegion {
    private final String name;
    private final String world;
    private final RegionType type;
    private final int priority;
    private final int minX;
    private final int maxX;
    private final int minY;
    private final int maxY;
    private final int minZ;
    private final int maxZ;
    private final List<Point2D> polygonPoints;
    private final Map<ZoneFlag, FlagState> flags;

    private ProtectedRegion(
            String name,
            String world,
            RegionType type,
            int priority,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ,
            List<Point2D> polygonPoints,
            Map<ZoneFlag, FlagState> flags
    ) {
        this.name = name;
        this.world = world;
        this.type = type;
        this.priority = priority;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.polygonPoints = List.copyOf(polygonPoints);
        EnumMap<ZoneFlag, FlagState> flagsCopy = new EnumMap<>(ZoneFlag.class);
        flagsCopy.putAll(flags);
        this.flags = Collections.unmodifiableMap(flagsCopy);
    }

    public static ProtectedRegion cuboid(String name, String world, int priority, BlockPoint first, BlockPoint second) {
        return new ProtectedRegion(
                name,
                world,
                RegionType.CUBOID,
                priority,
                Math.min(first.x(), second.x()),
                Math.max(first.x(), second.x()),
                Math.min(first.y(), second.y()),
                Math.max(first.y(), second.y()),
                Math.min(first.z(), second.z()),
                Math.max(first.z(), second.z()),
                List.of(),
                new EnumMap<>(ZoneFlag.class)
        );
    }

    public static ProtectedRegion polygon(String name, String world, int priority, int minY, int maxY, List<Point2D> points) {
        int minX = points.stream().mapToInt(Point2D::x).min().orElse(0);
        int maxX = points.stream().mapToInt(Point2D::x).max().orElse(0);
        int minZ = points.stream().mapToInt(Point2D::z).min().orElse(0);
        int maxZ = points.stream().mapToInt(Point2D::z).max().orElse(0);
        return new ProtectedRegion(
                name,
                world,
                RegionType.POLYGON,
                priority,
                minX,
                maxX,
                Math.min(minY, maxY),
                Math.max(minY, maxY),
                minZ,
                maxZ,
                points,
                new EnumMap<>(ZoneFlag.class)
        );
    }

    public ProtectedRegion withFlag(ZoneFlag flag, FlagState state) {
        EnumMap<ZoneFlag, FlagState> nextFlags = new EnumMap<>(ZoneFlag.class);
        nextFlags.putAll(flags);
        if (state == FlagState.UNSET) {
            nextFlags.remove(flag);
        } else {
            nextFlags.put(flag, state);
        }
        return new ProtectedRegion(name, world, type, priority, minX, maxX, minY, maxY, minZ, maxZ, polygonPoints, nextFlags);
    }

    public boolean contains(String worldName, int x, int y, int z) {
        if (!world.equals(worldName) || y < minY || y > maxY || x < minX || x > maxX || z < minZ || z > maxZ) {
            return false;
        }
        return type == RegionType.CUBOID || isInsidePolygon(x + 0.5D, z + 0.5D);
    }

    private boolean isInsidePolygon(double x, double z) {
        boolean inside = false;
        for (int i = 0, j = polygonPoints.size() - 1; i < polygonPoints.size(); j = i++) {
            Point2D current = polygonPoints.get(i);
            Point2D previous = polygonPoints.get(j);
            boolean crossesZ = (current.z() > z) != (previous.z() > z);
            if (crossesZ) {
                double intersectionX = (double) (previous.x() - current.x()) * (z - current.z()) / (previous.z() - current.z()) + current.x();
                if (x < intersectionX) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    public String name() {
        return name;
    }

    public String world() {
        return world;
    }

    public RegionType type() {
        return type;
    }

    public int priority() {
        return priority;
    }

    public int minX() {
        return minX;
    }

    public int maxX() {
        return maxX;
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    public int minZ() {
        return minZ;
    }

    public int maxZ() {
        return maxZ;
    }

    public List<Point2D> polygonPoints() {
        return polygonPoints;
    }

    public Map<ZoneFlag, FlagState> flags() {
        return flags;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProtectedRegion that)) {
            return false;
        }
        return name.equalsIgnoreCase(that.name) && world.equals(that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase(), world);
    }
}
