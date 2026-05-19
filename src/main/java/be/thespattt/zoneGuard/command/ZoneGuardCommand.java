package be.thespattt.zoneGuard.command;

import be.thespattt.zoneGuard.ZoneService;
import be.thespattt.zoneGuard.model.BlockPoint;
import be.thespattt.zoneGuard.model.FlagState;
import be.thespattt.zoneGuard.model.ProtectedRegion;
import be.thespattt.zoneGuard.model.ZoneFlag;
import be.thespattt.zoneGuard.selection.PlayerSelection;
import be.thespattt.zoneGuard.selection.SelectionManager;
import be.thespattt.zoneGuard.selection.SelectionMode;
import be.thespattt.zoneGuard.selection.SelectionVisualizer;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class ZoneGuardCommand implements CommandExecutor, TabCompleter, BasicCommand {
    private static final String PREFIX = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "ZoneGuard " + ChatColor.DARK_GRAY + ">> " + ChatColor.RESET;
    private static final String TITLE = ChatColor.AQUA + "" + ChatColor.BOLD;
    private static final String COMMAND = ChatColor.AQUA.toString();
    private static final String VALUE = ChatColor.YELLOW.toString();
    private static final String TEXT = ChatColor.GRAY.toString();
    private static final String SUCCESS = ChatColor.GREEN.toString();
    private static final String ERROR = ChatColor.RED.toString();

    private final ZoneService zoneService;
    private final SelectionManager selectionManager;
    private final SelectionVisualizer selectionVisualizer;

    public ZoneGuardCommand(ZoneService zoneService, SelectionManager selectionManager, SelectionVisualizer selectionVisualizer) {
        this.zoneService = zoneService;
        this.selectionManager = selectionManager;
        this.selectionVisualizer = selectionVisualizer;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        onCommand(source.getSender(), null, "zg", args);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        return onTabComplete(source.getSender(), null, "zg", args);
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return sender.hasPermission("zoneguard.admin");
    }

    @Override
    public String permission() {
        return "zoneguard.admin";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("zoneguard.admin")) {
            error(sender, "Tu n'as pas la permission d'utiliser cette commande.");
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "edit" -> handleEdit(sender, args);
            case "define" -> handleDefine(sender, args);
            case "delete", "remove" -> handleDelete(sender, args);
            case "flag" -> handleFlag(sender, args);
            case "flaglist", "flags" -> handleFlagList(sender);
            case "see" -> handleSee(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "save" -> {
                zoneService.saveAsync();
                success(sender, "Sauvegarde lancée en arrière-plan.");
            }
            case "reload" -> {
                zoneService.load();
                success(sender, "Zones rechargées depuis le stockage.");
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleEdit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            error(sender, "Seuls les joueurs peuvent créer une sélection.");
            return;
        }
        SelectionMode mode = args.length >= 2 && args[1].equalsIgnoreCase("any") ? SelectionMode.POLYGON : SelectionMode.CUBOID;
        selectionManager.selection(player.getUniqueId()).start(mode);
        selectionVisualizer.start(player);
        success(player, mode == SelectionMode.CUBOID
                ? "Sélection cuboïde activée. " + TEXT + "Hache en bois : clic gauche position 1, clic droit position 2."
                : "Sélection polygonale activée. " + TEXT + "Hache en bois : clic droit pour ajouter des points, clic gauche pour fermer.");
    }

    private void handleDefine(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            error(sender, "Seuls les joueurs peuvent définir une zone.");
            return;
        }
        if (args.length < 2) {
            usage(sender, "/zg define <nom>");
            return;
        }
        PlayerSelection selection = selectionManager.selection(player.getUniqueId());
        ProtectedRegion region;
        if (selection.mode() == SelectionMode.CUBOID) {
            if (selection.first().isEmpty() || selection.second().isEmpty()) {
                error(sender, "Sélectionne d'abord les deux positions du cuboïde.");
                return;
            }
            BlockPoint first = selection.first().orElseThrow();
            BlockPoint second = selection.second().orElseThrow();
            if (!first.world().equals(second.world())) {
                error(sender, "Les deux positions doivent être dans le même monde.");
                return;
            }
            World selectedWorld = org.bukkit.Bukkit.getWorld(first.world());
            if (selectedWorld == null) {
                error(sender, "Le monde sélectionné n'est pas chargé.");
                return;
            }
            BlockPoint fullHeightFirst = new BlockPoint(first.world(), first.x(), selectedWorld.getMinHeight(), first.z());
            BlockPoint fullHeightSecond = new BlockPoint(second.world(), second.x(), selectedWorld.getMaxHeight() - 1, second.z());
            int priority = zoneService.snapshot().nextPriority(first.world());
            region = ProtectedRegion.cuboid(args[1], first.world(), priority, fullHeightFirst, fullHeightSecond);
        } else {
            if (!selection.polygonClosed()) {
                error(sender, "Ferme d'abord le polygone avec un clic gauche.");
                return;
            }
            List<BlockPoint> points = selection.polygonBlockPoints();
            World selectedWorld = org.bukkit.Bukkit.getWorld(points.get(0).world());
            if (selectedWorld == null) {
                error(sender, "Le monde sélectionné n'est pas chargé.");
                return;
            }
            int priority = zoneService.snapshot().nextPriority(points.get(0).world());
            region = ProtectedRegion.polygon(args[1], points.get(0).world(), priority, selectedWorld.getMinHeight(), selectedWorld.getMaxHeight() - 1, selection.polygon2DPoints());
        }
        zoneService.defineRegion(region);
        selectionVisualizer.stop(player.getUniqueId());
        success(sender, "Zone " + VALUE + region.name() + SUCCESS + " créée dans le monde " + VALUE + region.world() + SUCCESS + ".");
    }

    private void handleFlag(CommandSender sender, String[] args) {
        if (args.length < 4) {
            usage(sender, "/zg flag <zone|global|global:monde> <flag> <allow|deny|unset>");
            return;
        }
        ZoneFlag flag = ZoneFlag.fromKey(args[2]).orElse(null);
        if (flag == null) {
            error(sender, "Flag inconnue. Utilise " + COMMAND + "/zg flaglist" + ERROR + " pour voir la liste.");
            return;
        }
        FlagState state;
        try {
            state = FlagState.fromInput(args[3]);
        } catch (IllegalArgumentException exception) {
            error(sender, "L'état doit être allow, deny ou unset.");
            return;
        }
        if (args[1].equalsIgnoreCase("global")) {
            if (!(sender instanceof Player player)) {
                usage(sender, "/zg flag global:<monde> <flag> <allow|deny|unset>");
                return;
            }
            zoneService.setGlobalFlag(player.getWorld(), flag, state);
            success(sender, "Flag globale " + VALUE + flag.key() + SUCCESS + " définie sur " + formatState(state) + SUCCESS + " pour " + VALUE + player.getWorld().getName() + SUCCESS + ".");
            return;
        }
        if (args[1].toLowerCase(Locale.ROOT).startsWith("global:")) {
            String worldName = args[1].substring("global:".length());
            World world = org.bukkit.Bukkit.getWorld(worldName);
            if (world == null) {
                error(sender, "Monde inconnu : '" + worldName + "'.");
                return;
            }
            zoneService.setGlobalFlag(world, flag, state);
            success(sender, "Flag globale " + VALUE + flag.key() + SUCCESS + " définie sur " + formatState(state) + SUCCESS + " pour " + VALUE + world.getName() + SUCCESS + ".");
            return;
        }
        if (!zoneService.setRegionFlag(args[1], flag, state)) {
            error(sender, "Zone inconnue : '" + args[1] + "'.");
            return;
        }
        success(sender, "Flag " + VALUE + flag.key() + SUCCESS + " définie sur " + formatState(state) + SUCCESS + " pour la zone " + VALUE + args[1] + SUCCESS + ".");
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender, "/zg delete <zone>");
            return;
        }
        if (!zoneService.deleteRegion(args[1])) {
            error(sender, "Zone inconnue : '" + args[1] + "'.");
            return;
        }
        success(sender, "Zone " + VALUE + args[1] + SUCCESS + " supprimée.");
    }

    private void handleSee(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            error(sender, "Seuls les joueurs peuvent afficher les particules d'une zone.");
            return;
        }
        if (args.length < 2) {
            usage(sender, "/zg see <zone>");
            return;
        }
        zoneService.snapshot().findRegion(args[1]).ifPresentOrElse(region -> {
            if (selectionVisualizer.showRegion(player, region)) {
                success(sender, "Affichage de la zone " + VALUE + region.name() + SUCCESS + " pendant quelques secondes.");
            } else {
                error(sender, "Va dans le monde '" + region.world() + "' pour voir cette zone.");
            }
        }, () -> error(sender, "Zone inconnue : '" + args[1] + "'."));
    }


    private void handleList(CommandSender sender) {
        if (zoneService.snapshot().regionsByWorld().isEmpty()) {
            info(sender, "Aucune zone n'est définie.");
            return;
        }
        header(sender, "Zones");
        zoneService.snapshot().regionsByWorld().forEach((world, regions) -> {
            sender.sendMessage(TEXT + "- " + VALUE + world + TEXT + " (" + regions.size() + "):");
            for (ProtectedRegion region : regions) {
                sender.sendMessage(TEXT + "  " + COMMAND + region.name() + TEXT + " [" + VALUE + formatType(region) + TEXT + ", priorité " + VALUE + region.priority() + TEXT + ", flags " + VALUE + region.flags().size() + TEXT + "]");
            }
        });
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender, "/zg info <zone>");
            return;
        }
        zoneService.snapshot().findRegion(args[1]).ifPresentOrElse(region -> {
            header(sender, "Zone " + region.name());
            sender.sendMessage(TEXT + "Type : " + VALUE + formatType(region) + TEXT + " | Monde : " + VALUE + region.world() + TEXT + " | Priorité : " + VALUE + region.priority());
            sender.sendMessage(TEXT + "Limites : " + VALUE + region.minX() + "," + region.minY() + "," + region.minZ() + TEXT + " -> " + VALUE + region.maxX() + "," + region.maxY() + "," + region.maxZ());
            sender.sendMessage(TEXT + "Flags : " + formatFlags(region));
        }, () -> error(sender, "Zone inconnue : '" + args[1] + "'."));
    }

    private void handleFlagList(CommandSender sender) {
        header(sender, "Flags");
        sender.sendMessage(TEXT + "Utilise " + COMMAND + "/zg flag <zone|global> <flag> <allow|deny|unset>");
        for (ZoneFlag flag : ZoneFlag.values()) {
            sender.sendMessage(COMMAND + "- " + flag.key() + TEXT + ": " + flag.description());
        }
    }

    private void sendHelp(CommandSender sender) {
        header(sender, "Commandes");
        helpLine(sender, "/zg edit [any]", "Démarre une sélection cuboïde ou polygonale.");
        helpLine(sender, "/zg define <nom>", "Crée une zone à partir de ta sélection.");
        helpLine(sender, "/zg list", "Affiche les zones existantes.");
        helpLine(sender, "/zg info <zone>", "Affiche les détails d'une zone.");
        helpLine(sender, "/zg see <zone>", "Affiche une zone avec des particules.");
        helpLine(sender, "/zg flag <zone|global> <flag> <allow|deny|unset>", "Modifie les règles de protection.");
        helpLine(sender, "/zg flaglist", "Affiche toutes les flags et leur effet.");
        helpLine(sender, "/zg delete <zone>", "Supprime une zone.");
        helpLine(sender, "/zg reload", "Recharge les zones sauvegardées.");
        helpLine(sender, "/zg save", "Lance une sauvegarde.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("zoneguard.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(args[0], List.of("edit", "define", "delete", "remove", "flag", "flaglist", "flags", "see", "list", "info", "reload", "save"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("edit")) {
            return filter(args[1], List.of("any"));
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("flag") || args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("see"))) {
            List<String> regionNames = zoneService.snapshot().regionsByWorld().values().stream()
                    .flatMap(List::stream)
                    .map(ProtectedRegion::name)
                    .toList();
            if (args[0].equalsIgnoreCase("flag")) {
                return filter(args[1], merge(List.of("global"), regionNames));
            }
            return filter(args[1], regionNames);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("flag")) {
            return filter(args[2], Arrays.stream(ZoneFlag.values()).map(ZoneFlag::key).toList());
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("flag")) {
            return filter(args[3], List.of("allow", "deny", "unset"));
        }
        return List.of();
    }

    private List<String> merge(List<String> first, List<String> second) {
        return java.util.stream.Stream.concat(first.stream(), second.stream()).toList();
    }

    private List<String> filter(String prefix, List<String> values) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)).toList();
    }

    private void header(CommandSender sender, String title) {
        sender.sendMessage(ChatColor.DARK_GRAY + "---------------- " + TITLE + title + ChatColor.DARK_GRAY + " ----------------");
    }

    private void helpLine(CommandSender sender, String command, String description) {
        sender.sendMessage(COMMAND + command + ChatColor.DARK_GRAY + " - " + TEXT + description);
    }

    private void usage(CommandSender sender, String usage) {
        sender.sendMessage(PREFIX + ERROR + "Utilisation : " + COMMAND + usage);
    }

    private void success(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + SUCCESS + message);
    }

    private void error(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + ERROR + message);
    }

    private void info(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + TEXT + message);
    }

    private String formatState(FlagState state) {
        return switch (state) {
            case ALLOW -> ChatColor.GREEN + "AUTORISÉ";
            case DENY -> ChatColor.RED + "REFUSÉ";
            case UNSET -> ChatColor.GRAY + "NON DÉFINI";
        };
    }

    private String formatType(ProtectedRegion region) {
        return switch (region.type()) {
            case CUBOID -> "cuboïde";
            case POLYGON -> "polygone";
        };
    }

    private String formatFlags(ProtectedRegion region) {
        if (region.flags().isEmpty()) {
            return VALUE + "aucune";
        }
        return region.flags().entrySet().stream()
                .map(entry -> COMMAND + entry.getKey().key() + TEXT + "=" + formatState(entry.getValue()))
                .collect(java.util.stream.Collectors.joining(TEXT + ", "));
    }
}
