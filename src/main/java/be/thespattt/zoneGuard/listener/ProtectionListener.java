package be.thespattt.zoneGuard.listener;

import be.thespattt.zoneGuard.ZoneService;
import be.thespattt.zoneGuard.model.ZoneFlag;
import be.thespattt.zoneGuard.util.EntityTypes;
import io.papermc.paper.event.player.PlayerOpenSignEvent;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProtectionListener implements Listener {
    private static final String PREFIX = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "ZoneGuard " + ChatColor.DARK_GRAY + ">> " + ChatColor.RESET;
    private static final String ERROR = ChatColor.RED.toString();
    private static final long MESSAGE_COOLDOWN_MILLIS = 1500L;

    private final ZoneService zoneService;
    private final Map<UUID, Long> lastMessageByPlayer = new ConcurrentHashMap<>();

    public ProtectionListener(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (canBypassBuildProtection(event.getPlayer())) {
            return;
        }
        if (zoneService.isDenied(event.getBlock().getLocation(), ZoneFlag.BLOCK_BREAK)) {
            event.setCancelled(true);
            deny(event.getPlayer(), "Tu ne peux pas casser de blocs dans cette zone.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (canBypassBuildProtection(event.getPlayer())) {
            return;
        }
        if (zoneService.isDenied(event.getBlock().getLocation(), ZoneFlag.BLOCK_PLACE)) {
            event.setCancelled(true);
            deny(event.getPlayer(), "Tu ne peux pas poser de blocs dans cette zone.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (canBypassBuildProtection(player)) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Location location = block.getLocation();
        if (isCauldron(block) && isDenied(location, ZoneFlag.BLOCK_BREAK, ZoneFlag.BLOCK_PLACE, ZoneFlag.INTERACT)) {
            event.setCancelled(true);
            deny(player, "Tu ne peux pas modifier ce chaudron dans cette zone.");
            return;
        }
        if (isSign(block) && zoneService.isDenied(location, ZoneFlag.SIGN)) {
            event.setCancelled(true);
            deny(player, "Tu ne peux pas modifier ce panneau dans cette zone.");
            return;
        }
        if (isProtectedInteractionBlock(block) && zoneService.isDenied(location, ZoneFlag.INTERACT)) {
            event.setCancelled(true);
            deny(player, "Tu ne peux pas interagir avec ce bloc dans cette zone.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSignOpen(PlayerOpenSignEvent event) {
        Player player = event.getPlayer();
        if (canBypassBuildProtection(player)) {
            return;
        }
        if (zoneService.isDenied(event.getSign().getLocation(), ZoneFlag.SIGN)) {
            event.setCancelled(true);
            deny(player, "Tu ne peux pas éditer ce panneau dans cette zone.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (canBypassBuildProtection(player)) {
            return;
        }
        if (zoneService.isDenied(event.getBlock().getLocation(), ZoneFlag.SIGN)) {
            event.setCancelled(true);
            deny(player, "Tu ne peux pas modifier ce panneau dans cette zone.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        handleBucket(event, ZoneFlag.BLOCK_BREAK, "Tu ne peux pas récupérer de liquide dans cette zone.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        handleBucket(event, ZoneFlag.BLOCK_PLACE, "Tu ne peux pas déposer de liquide dans cette zone.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        Optional<Player> remover = attackingPlayer(event.getRemover());
        if (remover.isEmpty() || canBypassBuildProtection(remover.orElseThrow())) {
            return;
        }
        if (zoneService.isDenied(event.getEntity().getLocation(), ZoneFlag.BLOCK_BREAK)) {
            event.setCancelled(true);
            deny(remover.orElseThrow(), "Tu ne peux pas casser cet élément dans cette zone.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null || canBypassBuildProtection(player)) {
            return;
        }
        if (zoneService.isDenied(event.getEntity().getLocation(), ZoneFlag.BLOCK_PLACE)) {
            event.setCancelled(true);
            deny(player, "Tu ne peux pas poser cet élément dans cette zone.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (canBypassBuildProtection(player)) {
            return;
        }
        Entity clicked = event.getRightClicked();
        if (isProtectedFrameLikeEntity(clicked) && isDenied(clicked.getLocation(), ZoneFlag.BLOCK_BREAK, ZoneFlag.INTERACT)) {
            event.setCancelled(true);
            deny(player, "Tu ne peux pas modifier cet élément dans cette zone.");
            return;
        }
        if (zoneService.isDenied(clicked.getLocation(), ZoneFlag.INTERACT)) {
            event.setCancelled(true);
            deny(player, "Tu ne peux pas interagir avec cette entité dans cette zone.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        Player player = event.getPlayer();
        if (canBypassBuildProtection(player)) {
            return;
        }
        if (isDenied(event.getRightClicked().getLocation(), ZoneFlag.BLOCK_BREAK, ZoneFlag.INTERACT)) {
            event.setCancelled(true);
            deny(player, "Tu ne peux pas modifier cette armor stand dans cette zone.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> zoneService.isDenied(block.getLocation(), ZoneFlag.EXPLOSIONS));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> zoneService.isDenied(block.getLocation(), ZoneFlag.EXPLOSIONS));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        event.setCancelled(zoneService.isDenied(event.getBlock().getLocation(), ZoneFlag.FIRE_SPREAD));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        event.setCancelled(zoneService.isDenied(event.getBlock().getLocation(), ZoneFlag.FIRE_SPREAD));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Entity entity = event.getEntity();
        if (EntityTypes.isMonster(entity)) {
            event.setCancelled(zoneService.isDenied(event.getLocation(), ZoneFlag.MONSTER_SPAWN));
        } else if (EntityTypes.isAnimal(entity)) {
            event.setCancelled(zoneService.isDenied(event.getLocation(), ZoneFlag.ANIMAL_SPAWN));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (zoneService.isDenied(player.getLocation(), ZoneFlag.HUNGER)) {
                event.setCancelled(true);
                deny(player, "La perte de faim est désactivée dans cette zone.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof ItemFrame || event.getEntity() instanceof Painting || event.getEntity() instanceof ArmorStand) {
            handleProtectedEntityDamage(event);
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (zoneService.isDenied(player.getLocation(), ZoneFlag.DAMAGE)) {
            event.setCancelled(true);
            deny(player, "Tous les dégâts sont désactivés dans cette zone.");
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && zoneService.isDenied(player.getLocation(), ZoneFlag.FALL_DAMAGE)) {
            event.setCancelled(true);
            deny(player, "Les dégâts de chute sont désactivés dans cette zone.");
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ItemFrame || event.getEntity() instanceof Painting || event.getEntity() instanceof ArmorStand) {
            handleProtectedEntityDamage(event);
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Entity damager = event.getDamager();
        Optional<Player> attackingPlayer = attackingPlayer(damager);
        if (attackingPlayer.isPresent() && zoneService.isDenied(victim.getLocation(), ZoneFlag.PVP)) {
            event.setCancelled(true);
            deny(victim, "Le PvP est désactivé dans cette zone.");
            deny(attackingPlayer.orElseThrow(), "Tu ne peux pas attaquer un joueur dans cette zone.");
        } else if (attackingPlayer.isEmpty() && zoneService.isDenied(victim.getLocation(), ZoneFlag.PVE)) {
            event.setCancelled(true);
            deny(victim, "Les dégâts des entités sont désactivés dans cette zone.");
        }
    }

    private void handleBucket(PlayerBucketEvent event, ZoneFlag flag, String message) {
        Player player = event.getPlayer();
        if (canBypassBuildProtection(player)) {
            return;
        }
        if (zoneService.isDenied(event.getBlock().getLocation(), flag) || zoneService.isDenied(event.getBlockClicked().getLocation(), flag)) {
            event.setCancelled(true);
            deny(player, message);
        }
    }

    private void handleProtectedEntityDamage(EntityDamageEvent event) {
        if (!(event instanceof EntityDamageByEntityEvent damageByEntityEvent)) {
            if (zoneService.isDenied(event.getEntity().getLocation(), ZoneFlag.BLOCK_BREAK)) {
                event.setCancelled(true);
            }
            return;
        }
        Optional<Player> player = attackingPlayer(damageByEntityEvent.getDamager());
        if (player.isEmpty() || canBypassBuildProtection(player.orElseThrow())) {
            return;
        }
        if (zoneService.isDenied(event.getEntity().getLocation(), ZoneFlag.BLOCK_BREAK)) {
            event.setCancelled(true);
            deny(player.orElseThrow(), "Tu ne peux pas casser cet élément dans cette zone.");
        }
    }

    private boolean isDenied(Location location, ZoneFlag... flags) {
        for (ZoneFlag flag : flags) {
            if (zoneService.isDenied(location, flag)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCauldron(Block block) {
        return block.getType().name().endsWith("CAULDRON");
    }

    private boolean isProtectedInteractionBlock(Block block) {
        Material type = block.getType();
        return !isSign(block) && (type.isInteractable() || block.getBlockData() instanceof Openable || block.getState() instanceof Container);
    }

    private boolean isSign(Block block) {
        return block.getState() instanceof Sign;
    }

    private boolean isProtectedFrameLikeEntity(Entity entity) {
        return entity instanceof ItemFrame || entity instanceof Painting || entity instanceof ArmorStand;
    }

    private Optional<Player> attackingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return Optional.of(player);
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            return shooter instanceof Player player ? Optional.of(player) : Optional.empty();
        }
        if (damager instanceof AreaEffectCloud cloud) {
            ProjectileSource source = cloud.getSource();
            return source instanceof Player player ? Optional.of(player) : Optional.empty();
        }
        return Optional.empty();
    }

    private boolean canBypassBuildProtection(Player player) {
        return player.getGameMode() == GameMode.CREATIVE && player.hasPermission("zoneguard.admin");
    }

    private void deny(Player player, String message) {
        long now = System.currentTimeMillis();
        Long lastMessage = lastMessageByPlayer.get(player.getUniqueId());
        if (lastMessage != null && now - lastMessage < MESSAGE_COOLDOWN_MILLIS) {
            return;
        }
        lastMessageByPlayer.put(player.getUniqueId(), now);
        player.sendMessage(PREFIX + ERROR + message);
    }
}
