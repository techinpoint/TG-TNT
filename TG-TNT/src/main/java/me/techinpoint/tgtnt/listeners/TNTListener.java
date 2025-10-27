package me.techinpoint.tgtnt.listeners;

import me.techinpoint.tgtnt.TGMain;
import me.techinpoint.tgtnt.utils.TNTUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Event listener for TNT-related events
 * Handles TNT placement, explosion customization, and damage control
 *
 * @author Techinpoint Gamerz (TG)
 */
public class TNTListener implements Listener {

    private final TGMain plugin;
    private final TNTUtils tntUtils;

    public TNTListener(TGMain plugin) {
        this.plugin = plugin;
        this.tntUtils = new TNTUtils(plugin);
    }

    /**
     * Handle TNT placement to apply custom properties
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onTNTPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() != Material.TNT) {
            return;
        }

        String tntType = tntUtils.getTNTType(item);
        if (tntType == null) {
            return;
        }

        ConfigurationSection config = tntUtils.getTypeConfig(tntType);
        if (config == null) {
            return;
        }

        Block block = event.getBlockPlaced();
        block.setMetadata("tgtnt_type", new FixedMetadataValue(plugin, tntType));
    }

    /**
     * Handle obsidian breaking (runs at LOW priority - before main explosion)
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onObsidianBreak(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() != EntityType.PRIMED_TNT) {
            return;
        }

        TNTPrimed tnt = (TNTPrimed) entity;
        if (!tnt.hasMetadata("tgtnt_type")) {
            return;
        }

        String tntType = tnt.getMetadata("tgtnt_type").get(0).asString();
        ConfigurationSection config = tntUtils.getTypeConfig(tntType);
        if (config == null) {
            return;
        }

        if (!config.getBoolean("break_obsidian", false)) {
            return;
        }

        int radius = (int) (entity instanceof Explosive ? ((Explosive) entity).getYield() : 4);
        Block block = event.getLocation().getBlock();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block relativeBlock = block.getRelative(x, y, z);
                    if (isObsidian(relativeBlock.getType()) && !event.blockList().contains(relativeBlock)) {
                        event.blockList().add(relativeBlock);
                    }
                }
            }
        }
    }

    /**
     * Handle TNT explosion to apply custom explosion properties (runs at HIGH priority)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onTNTExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() != EntityType.PRIMED_TNT) {
            return;
        }

        TNTPrimed tnt = (TNTPrimed) entity;
        if (!tnt.hasMetadata("tgtnt_type")) {
            return;
        }

        String tntType = tnt.getMetadata("tgtnt_type").get(0).asString();
        ConfigurationSection config = tntUtils.getTypeConfig(tntType);
        if (config == null) {
            return;
        }

        boolean damageBlocks = config.getBoolean("damage_blocks", true);
        boolean createFire = config.getBoolean("create_fire", false);
        boolean breakObsidian = config.getBoolean("break_obsidian", false);
        boolean ignoreWater = config.getBoolean("ignore_water", false);
        boolean removeWater = config.getBoolean("remove_water", false);

        Location loc = tnt.getLocation();
        Block locBlock = loc.getBlock();

        if (!ignoreWater && isUnderwater(loc, tntType)) {
            event.setCancelled(true);
            loc.getWorld().spawnParticle(Particle.SPLASH, loc, 10, 0.5, 0.5, 0.5, 0.1);
            return;
        }

        if (removeWater && damageBlocks) {
            double radius = config.getDouble("power", 4.0);
            for (int x = -(int)radius; x <= radius; x++) {
                for (int y = -(int)radius; y <= radius; y++) {
                    for (int z = -(int)radius; z <= radius; z++) {
                        Block block = locBlock.getRelative(x, y, z);
                        if (isBlockLiquidlike(block) && block.getType() == Material.WATER) {
                            block.setType(Material.AIR);
                        }
                    }
                }
            }
        }

        // Handle block damage
        if (!damageBlocks) {
            event.blockList().clear();
        } else if (!breakObsidian) {
            // Remove obsidian blocks from the explosion list if we DON'T want to break them
            Iterator<Block> iterator = event.blockList().iterator();
            while (iterator.hasNext()) {
                Block block = iterator.next();
                if (isObsidian(block.getType())) {
                    iterator.remove();
                }
            }
        }
        // If breakObsidian is true, all blocks (including obsidian) stay in the list and get destroyed

        // Handle fire creation
        if (createFire && damageBlocks) {
            Location center = tnt.getLocation();
            double radius = config.getDouble("power", 4.0);
            double fireSpread = config.getDouble("fire_spread", 0.7);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (int x = -(int)radius; x <= radius; x++) {
                    for (int y = -(int)radius; y <= radius; y++) {
                        for (int z = -(int)radius; z <= radius; z++) {
                            Location fireLoc = center.clone().add(x, y, z);
                            Block fireBlock = fireLoc.getBlock();
                            Block belowBlock = fireLoc.clone().add(0, -1, 0).getBlock();
                            if (fireBlock.getType() == Material.AIR &&
                                    belowBlock.getType().isSolid() &&
                                    Math.random() < fireSpread &&
                                    fireLoc.distance(center) <= radius) {
                                fireBlock.setType(Material.FIRE);
                            }
                        }
                    }
                }
            }, 1L);
        }

        if (plugin.getConfig().getBoolean("general.chain_reaction", true)) {
            triggerChainReaction(tnt.getLocation(), tntType, config.getDouble("power", 4.0));
        }

        applyKnockback(tnt.getLocation(), config.getDouble("knockback", 1.0));
    }

    /**
     * Handle player damage from custom TNT
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager().getType() != EntityType.PRIMED_TNT) {
            return;
        }

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        TNTPrimed tnt = (TNTPrimed) event.getDamager();
        if (!tnt.hasMetadata("tgtnt_type")) {
            return;
        }

        String tntType = tnt.getMetadata("tgtnt_type").get(0).asString();
        ConfigurationSection config = tntUtils.getTypeConfig(tntType);
        if (config == null) {
            return;
        }

        boolean damagePlayers = config.getBoolean("damage_players", true);
        if (!damagePlayers) {
            event.setCancelled(true);
        }
    }

    /**
     * Handle TNT ignition by redstone or fire
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onTNTIgnite(ExplosionPrimeEvent event) {
        if (event.getEntity().getType() != EntityType.PRIMED_TNT) {
            return;
        }

        TNTPrimed tnt = (TNTPrimed) event.getEntity();
        Location loc = tnt.getLocation();
        Block block = loc.getBlock();
        String tntType = null;

        if (block.hasMetadata("tgtnt_type")) {
            tntType = block.getMetadata("tgtnt_type").get(0).asString();
            block.removeMetadata("tgtnt_type", plugin);
        } else {
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block nearbyBlock = loc.clone().add(x, y, z).getBlock();
                        if (nearbyBlock.hasMetadata("tgtnt_type")) {
                            tntType = nearbyBlock.getMetadata("tgtnt_type").get(0).asString();
                            nearbyBlock.removeMetadata("tgtnt_type", plugin);
                            break;
                        }
                    }
                    if (tntType != null) break;
                }
                if (tntType != null) break;
            }
        }

        if (tntType != null) {
            ConfigurationSection config = tntUtils.getTypeConfig(tntType);
            if (config != null) {
                float power = (float) config.getDouble("power", 4.0);
                event.setRadius(power);
                tnt.setMetadata("tgtnt_type", new FixedMetadataValue(plugin, tntType));
            }
        }
    }

    /**
     * Handle TNT ignition to apply custom fuse time
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onTNTPrime(EntitySpawnEvent event) {
        if (event.getEntity().getType() != EntityType.PRIMED_TNT) {
            return;
        }

        TNTPrimed tnt = (TNTPrimed) event.getEntity();
        Location loc = event.getLocation();
        Block block = loc.getBlock();
        String tntType = null;

        if (block.hasMetadata("tgtnt_type")) {
            tntType = block.getMetadata("tgtnt_type").get(0).asString();
            block.removeMetadata("tgtnt_type", plugin);
        } else {
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block nearbyBlock = loc.clone().add(x, y, z).getBlock();
                        if (nearbyBlock.hasMetadata("tgtnt_type")) {
                            tntType = nearbyBlock.getMetadata("tgtnt_type").get(0).asString();
                            nearbyBlock.removeMetadata("tgtnt_type", plugin);
                            break;
                        }
                    }
                    if (tntType != null) break;
                }
                if (tntType != null) break;
            }
        }

        if (tntType == null) {
            return;
        }

        ConfigurationSection config = tntUtils.getTypeConfig(tntType);
        if (config == null) {
            return;
        }

        int fuseTicks = config.getInt("fuse", 80);
        float power = (float) config.getDouble("power", 4.0);
        tnt.setFuseTicks(fuseTicks);
        tnt.setYield(power);
        tnt.setMetadata("tgtnt_type", new FixedMetadataValue(plugin, tntType));
    }

    /**
     * Apply custom knockback to nearby entities
     */
    private void applyKnockback(Location center, double multiplier) {
        if (multiplier <= 0) {
            return;
        }

        double radius = 10.0;
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof Player || entity.getType() == EntityType.DROPPED_ITEM) {
                Vector direction = entity.getLocation().toVector().subtract(center.toVector()).normalize();
                double distance = entity.getLocation().distance(center);
                double force = (1 - (distance / radius)) * multiplier * 2;
                if (force > 0) {
                    entity.setVelocity(entity.getVelocity().add(direction.multiply(force)));
                }
            }
        }
    }

    /**
     * Trigger chain reaction for nearby TNT
     */
    private void triggerChainReaction(Location center, String tntType, double power) {
        double radius = power * 1.5;
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity.getType() == EntityType.PRIMED_TNT) {
                TNTPrimed nearbyTnt = (TNTPrimed) entity;
                if (!nearbyTnt.hasMetadata("tgtnt_type")) {
                    nearbyTnt.setMetadata("tgtnt_type", new FixedMetadataValue(plugin, tntType));
                }
                if (nearbyTnt.getFuseTicks() > 10) {
                    nearbyTnt.setFuseTicks(10);
                }
            }
        }

        for (int x = -((int) radius); x <= radius; x++) {
            for (int y = -((int) radius); y <= radius; y++) {
                for (int z = -((int) radius); z <= radius; z++) {
                    Location loc = center.clone().add(x, y, z);
                    Block block = loc.getBlock();
                    if (block.getType() == Material.TNT) {
                        block.setType(Material.AIR);
                        TNTPrimed tnt = (TNTPrimed) center.getWorld().spawnEntity(loc, EntityType.PRIMED_TNT);
                        tnt.setFuseTicks(10);
                        tnt.setMetadata("tgtnt_type", new FixedMetadataValue(plugin, tntType));
                    }
                }
            }
        }
    }

    /**
     * Check if material is obsidian (including crying obsidian)
     */
    private boolean isObsidian(Material material) {
        return material == Material.OBSIDIAN ||
                (Material.getMaterial("CRYING_OBSIDIAN") != null &&
                        material == Material.getMaterial("CRYING_OBSIDIAN"));
    }

    private static boolean isBlockLiquidlike(Block block) {
        return block.isLiquid();
    }

    private static boolean isLocationSurroundedByLiquid(Location loc) {
        Block block = loc.getBlock();
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP};
        for (BlockFace face : faces) {
            if (isBlockLiquidlike(block.getRelative(face))) {
                return true;
            }
        }
        return false;
    }

    private boolean isUnderwater(Location loc, String tntType) {
        return isBlockLiquidlike(loc.getBlock()) || isLocationSurroundedByLiquid(loc);
    }
}