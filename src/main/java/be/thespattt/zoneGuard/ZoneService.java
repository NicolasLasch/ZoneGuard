package be.thespattt.zoneGuard;

import be.thespattt.zoneGuard.model.FlagState;
import be.thespattt.zoneGuard.model.ProtectedRegion;
import be.thespattt.zoneGuard.model.ZoneData;
import be.thespattt.zoneGuard.model.ZoneFlag;
import be.thespattt.zoneGuard.storage.RegionStorage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class ZoneService {
    private final RegionStorage storage;
    private final ExecutorService saveExecutor;
    private final AtomicReference<ZoneData> data = new AtomicReference<>(new ZoneData());

    public ZoneService(Plugin plugin) {
        this.storage = new RegionStorage(plugin);
        this.saveExecutor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "ZoneGuard Storage");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void load() {
        data.set(storage.load());
    }

    public void saveAsync() {
        ZoneData snapshot = data.get();
        saveExecutor.execute(() -> storage.save(snapshot));
    }

    public void shutdown() {
        saveAsync();
        saveExecutor.shutdown();
        try {
            if (!saveExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                saveExecutor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            saveExecutor.shutdownNow();
        }
    }

    public ZoneData snapshot() {
        return data.get();
    }

    public void defineRegion(ProtectedRegion region) {
        data.updateAndGet(current -> current.upsertRegion(region));
        saveAsync();
    }

    public boolean setRegionFlag(String regionName, ZoneFlag flag, FlagState state) {
        if (data.get().findRegion(regionName).isEmpty()) {
            return false;
        }
        data.updateAndGet(current -> current.updateRegionFlag(regionName, flag, state));
        saveAsync();
        return true;
    }

    public boolean deleteRegion(String regionName) {
        if (data.get().findRegion(regionName).isEmpty()) {
            return false;
        }
        data.updateAndGet(current -> current.deleteRegion(regionName));
        saveAsync();
        return true;
    }

    public void setGlobalFlag(World world, ZoneFlag flag, FlagState state) {
        data.updateAndGet(current -> current.updateGlobalFlag(world.getName(), flag, state));
        saveAsync();
    }

    public boolean isDenied(Location location, ZoneFlag flag) {
        return resolve(location, flag) == FlagState.DENY;
    }

    public FlagState resolve(Location location, ZoneFlag flag) {
        World world = location.getWorld();
        if (world == null) {
            return FlagState.UNSET;
        }
        ZoneData snapshot = data.get();
        String worldName = world.getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        for (ProtectedRegion region : snapshot.regions(worldName)) {
            if (region.contains(worldName, x, y, z)) {
                FlagState regionState = region.flags().get(flag);
                if (regionState != null) {
                    return regionState;
                }
            }
        }
        return Optional.ofNullable(snapshot.globalFlagsByWorld().get(worldName))
                .map(flags -> flags.get(flag))
                .orElse(FlagState.UNSET);
    }

    public Map<ZoneFlag, FlagState> globalFlags(World world) {
        return data.get().globalFlagsByWorld().getOrDefault(world.getName(), Map.of());
    }
}
