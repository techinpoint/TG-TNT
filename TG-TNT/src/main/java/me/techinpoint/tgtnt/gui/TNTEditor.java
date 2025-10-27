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

import java.util.*;

/**
 * TNT Editor GUI
 * Allows admins to edit TNT properties in real-time
 *
 * @author Techinpoint Gamerz (TG)
 */
public class TNTEditor implements Listener {

    private final TGMain plugin;
    private final TNTUtils tntUtils;
    private final Map<UUID, String> editingType = new HashMap<>();

    public TNTEditor(TGMain plugin) {
        this.plugin = plugin;
        this.tntUtils = new TNTUtils(plugin);
    }

    /**
     * Open the editor menu for a player
     * @param player Player to open editor for
     * @param type TNT type to edit (null for type selection)
     */
    public void openEditor(Player player, String type) {
        if (type == null) {
            openTypeSelector(player);
        } else {
            openPropertyEditor(player, type);
        }
    }

    /**
     * Open type selection menu
     */
    private void openTypeSelector(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, "§c§lSelect TNT Type to Edit");

        Set<String> types = tntUtils.getAllTypes();
        int slot = 0;

        for (String type : types) {
            if (slot >= 27) break;

            ItemStack icon = createTypeIcon(type);
            if (icon != null) {
                menu.setItem(slot, icon);
                slot++;
            }
        }

