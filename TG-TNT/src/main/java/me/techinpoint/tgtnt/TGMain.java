package me.techinpoint.tgtnt;

import me.techinpoint.tgtnt.commands.TNTCommand;
import me.techinpoint.tgtnt.listeners.TNTListener;
import me.techinpoint.tgtnt.gui.TNTEditor;
import me.techinpoint.tgtnt.gui.TNTMenu;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * TG-TNT Main Plugin Class
 * Advanced TNT customization plugin by Techinpoint Gamerz
 *
 * @author Techinpoint Gamerz (TG)
 * @version 1.0.0
 */
public class TGMain extends JavaPlugin {

    private static TGMain instance;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        registerCommands();
        registerListeners();

        getLogger().info("§6========================================");
        getLogger().info("§6TG-TNT loaded successfully!");
        getLogger().info("§6Version: 1.0.0");
        getLogger().info("§6Author: Techinpoint Gamerz (TG)");
        getLogger().info("§6========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("§6TG-TNT disabled. Thanks for using!");
    }

    /**
     * Register plugin commands
     */
    private void registerCommands() {
        TNTCommand tntCommand = new TNTCommand(this);
        getCommand("tgtnt").setExecutor(tntCommand);
        getCommand("tgtnt").setTabCompleter(tntCommand);
    }

    /**
     * Register event listeners
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new TNTListener(this), this);
        getServer().getPluginManager().registerEvents(new TNTMenu(this), this);
        getServer().getPluginManager().registerEvents(new TNTEditor(this), this);
    }

    /**
     * Reload plugin configuration
     */
    public void reloadConfiguration() {
        reloadConfig();
    }

    /**
     * Get plugin instance
     * @return Plugin instance
     */
    public static TGMain getInstance() {
        return instance;
    }

    /**
     * Get formatted message from config
     * @param key Message key
     * @return Formatted message
     */
    public String getMessage(String key) {
        String prefix = getConfig().getString("messages.prefix", "§6[TG-TNT]§r ");
        String message = getConfig().getString("messages." + key, "Message not found: " + key);
        return (prefix + message).replace("&", "§");
    }

    /**
     * Get formatted message without prefix
     * @param key Message key
     * @return Formatted message
     */
    public String getMessageNoPrefix(String key) {
        String message = getConfig().getString("messages." + key, "Message not found: " + key);
        return message.replace("&", "§");
    }
}
