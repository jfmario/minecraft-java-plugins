package com.johnfmarion.spearupgrades;

import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpearListener implements Listener {

    private final SpearUpgradesCow plugin;
    private final SpearManager spearManager;

    /** Spear level to give on respawn, keyed by player UUID. */
    private final Map<UUID, Integer> respawnLevel = new HashMap<>();

    public SpearListener(SpearUpgradesCow plugin, SpearManager spearManager) {
        this.plugin = plugin;
        this.spearManager = spearManager;
    }

    // -------------------------------------------------------------------------
    // Join: give a level-1 spear to players who don't already have one
    // -------------------------------------------------------------------------
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (findSpear(player) == null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> giveSpear(player, 1), 1L);
        }
    }

    // -------------------------------------------------------------------------
    // Player kill with spear -> upgrade one tier.
    // -------------------------------------------------------------------------
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (!spearManager.isSpear(weapon)) return;

        int currentLevel = spearManager.getSpearLevel(weapon);
        if (currentLevel >= spearManager.getMaxLevel()) {
            killer.sendMessage("§6Your spear is already at maximum power!");
            return;
        }

        int newLevel = currentLevel + 1;
        killer.getInventory().setItemInMainHand(spearManager.createSpear(newLevel));
        killer.sendMessage("§aSpear upgraded → " + spearManager.getDisplayName(newLevel) + "§a!");
    }

    // -------------------------------------------------------------------------
    // Bonus damage: simulate Sharpness X on the Netherite Spear.
    // Vanilla Lunge is applied directly as an enchantment when spears are created.
    // -------------------------------------------------------------------------
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!spearManager.isSpear(weapon)) return;

        int level = spearManager.getSpearLevel(weapon);

        // Extra damage for Netherite Spear (Sharpness X simulation)
        double bonus = spearManager.getBonusDamage(level);
        if (bonus > 0) {
            event.setDamage(event.getDamage() + bonus);
        }
    }

    // -------------------------------------------------------------------------
    // Death: record respawn level (current - 1), strip spear from drops
    // -------------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Determine what level they had
        ItemStack spear = findSpear(player);
        int level = (spear != null) ? spearManager.getSpearLevel(spear) : 1;

        // Downgrade by one, floor at 1
        respawnLevel.put(player.getUniqueId(), Math.max(spearManager.getMinLevel(), level - 1));

        // Remove all custom spears from the drop list so no one can pick them up
        event.getDrops().removeIf(item -> spearManager.isSpear(item));
    }

    // -------------------------------------------------------------------------
    // Respawn: give a downgraded spear
    // -------------------------------------------------------------------------
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Integer stored = respawnLevel.remove(player.getUniqueId());
        int level = (stored != null) ? stored : 1;

        // Run one tick later so the inventory is fully cleared before we add the spear
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> giveSpear(player, level), 1L);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void giveSpear(Player player, int level) {
        removeAllSpears(player);
        ItemStack spear = spearManager.createSpear(level);

        PlayerInventory inv = player.getInventory();
        // Prefer main hand if empty; otherwise first free slot; otherwise slot 0
        if (inv.getItemInMainHand().getType() == Material.AIR) {
            inv.setItemInMainHand(spear);
        } else {
            int free = inv.firstEmpty();
            if (free >= 0) {
                inv.setItem(free, spear);
            } else {
                inv.setItemInMainHand(spear); // overwrite if inventory is completely full
            }
        }
    }

    private ItemStack findSpear(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (spearManager.isSpear(item)) return item;
        }
        return null;
    }

    private void removeAllSpears(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (spearManager.isSpear(contents[i])) contents[i] = null;
        }
        player.getInventory().setContents(contents);
    }
}
