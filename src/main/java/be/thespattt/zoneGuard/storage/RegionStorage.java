package be.thespattt.zoneGuard.storage;

import be.thespattt.zoneGuard.model.FlagState;
import be.thespattt.zoneGuard.model.Point2D;
import be.thespattt.zoneGuard.model.ProtectedRegion;
import be.thespattt.zoneGuard.model.RegionType;
import be.thespattt.zoneGuard.model.ZoneData;
import be.thespattt.zoneGuard.model.ZoneFlag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class RegionStorage {
    private final Plugin plugin;
    private final File file;

    public RegionStorage(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "regions.yml");
    }

    public ZoneData load() {
        if (!file.exists()) {
            return new ZoneData();
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Map<String, List<ProtectedRegion>> regionsByWorld = new HashMap<>();
        Map<String, Map<ZoneFlag, FlagState>> globalsByWorld = new HashMap<>();

        ConfigurationSection worlds = yaml.getConfigurationSection("worlds");
        if (worlds == null) {
            return new ZoneData();
        }
        for (String world : worlds.getKeys(false)) {
            ConfigurationSection worldSection = worlds.getConfigurationSection(world);
            if (worldSection == null) {
                continue;
            }
            globalsByWorld.put(world, readFlags(worldSection.getConfigurationSection("global-flags")));
            ConfigurationSection regionsSection = worldSection.getConfigurationSection("regions");
            if (regionsSection == null) {
                continue;
            }
            List<ProtectedRegion> regions = new ArrayList<>();
            for (String name : regionsSection.getKeys(false)) {
                ConfigurationSection section = regionsSection.getConfigurationSection(name);
                if (section == null) {
                    continue;
                }
                ProtectedRegion region = readRegion(name, world, section);
                if (region != null) {
                    regions.add(region);
                }
            }
            regionsByWorld.put(world, regions);
        }
        return new ZoneData(regionsByWorld, globalsByWorld);
    }

    public void save(ZoneData data) {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Impossible de créer le dossier de données ZoneGuard.");
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        data.globalFlagsByWorld().forEach((world, flags) -> writeFlags(yaml.createSection("worlds." + world + ".global-flags"), flags));
        data.regionsByWorld().forEach((world, regions) -> {
            for (ProtectedRegion region : regions) {
                writeRegion(yaml.createSection("worlds." + world + ".regions." + region.name()), region);
            }
        });
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Impossible de sauvegarder les zones ZoneGuard.", exception);
        }
    }

    private ProtectedRegion readRegion(String name, String world, ConfigurationSection section) {
        RegionType type = RegionType.valueOf(section.getString("type", RegionType.CUBOID.name()));
        int priority = section.getInt("priority", 0);
        ProtectedRegion region;
        if (type == RegionType.CUBOID) {
            region = ProtectedRegion.cuboid(
                    name,
                    world,
                    priority,
                    new be.thespattt.zoneGuard.model.BlockPoint(world, section.getInt("min.x"), section.getInt("min.y"), section.getInt("min.z")),
                    new be.thespattt.zoneGuard.model.BlockPoint(world, section.getInt("max.x"), section.getInt("max.y"), section.getInt("max.z"))
            );
        } else {
            List<Point2D> points = new ArrayList<>();
            for (Map<?, ?> rawPoint : section.getMapList("points")) {
                Object x = rawPoint.get("x");
                Object z = rawPoint.get("z");
                if (x instanceof Number xNumber && z instanceof Number zNumber) {
                    points.add(new Point2D(xNumber.intValue(), zNumber.intValue()));
                }
            }
            if (points.size() < 3) {
                return null;
            }
            region = ProtectedRegion.polygon(name, world, priority, section.getInt("min.y"), section.getInt("max.y"), points);
        }
        for (Map.Entry<ZoneFlag, FlagState> flag : readFlags(section.getConfigurationSection("flags")).entrySet()) {
            region = region.withFlag(flag.getKey(), flag.getValue());
        }
        return region;
    }

    private void writeRegion(ConfigurationSection section, ProtectedRegion region) {
        section.set("type", region.type().name());
        section.set("priority", region.priority());
        section.set("min.x", region.minX());
        section.set("min.y", region.minY());
        section.set("min.z", region.minZ());
        section.set("max.x", region.maxX());
        section.set("max.y", region.maxY());
        section.set("max.z", region.maxZ());
        if (region.type() == RegionType.POLYGON) {
            List<Map<String, Integer>> points = region.polygonPoints().stream()
                    .map(point -> Map.of("x", point.x(), "z", point.z()))
                    .toList();
            section.set("points", points);
        }
        writeFlags(section.createSection("flags"), region.flags());
    }

    private Map<ZoneFlag, FlagState> readFlags(ConfigurationSection section) {
        EnumMap<ZoneFlag, FlagState> flags = new EnumMap<>(ZoneFlag.class);
        if (section == null) {
            return flags;
        }
        for (String key : section.getKeys(false)) {
            ZoneFlag.fromKey(key).ifPresent(flag -> flags.put(flag, FlagState.valueOf(section.getString(key, FlagState.UNSET.name()))));
        }
        return flags;
    }

    private void writeFlags(ConfigurationSection section, Map<ZoneFlag, FlagState> flags) {
        flags.forEach((flag, state) -> section.set(flag.key(), state.name()));
    }
}
