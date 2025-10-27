package me.techinpoint.tgtnt.commands;

import me.techinpoint.tgtnt.TGMain;
import me.techinpoint.tgtnt.gui.TNTEditor;
import me.techinpoint.tgtnt.gui.TNTMenu;
import me.techinpoint.tgtnt.utils.TNTUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Main command handler for TG-TNT
 * Handles all plugin commands: give, reload, menu, editor, info
 *
 * @author Techinpoint Gamerz (TG)
 */
public class TNTCommand implements CommandExecutor, TabCompleter {

    private final TGMain plugin;
    private final TNTUtils tntUtils;
    private final TNTMenu tntMenu;
    private final TNTEditor tntEditor;

    public TNTCommand(TGMain plugin) {
        this.plugin = plugin;
        this.tntUtils = new TNTUtils(plugin);
        this.tntMenu = new TNTMenu(plugin);
        this.tntEditor = new TNTEditor(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getMessage("invalid_usage"));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give":
                return handleGive(sender, args);
            case "reload":
                return handleReload(sender);
            case "menu":
                return handleMenu(sender);
            case "editor":
                return handleEditor(sender);
            case "info":
                return handleInfo(sender);
            default:
                sender.sendMessage(plugin.getMessage("invalid_usage"));
                return true;
        }
    }

    /**
     * Handle /tgtnt give command
     */
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("tgtnt.give")) {
            sender.sendMessage(plugin.getMessage("no_permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /tgtnt give <player> <type> [amount]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessage("player_not_found"));
            return true;
        }

        String type = args[2].toLowerCase();
        if (!tntUtils.isValidType(type)) {
            String types = String.join(", ", tntUtils.getAllTypes());
            sender.sendMessage(plugin.getMessage("invalid_tnt_type").replace("{types}", types));
            return true;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount < 1 || amount > 64) {
                    sender.sendMessage("§cAmount must be between 1 and 64!");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid amount!");
                return true;
            }
        }

        ItemStack tnt = tntUtils.createTNT(type, amount);
        if (tnt == null) {
            sender.sendMessage("§cFailed to create TNT item!");
            return true;
        }

        target.getInventory().addItem(tnt);

        String message = plugin.getMessageNoPrefix("tnt_given")
                .replace("{amount}", String.valueOf(amount))
                .replace("{type}", tntUtils.getDisplayName(type))
                .replace("{player}", target.getName());
        sender.sendMessage(plugin.getConfig().getString("messages.prefix", "§6[TG-TNT]§r ").replace("&", "§") + message);

        String receiveMessage = plugin.getMessageNoPrefix("tnt_received")
                .replace("{amount}", String.valueOf(amount))
                .replace("{type}", tntUtils.getDisplayName(type));
        target.sendMessage(plugin.getConfig().getString("messages.prefix", "§6[TG-TNT]§r ").replace("&", "§") + receiveMessage);

        return true;
    }

    /**
     * Handle /tgtnt reload command
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("tgtnt.reload")) {
            sender.sendMessage(plugin.getMessage("no_permission"));
            return true;
        }

        plugin.reloadConfiguration();
        sender.sendMessage(plugin.getMessage("reload_success"));
        return true;
    }

    /**
     * Handle /tgtnt menu command
     */
    private boolean handleMenu(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (!sender.hasPermission("tgtnt.menu")) {
            sender.sendMessage(plugin.getMessage("no_permission"));
            return true;
        }

        Player player = (Player) sender;
        tntMenu.openMenu(player);
        return true;
    }

    /**
     * Handle /tgtnt editor command
     */
    private boolean handleEditor(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (!sender.hasPermission("tgtnt.editor")) {
            sender.sendMessage(plugin.getMessage("no_permission"));
            return true;
        }

        Player player = (Player) sender;
        tntEditor.openEditor(player, null);
        return true;
    }

    /**
     * Handle /tgtnt info command
     */
    private boolean handleInfo(CommandSender sender) {
        if (!sender.hasPermission("tgtnt.info")) {
            sender.sendMessage(plugin.getMessage("no_permission"));
            return true;
        }

        sender.sendMessage("§6§l========================================");
        sender.sendMessage(plugin.getMessage("plugin_info"));
        sender.sendMessage("§7Description: §fAdvanced TNT customization");
        sender.sendMessage("§7Commands: §f/tgtnt <give|reload|menu|editor|info>");
        sender.sendMessage("§7TNT Types: §f" + String.join(", ", tntUtils.getAllTypes()));
        sender.sendMessage("§6§l========================================");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = List.of("give", "reload", "menu", "editor", "info");
            for (String sub : subCommands) {
                if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (String type : tntUtils.getAllTypes()) {
                if (type.toLowerCase().startsWith(args[2].toLowerCase())) {
                    completions.add(type);
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            completions.add("1");
            completions.add("16");
            completions.add("32");
            completions.add("64");
        }

        return completions;
    }
}
