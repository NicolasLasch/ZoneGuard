package be.thespattt.zoneGuard.selection;

import be.thespattt.zoneGuard.model.BlockPoint;
import be.thespattt.zoneGuard.model.Point2D;
import be.thespattt.zoneGuard.model.ProtectedRegion;
import be.thespattt.zoneGuard.model.RegionType;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SelectionVisualizer {
    private static final double STEP = 0.75D;
    private static final double HEIGHT_OFFSET = 1.08D;
    private static final Particle.DustOptions CUBOID_DUST = new Particle.DustOptions(Color.fromRGB(80, 220, 255), 1.2F);
    private static final Particle.DustOptions POLYGON_DUST = new Particle.DustOptions(Color.fromRGB(255, 200, 60), 1.2F);
    private static final Particle.DustOptions POINT_DUST = new Particle.DustOptions(Color.fromRGB(120, 255, 120), 1.6F);

    private final Plugin plugin;
    private final SelectionManager selectionManager;
    private final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> regionPreviewTasks = new ConcurrentHashMap<>();

    public SelectionVisualizer(Plugin plugin, SelectionManager selectionManager) {
        this.plugin = plugin;
        this.selectionManager = selectionManager;
    }

    public void start(Player player) {
        if (tasks.containsKey(player.getUniqueId())) {
            return;
        }
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
            if (!player.isOnline()) {
                stop(player.getUniqueId());
                return;
            }
            draw(player, selectionManager.selection(player.getUniqueId()));
        }, null, 1L, 10L);
        if (task != null) {
            tasks.put(player.getUniqueId(), task);
        }
    }

    public void stop(UUID playerId) {
        ScheduledTask task = tasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    public void stopAll() {
        tasks.keySet().forEach(this::stop);
        regionPreviewTasks.keySet().forEach(this::stopRegionPreview);
    }

    public boolean showRegion(Player player, ProtectedRegion region) {
        World world = Bukkit.getWorld(region.world());
        if (world == null || !world.equals(player.getWorld())) {
            return false;
        }
        stopRegionPreview(player.getUniqueId());
        int[] ticks = {0};
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
            if (!player.isOnline() || !player.getWorld().equals(world) || ticks[0]++ >= 40) {
                stopRegionPreview(player.getUniqueId());
                return;
            }
            drawRegion(player, region, player.getLocation().getBlockY() + HEIGHT_OFFSET);
        }, null, 1L, 5L);
        if (task != null) {
            regionPreviewTasks.put(player.getUniqueId(), task);
        }
        return task != null;
    }

    private void stopRegionPreview(UUID playerId) {
        ScheduledTask task = regionPreviewTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    private void draw(Player player, PlayerSelection selection) {
        if (selection.mode() == SelectionMode.CUBOID) {
            drawCuboid(player, selection);
        } else {
            drawPolygon(player, selection);
        }
    }

    private void drawCuboid(Player player, PlayerSelection selection) {
        if (selection.first().isEmpty() && selection.second().isEmpty()) {
            return;
        }
        BlockPoint first = selection.first().or(() -> selection.second()).orElseThrow();
        BlockPoint second = selection.second().orElse(first);
        if (!first.world().equals(second.world()) || !first.world().equals(player.getWorld().getName())) {
            return;
        }
        double minX = Math.min(first.x(), second.x());
        double maxX = Math.max(first.x(), second.x()) + 1.0D;
        double minZ = Math.min(first.z(), second.z());
        double maxZ = Math.max(first.z(), second.z()) + 1.0D;
        double y = player.getLocation().getBlockY() + HEIGHT_OFFSET;
        drawFlatRectangle(player, player.getWorld(), minX, y, minZ, maxX, maxZ, CUBOID_DUST, true);
    }

    private void drawRegion(Player player, ProtectedRegion region, double y) {
        World world = player.getWorld();
        if (region.type() == RegionType.CUBOID) {
            double minX = region.minX();
            double maxX = region.maxX() + 1.0D;
            double minZ = region.minZ();
            double maxZ = region.maxZ() + 1.0D;
            drawFlatRectangle(player, world, minX, y, minZ, maxX, maxZ, CUBOID_DUST, true);
            return;
        }
        List<Point2D> points = region.polygonPoints();
        for (Point2D point : points) {
            drawPoint(player, world, point.x() + 0.5D, y, point.z() + 0.5D);
        }
        for (int i = 0; i < points.size(); i++) {
            Point2D from = points.get(i);
            Point2D to = points.get((i + 1) % points.size());
            drawLine(player, world, from.x() + 0.5D, y, from.z() + 0.5D, to.x() + 0.5D, y, to.z() + 0.5D, POLYGON_DUST);
        }
    }

    private void drawPolygon(Player player, PlayerSelection selection) {
        List<BlockPoint> points = selection.polygonBlockPoints();
        if (points.isEmpty() || !points.get(0).world().equals(player.getWorld().getName())) {
            return;
        }
        double y = points.get(0).y() + HEIGHT_OFFSET;
        World world = player.getWorld();
        for (BlockPoint point : points) {
            drawPoint(player, world, point.x() + 0.5D, y, point.z() + 0.5D);
        }
        for (int i = 0; i < points.size() - 1; i++) {
            BlockPoint from = points.get(i);
            BlockPoint to = points.get(i + 1);
            drawLine(player, world, from.x() + 0.5D, y, from.z() + 0.5D, to.x() + 0.5D, y, to.z() + 0.5D, POLYGON_DUST);
        }
        if (selection.polygonClosed() && points.size() >= 3) {
            BlockPoint last = points.get(points.size() - 1);
            BlockPoint first = points.get(0);
            drawLine(player, world, last.x() + 0.5D, y, last.z() + 0.5D, first.x() + 0.5D, y, first.z() + 0.5D, POLYGON_DUST);
        }
    }

    private void drawLine(Player player, World world, double startX, double startY, double startZ, double endX, double endY, double endZ, Particle.DustOptions dust) {
        double deltaX = endX - startX;
        double deltaY = endY - startY;
        double deltaZ = endZ - startZ;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        int samples = Math.max(1, (int) Math.ceil(distance / STEP));
        for (int i = 0; i <= samples; i++) {
            double ratio = (double) i / samples;
            Location location = new Location(world, startX + deltaX * ratio, startY + deltaY * ratio, startZ + deltaZ * ratio);
            player.spawnParticle(Particle.DUST, location, 1, 0.0D, 0.0D, 0.0D, 0.0D, dust);
        }
    }

    private void drawFlatRectangle(Player player, World world, double minX, double y, double minZ, double maxX, double maxZ, Particle.DustOptions dust, boolean corners) {
        drawLine(player, world, minX, y, minZ, maxX, y, minZ, dust);
        drawLine(player, world, maxX, y, minZ, maxX, y, maxZ, dust);
        drawLine(player, world, maxX, y, maxZ, minX, y, maxZ, dust);
        drawLine(player, world, minX, y, maxZ, minX, y, minZ, dust);
        if (corners) {
            drawPoint(player, world, minX, y, minZ);
            drawPoint(player, world, maxX, y, minZ);
            drawPoint(player, world, maxX, y, maxZ);
            drawPoint(player, world, minX, y, maxZ);
        }
    }

    private void drawPoint(Player player, World world, double x, double y, double z) {
        player.spawnParticle(Particle.DUST, new Location(world, x, y, z), 3, 0.08D, 0.08D, 0.08D, 0.0D, POINT_DUST);
    }
}
