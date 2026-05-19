package be.thespattt.zoneGuard.util;

import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;

public final class EntityTypes {
    private EntityTypes() {
    }

    public static boolean isMonster(Entity entity) {
        return entity instanceof Monster;
    }

    public static boolean isAnimal(Entity entity) {
        return entity instanceof Animals;
    }
}
