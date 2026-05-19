package be.thespattt.zoneGuard.selection;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SelectionManager {
    private final Map<UUID, PlayerSelection> selections = new ConcurrentHashMap<>();

    public PlayerSelection selection(UUID playerId) {
        return selections.computeIfAbsent(playerId, ignored -> new PlayerSelection());
    }
}
