package be.thespattt.zoneGuard.listener;

import be.thespattt.zoneGuard.model.BlockPoint;
import be.thespattt.zoneGuard.selection.PlayerSelection;
import be.thespattt.zoneGuard.selection.SelectionManager;
import be.thespattt.zoneGuard.selection.SelectionMode;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class SelectionListener implements Listener {
    private static final String PREFIX = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "ZoneGuard " + ChatColor.DARK_GRAY + ">> " + ChatColor.RESET;
    private static final String SUCCESS = ChatColor.GREEN.toString();
    private static final String ERROR = ChatColor.RED.toString();
    private static final String VALUE = ChatColor.YELLOW.toString();

    private final SelectionManager selectionManager;

    public SelectionListener(SelectionManager selectionManager) {
        this.selectionManager = selectionManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("zoneguard.admin") || event.getItem() == null || event.getItem().getType() != Material.WOODEN_AXE) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }
        PlayerSelection selection = selectionManager.selection(player.getUniqueId());
        BlockPoint point = new BlockPoint(clicked.getWorld().getName(), clicked.getX(), clicked.getY(), clicked.getZ());
        if (selection.mode() == SelectionMode.CUBOID) {
            handleCuboid(event, player, selection, point);
        } else {
            handlePolygon(event, player, selection, point);
        }
    }

    private void handleCuboid(PlayerInteractEvent event, Player player, PlayerSelection selection, BlockPoint point) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            selection.setFirst(point);
            player.sendMessage(PREFIX + SUCCESS + "Position 1 définie : " + VALUE + format(point) + SUCCESS + ".");
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            selection.setSecond(point);
            player.sendMessage(PREFIX + SUCCESS + "Position 2 définie : " + VALUE + format(point) + SUCCESS + ".");
        }
    }

    private void handlePolygon(PlayerInteractEvent event, Player player, PlayerSelection selection, BlockPoint point) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            if (selection.world().isPresent() && !selection.world().orElseThrow().equals(point.world())) {
                player.sendMessage(PREFIX + ERROR + "Tous les points du polygone doivent être dans le même monde.");
                return;
            }
            selection.addPolygonPoint(point);
            player.sendMessage(PREFIX + SUCCESS + "Point polygonal #" + selection.polygonBlockPoints().size() + " ajouté : " + VALUE + format(point) + SUCCESS + ".");
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            selection.closePolygon();
            if (selection.polygonClosed()) {
                player.sendMessage(PREFIX + SUCCESS + "Polygone fermé avec " + VALUE + selection.polygonBlockPoints().size() + SUCCESS + " points.");
            } else {
                player.sendMessage(PREFIX + ERROR + "Ajoute au moins 3 points avant de fermer un polygone.");
            }
        }
    }

    private String format(BlockPoint point) {
        return point.x() + ", " + point.y() + ", " + point.z();
    }
}
