package me.techinpoint.tgtnt.gui;

import me.techinpoint.tgtnt.TGMain;
import me.techinpoint.tgtnt.utils.TNTUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * TNT Selection Menu GUI
 * Displays all available TNT types for players to select
 *
 * @author Techinpoint Gamerz (TG)
 */
public class TNTMenu implements Listener {

    private final TGMain plugin;
    private final TNTUtils tntUtils;

    public TNTMenu(TGMain plugin) {
        this.plugin = plugin;
        this.tntUtils = new TNTUtils(plugin);
    }

    /**
     * Open the TNT selection menu for a player
     * @param player Player to open menu for
     */
    public void openMenu(Player player) {
        String title = plugin.getConfig().getString("gui.menu.title", "&6&lTG-TNT Menu").replace("&", "§");
        int size = plugin.getConfig().getInt("gui.menu.size", 27);

        Inventory menu = Bukkit.createInventory(null, size, title);

        Set<String> types = tntUtils.getAllTypes();
        int slot = 0;

        for (String type : types) {
            if (slot >= size) break;

            ItemStack tntItem = createMenuIcon(type);
            if (tntItem != null) {
                menu.setItem(slot, tntItem);
                slot++;
            }
        }

        for (int i = slot; i < size; i++) {
            ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = filler.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(" ");
                filler.setItemMeta(meta);
            }
            menu.setItem(i, filler);
        }

        player.openInventory(menu);
    }

    /**
     * Create a menu icon for a TNT type
     * @param type TNT type
     * @return ItemStack icon
     */
    private ItemStack createMenuIcon(String type) {
        ConfigurationSection config = tntUtils.getTypeConfig(type);
        if (config == null) {
            return null;
        }

        ItemStack icon = new ItemStack(Material.TNT);
        ItemMeta meta = icon.getItemMeta();

        if (meta == null) {
            return icon;
        }

        String name = config.getString("name", type).replace("&", "§");
        String description = config.getString("description", "").replace("&", "§");

        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        lore.add(description);
        lore.add("");
        lore.add("§7Properties:");
        lore.add("§8 • §7Power: §e" + config.getDouble("power"));
        lore.add("§8 • §7Fuse: §e" + config.getInt("fuse") + " ticks");
        lore.add("§8 • §7Knockback: §e" + config.getDouble("knockback") + "x");
        lore.add("");

        List<String> features = new ArrayList<>();
        if (config.getBoolean("create_fire")) features.add("§c⚡ Creates Fire");
        if (config.getBoolean("break_obsidian")) features.add("§5⛏ Breaks Obsidian");
        if (!config.getBoolean("damage_players")) features.add("§a✓ Safe for Players");
        if (config.getBoolean("ignore_water")) features.add("§b🌊 Ignores Water");
        if (config.getInt("water_radius") > 0) features.add("§b💧 Water Radius: " + config.getInt("water_radius"));

        if (!features.isEmpty()) {
            lore.add("§7Features:");
            for (String feature : features) {
                lore.add("§8 • " + feature);
            }
            lore.add("");
        }

        lore.add("§eClick to receive this TNT!");
        lore.add("§6TG-TNT §7by §eTechinpoint Gamerz");

        meta.setLore(lore);
        icon.setItemMeta(meta);

        return icon;
    }

    /**
     * Handle inventory click events in the TNT menu
     */
    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        String title = plugin.getConfig().getString("gui.menu.title", "&6&lTG-TNT Menu").replace("&", "§");

        if (!event.getView().getTitle().equals(title)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() != Material.TNT) {
            return;
        }

        Set<String> types = tntUtils.getAllTypes();
        for (String type : types) {
            ConfigurationSection config = tntUtils.getTypeConfig(type);
            if (config == null) continue;

            String name = config.getString("name", type).replace("&", "§");
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) continue;

            if (meta.getDisplayName().equals(name)) {
                ItemStack tnt = tntUtils.createTNT(type, 1);
                if (tnt != null) {
                    player.getInventory().addItem(tnt);
                    player.sendMessage(plugin.getConfig().getString("messages.prefix", "§6[TG-TNT]§r ").replace("&", "§") +
                                     "§aYou received 1x " + name + "§a!");
                    player.closeInventory();
                }
                return;
            }
        }
    }
}
