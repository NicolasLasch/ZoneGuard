package be.thespattt.zoneGuard;

import be.thespattt.zoneGuard.command.ZoneGuardCommand;
import be.thespattt.zoneGuard.listener.ProtectionListener;
import be.thespattt.zoneGuard.listener.SelectionListener;
import be.thespattt.zoneGuard.selection.SelectionManager;
import be.thespattt.zoneGuard.selection.SelectionVisualizer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class ZoneGuard extends JavaPlugin {
    private ZoneService zoneService;
    private SelectionManager selectionManager;
    private SelectionVisualizer selectionVisualizer;

    @Override
    public void onEnable() {
        zoneService = new ZoneService(this);
        selectionManager = new SelectionManager();
        selectionVisualizer = new SelectionVisualizer(this, selectionManager);
        zoneService.load();

        ZoneGuardCommand command = new ZoneGuardCommand(zoneService, selectionManager, selectionVisualizer);
        registerCommand("zg", "Gérer les sélections, zones et flags ZoneGuard.", List.of("zoneguard"), command);

        getServer().getPluginManager().registerEvents(new SelectionListener(selectionManager), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(zoneService), this);
        getLogger().info("ZoneGuard activé avec stockage compatible Folia.");
    }

    @Override
    public void onDisable() {
        if (zoneService != null) {
            zoneService.shutdown();
        }
        if (selectionVisualizer != null) {
            selectionVisualizer.stopAll();
        }
    }
}
