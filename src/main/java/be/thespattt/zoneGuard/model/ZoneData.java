package be.thespattt.zoneGuard.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ZoneData {
    private final Map<String, List<ProtectedRegion>> regionsByWorld;
    private final Map<String, Map<ZoneFlag, FlagState>> globalFlagsByWorld;

    public ZoneData() {
        this(Map.of(), Map.of());
    }

    public ZoneData(Map<String, List<ProtectedRegion>> regionsByWorld, Map<String, Map<ZoneFlag, FlagState>> globalFlagsByWorld) {
        Map<String, List<ProtectedRegion>> regionsCopy = new HashMap<>();
        regionsByWorld.forEach((world, regions) -> {
            List<ProtectedRegion> sorted = new ArrayList<>(regions);
            sorted.sort(Comparator.comparingInt(ProtectedRegion::priority).reversed());
            regionsCopy.put(world, List.copyOf(sorted));
        });
        Map<String, Map<ZoneFlag, FlagState>> globalsCopy = new HashMap<>();
        globalFlagsByWorld.forEach((world, flags) -> {
            EnumMap<ZoneFlag, FlagState> flagsCopy = new EnumMap<>(ZoneFlag.class);
            flagsCopy.putAll(flags);
            globalsCopy.put(world, Collections.unmodifiableMap(flagsCopy));
        });
        this.regionsByWorld = Collections.unmodifiableMap(regionsCopy);
        this.globalFlagsByWorld = Collections.unmodifiableMap(globalsCopy);
    }

    public List<ProtectedRegion> regions(String world) {
        return regionsByWorld.getOrDefault(world, List.of());
    }

    public Map<String, List<ProtectedRegion>> regionsByWorld() {
        return regionsByWorld;
    }

    public Map<String, Map<ZoneFlag, FlagState>> globalFlagsByWorld() {
        return globalFlagsByWorld;
    }

    public Optional<ProtectedRegion> findRegion(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return regionsByWorld.values().stream()
                .flatMap(List::stream)
                .filter(region -> region.name().toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }

    public ZoneData upsertRegion(ProtectedRegion region) {
        Map<String, List<ProtectedRegion>> next = mutableRegions();
        List<ProtectedRegion> regions = new ArrayList<>(next.getOrDefault(region.world(), List.of()));
        regions.removeIf(existing -> existing.name().equalsIgnoreCase(region.name()));
        regions.add(region);
        next.put(region.world(), regions);
        return new ZoneData(next, globalFlagsByWorld);
    }

    public ZoneData updateRegionFlag(String name, ZoneFlag flag, FlagState state) {
        Map<String, List<ProtectedRegion>> next = mutableRegions();
        for (Map.Entry<String, List<ProtectedRegion>> entry : next.entrySet()) {
            List<ProtectedRegion> regions = new ArrayList<>(entry.getValue());
            for (int i = 0; i < regions.size(); i++) {
                if (regions.get(i).name().equalsIgnoreCase(name)) {
                    regions.set(i, regions.get(i).withFlag(flag, state));
                    next.put(entry.getKey(), regions);
                    return new ZoneData(next, globalFlagsByWorld);
                }
            }
        }
        return this;
    }

    public ZoneData deleteRegion(String name) {
        Map<String, List<ProtectedRegion>> next = mutableRegions();
        boolean changed = false;
        for (Map.Entry<String, List<ProtectedRegion>> entry : new ArrayList<>(next.entrySet())) {
            List<ProtectedRegion> regions = new ArrayList<>(entry.getValue());
            changed |= regions.removeIf(region -> region.name().equalsIgnoreCase(name));
            if (regions.isEmpty()) {
                next.remove(entry.getKey());
            } else {
                next.put(entry.getKey(), regions);
            }
        }
        return changed ? new ZoneData(next, globalFlagsByWorld) : this;
    }

    public ZoneData updateGlobalFlag(String world, ZoneFlag flag, FlagState state) {
        Map<String, Map<ZoneFlag, FlagState>> next = mutableGlobalFlags();
        EnumMap<ZoneFlag, FlagState> flags = new EnumMap<>(ZoneFlag.class);
        flags.putAll(next.getOrDefault(world, Map.of()));
        if (state == FlagState.UNSET) {
            flags.remove(flag);
        } else {
            flags.put(flag, state);
        }
        next.put(world, flags);
        return new ZoneData(regionsByWorld, next);
    }

    public int nextPriority(String world) {
        return regions(world).stream().mapToInt(ProtectedRegion::priority).max().orElse(0) + 1;
    }

    private Map<String, List<ProtectedRegion>> mutableRegions() {
        Map<String, List<ProtectedRegion>> next = new HashMap<>();
        regionsByWorld.forEach((world, regions) -> next.put(world, new ArrayList<>(regions)));
        return next;
    }

    private Map<String, Map<ZoneFlag, FlagState>> mutableGlobalFlags() {
        Map<String, Map<ZoneFlag, FlagState>> next = new HashMap<>();
        globalFlagsByWorld.forEach((world, flags) -> {
            EnumMap<ZoneFlag, FlagState> flagsCopy = new EnumMap<>(ZoneFlag.class);
            flagsCopy.putAll(flags);
            next.put(world, flagsCopy);
        });
        return next;
    }
}