        player.openInventory(menu);
    }

    /**
     * Open property editor for specific TNT type
     */
    private void openPropertyEditor(Player player, String type) {
        editingType.put(player.getUniqueId(), type);

        String title = plugin.getConfig().getString("gui.editor.title", "&c&lTNT Editor").replace("&", "§");
        int size = plugin.getConfig().getInt("gui.editor.size", 54);

        Inventory editor = Bukkit.createInventory(null, size, title + " - " + type);

        ConfigurationSection config = tntUtils.getTypeConfig(type);
        if (config == null) {
            player.sendMessage("§cError: TNT type not found!");
            return;
        }

        editor.setItem(10, createPropertyIcon(Material.BLAZE_POWDER, "§ePower",
                Arrays.asList("§7Current: §e" + config.getDouble("power"),
                        "", "§eLeft Click: §a+0.5", "§eRight Click: §c-0.5",
                        "§eShift+Left: §a+2.0", "§eShift+Right: §c-2.0")));

        editor.setItem(11, createPropertyIcon(Material.CLOCK, "§eFuse Time",
                Arrays.asList("§7Current: §e" + config.getInt("fuse") + " ticks",
                        "", "§eLeft Click: §a+10", "§eRight Click: §c-10",
                        "§eShift+Left: §a+40", "§eShift+Right: §c-40")));

        editor.setItem(12, createPropertyIcon(Material.FEATHER, "§eKnockback",
                Arrays.asList("§7Current: §e" + config.getDouble("knockback") + "x",
                        "", "§eLeft Click: §a+0.1", "§eRight Click: §c-0.1",
                        "§eShift+Left: §a+0.5", "§eShift+Right: §c-0.5")));

        editor.setItem(13, createPropertyIcon(Material.FIRE_CHARGE, "§eFire Spread",
                Arrays.asList("§7Current: §e" + String.format("%.2f", config.getDouble("fire_spread")),
                        "§7Density of fire blocks created",
                        "", "§eLeft Click: §a+0.05", "§eRight Click: §c-0.05",
                        "§eShift+Left: §a+0.2", "§eShift+Right: §c-0.2")));

        editor.setItem(14, createToggleIcon(Material.FLINT_AND_STEEL, "§eCreate Fire",
                config.getBoolean("create_fire")));

        editor.setItem(15, createToggleIcon(Material.PLAYER_HEAD, "§eDamage Players",
                config.getBoolean("damage_players")));

        editor.setItem(16, createToggleIcon(Material.GRASS_BLOCK, "§eDamage Blocks",
                config.getBoolean("damage_blocks")));

        editor.setItem(19, createToggleIcon(Material.OBSIDIAN, "§eBreak Obsidian",
                config.getBoolean("break_obsidian")));

        editor.setItem(20, createToggleIcon(Material.WATER_BUCKET, "§eIgnore Water",
                config.getBoolean("ignore_water")));

        editor.setItem(21, createPropertyIcon(Material.BUCKET, "§eWater Radius",
                Arrays.asList("§7Current: §e" + config.getInt("water_radius") + " blocks",
                        "§70 = disabled",
                        "", "§eLeft Click: §a+1", "§eRight Click: §c-1",
                        "§eShift+Left: §a+5", "§eShift+Right: §c-5")));

        editor.setItem(48, createActionIcon(Material.EMERALD_BLOCK, "§a§lSave Changes",
                Collections.singletonList("§7Click to save and close")));

        editor.setItem(49, createActionIcon(Material.BARRIER, "§c§lCancel",
                Collections.singletonList("§7Click to close without saving")));

        editor.setItem(50, createActionIcon(Material.CHEST, "§e§lBack to Type Selection",
                Collections.singletonList("§7Return to TNT type selection")));

        for (int i = 0; i < size; i++) {
            if (editor.getItem(i) == null) {
                ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta meta = filler.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(" ");
                    filler.setItemMeta(meta);
                }
                editor.setItem(i, filler);
            }
        }

        player.openInventory(editor);
    }

    /**
     * Create icon for type selection
     */
    private ItemStack createTypeIcon(String type) {
        ConfigurationSection config = tntUtils.getTypeConfig(type);
        if (config == null) return null;

        ItemStack icon = new ItemStack(Material.TNT);
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) return icon;

        String name = config.getString("name", type).replace("&", "§");
        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        lore.add("§7Click to edit this TNT type");
        meta.setLore(lore);

        icon.setItemMeta(meta);
        return icon;
    }

    /**
     * Create property adjustment icon
     */
    private ItemStack createPropertyIcon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create toggle icon
     */
    private ItemStack createToggleIcon(Material material, String name, boolean enabled) {
        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(name);
        List<String> lore = Arrays.asList(
                "§7Status: " + (enabled ? "§aEnabled" : "§cDisabled"),
                "",
                "§eClick to toggle"
        );
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create action button icon
     */
    private ItemStack createActionIcon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Handle editor menu clicks
     */
    @EventHandler
    public void onEditorClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals("§c§lSelect TNT Type to Edit")) {
            event.setCancelled(true);
            handleTypeSelection(player, event);
            return;
        }

        if (!title.startsWith(plugin.getConfig().getString("gui.editor.title", "&c&lTNT Editor").replace("&", "§"))) {
            return;
        }

        event.setCancelled(true);
        handlePropertyEdit(player, event);
    }

    /**
     * Handle type selection clicks
     */
    private void handleTypeSelection(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.TNT) return;

        Set<String> types = tntUtils.getAllTypes();
        for (String type : types) {
            ConfigurationSection config = tntUtils.getTypeConfig(type);
            if (config == null) continue;

            String name = config.getString("name", type).replace("&", "§");
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) continue;

            if (meta.getDisplayName().equals(name)) {
                openPropertyEditor(player, type);
                return;
            }
        }
    }

    /**
     * Handle property editing clicks
     */
    private void handlePropertyEdit(Player player, InventoryClickEvent event) {
        String type = editingType.get(player.getUniqueId());
        if (type == null) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        int slot = event.getSlot();

        switch (slot) {
            case 10:
                adjustPower(type, event.isShiftClick() ? 2.0 : 0.5, event.isLeftClick());
                openPropertyEditor(player, type);
                break;
            case 11:
                adjustFuse(type, event.isShiftClick() ? 40 : 10, event.isLeftClick());
                openPropertyEditor(player, type);
                break;
            case 12:
                adjustKnockback(type, event.isShiftClick() ? 0.5 : 0.1, event.isLeftClick());
                openPropertyEditor(player, type);
                break;
            case 13:
                adjustFireSpread(type, event.isShiftClick() ? 0.2 : 0.05, event.isLeftClick());
                openPropertyEditor(player, type);
                break;
            case 14:
                toggleProperty(type, "create_fire");
                openPropertyEditor(player, type);
                break;
            case 15:
                toggleProperty(type, "damage_players");
                openPropertyEditor(player, type);
                break;
            case 16:
                toggleProperty(type, "damage_blocks");
                openPropertyEditor(player, type);
                break;
            case 19:
                toggleProperty(type, "break_obsidian");
                openPropertyEditor(player, type);
                break;
            case 20:
                toggleProperty(type, "ignore_water");
                openPropertyEditor(player, type);
                break;
            case 21:
                adjustWaterRadius(type, event.isShiftClick() ? 5 : 1, event.isLeftClick());
                openPropertyEditor(player, type);
                break;
            case 48:
                plugin.saveConfig();
                player.sendMessage(plugin.getMessage("editor_saved"));
                player.closeInventory();
                editingType.remove(player.getUniqueId());
                break;
            case 49:
                plugin.reloadConfiguration();
                player.closeInventory();
                editingType.remove(player.getUniqueId());
                break;
            case 50:
                openTypeSelector(player);
                editingType.remove(player.getUniqueId());
                break;
        }
    }

    private void adjustPower(String type, double amount, boolean increase) {
        ConfigurationSection config = tntUtils.getTypeConfig(type);
        if (config == null) return;

        double current = config.getDouble("power");
        double newValue = increase ? current + amount : Math.max(0.5, current - amount);
        tntUtils.updateTypeConfig(type, "power", newValue);
    }

    private void adjustFuse(String type, int amount, boolean increase) {
        ConfigurationSection config = tntUtils.getTypeConfig(type);
        if (config == null) return;

        int current = config.getInt("fuse");
        int newValue = increase ? current + amount : Math.max(10, current - amount);
        tntUtils.updateTypeConfig(type, "fuse", newValue);
    }

    private void adjustKnockback(String type, double amount, boolean increase) {
        ConfigurationSection config = tntUtils.getTypeConfig(type);
        if (config == null) return;

        double current = config.getDouble("knockback");
        double newValue = increase ? current + amount : Math.max(0.0, current - amount);
        tntUtils.updateTypeConfig(type, "knockback", newValue);
    }

    private void adjustFireSpread(String type, double amount, boolean increase) {
        ConfigurationSection config = tntUtils.getTypeConfig(type);
        if (config == null) return;

        double current = config.getDouble("fire_spread");
        double newValue = increase ? Math.min(1.0, current + amount) : Math.max(0.0, current - amount);
        tntUtils.updateTypeConfig(type, "fire_spread", newValue);
    }

    private void toggleProperty(String type, String property) {
        ConfigurationSection config = tntUtils.getTypeConfig(type);
        if (config == null) return;

        boolean current = config.getBoolean(property);
        tntUtils.updateTypeConfig(type, property, !current);
    }

    private void adjustWaterRadius(String type, int amount, boolean increase) {
        ConfigurationSection config = tntUtils.getTypeConfig(type);
        if (config == null) return;

        int current = config.getInt("water_radius");
        int newValue = increase ? current + amount : Math.max(0, current - amount);
        tntUtils.updateTypeConfig(type, "water_radius", newValue);
    }
}
