package me.techinpoint.tgtnt.utils;

import me.techinpoint.tgtnt.TGMain;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility class for TNT-related operations
 * Handles TNT item creation, data management, and type validation
 *
 * @author Techinpoint Gamerz (TG)
 */
public class TNTUtils {

    private final TGMain plugin;
    private final NamespacedKey tntTypeKey;

    public TNTUtils(TGMain plugin) {
        this.plugin = plugin;
        this.tntTypeKey = new NamespacedKey(plugin, "tnt_type");
    }

    /**
     * Create a custom TNT item with specified type
     * @param type TNT type from config
     * @param amount Amount of TNT
     * @return Custom TNT ItemStack
     */
    public ItemStack createTNT(String type, int amount) {
        ConfigurationSection typeConfig = plugin.getConfig().getConfigurationSection("types." + type);

        if (typeConfig == null) {
            return null;
        }

        ItemStack tnt = new ItemStack(Material.TNT, amount);
        ItemMeta meta = tnt.getItemMeta();

        if (meta == null) {
            return tnt;
        }

        String name = typeConfig.getString("name", "&fTNT").replace("&", "§");
        String description = typeConfig.getString("description", "").replace("&", "§");

        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        lore.add(description);
        lore.add("");
        lore.add("§7Power: §e" + typeConfig.getDouble("power"));
        lore.add("§7Fuse: §e" + typeConfig.getInt("fuse") + " ticks");
        lore.add("");
        lore.add("§6TG-TNT §7by §eTechinpoint Gamerz");
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(tntTypeKey, PersistentDataType.STRING, type);

        tnt.setItemMeta(meta);
        return tnt;
    }

    /**
     * Get TNT type from ItemStack
     * @param item ItemStack to check
     * @return TNT type or null if not custom TNT
     */
    public String getTNTType(ItemStack item) {
        if (item == null || item.getType() != Material.TNT) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        return meta.getPersistentDataContainer().get(tntTypeKey, PersistentDataType.STRING);
    }

    /**
     * Check if TNT type exists in config
     * @param type TNT type to check
     * @return true if type exists
     */
    public boolean isValidType(String type) {
        return plugin.getConfig().contains("types." + type);
    }

    /**
     * Get all TNT type names
     * @return Set of type names
     */
    public Set<String> getAllTypes() {
        ConfigurationSection typesSection = plugin.getConfig().getConfigurationSection("types");
        if (typesSection == null) {
            return Set.of();
        }
        return typesSection.getKeys(false);
    }

    /**
     * Get configuration section for TNT type
     * @param type TNT type
     * @return ConfigurationSection or null
     */
    public ConfigurationSection getTypeConfig(String type) {
        return plugin.getConfig().getConfigurationSection("types." + type);
    }

    /**
     * Update TNT type configuration
     * @param type TNT type
     * @param key Configuration key
     * @param value Configuration value
     */
    public void updateTypeConfig(String type, String key, Object value) {
        plugin.getConfig().set("types." + type + "." + key, value);
        plugin.saveConfig();
    }

    /**
     * Get NamespacedKey for TNT type
     * @return NamespacedKey
     */
    public NamespacedKey getTNTTypeKey() {
        return tntTypeKey;
    }

    /**
     * Format color codes in string
     * @param text Text to format
     * @return Formatted text
     */
    public static String colorize(String text) {
        return text.replace("&", "§");
    }

    /**
     * Get display name for TNT type
     * @param type TNT type
     * @return Display name
     */
    public String getDisplayName(String type) {
        ConfigurationSection typeConfig = getTypeConfig(type);
        if (typeConfig == null) {
            return type;
        }
        return colorize(typeConfig.getString("name", type));
    }
}
