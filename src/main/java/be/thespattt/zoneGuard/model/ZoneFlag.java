package be.thespattt.zoneGuard.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum ZoneFlag {
    BLOCK_BREAK("Autorise ou bloque la destruction de blocs."),
    BLOCK_PLACE("Autorise ou bloque la pose de blocs."),
    INTERACT("Autorise ou bloque l'interaction avec les blocs et entités interactives."),
    SIGN("Autorise ou bloque la modification, teinture et édition des panneaux."),
    EXPLOSIONS("Autorise ou bloque les dégâts des explosions sur les blocs."),
    FIRE_SPREAD("Autorise ou bloque l'allumage et la propagation du feu."),
    MONSTER_SPAWN("Autorise ou bloque l'apparition des monstres."),
    ANIMAL_SPAWN("Autorise ou bloque l'apparition des animaux."),
    HUNGER("Autorise ou bloque la perte de faim des joueurs."),
    DAMAGE("Autorise ou bloque les dégâts généraux hors combat."),
    FALL_DAMAGE("Autorise ou bloque les dégâts de chute."),
    PVE("Autorise ou bloque les dégâts des entités contre les joueurs."),
    PVP("Autorise ou bloque les combats entre joueurs.");

    private final String description;

    ZoneFlag(String description) {
        this.description = description;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public String description() {
        return description;
    }

    public static Optional<ZoneFlag> fromKey(String key) {
        String normalized = key.toUpperCase(Locale.ROOT).replace('-', '_');
        return Arrays.stream(values()).filter(flag -> flag.name().equals(normalized)).findFirst();
    }
}
