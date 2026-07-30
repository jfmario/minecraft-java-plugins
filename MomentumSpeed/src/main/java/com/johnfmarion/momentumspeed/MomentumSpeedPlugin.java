package com.johnfmarion.momentumspeed;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple Spigot plugin that increases player walk speed while they're moving and
 * resets it immediately when they stop moving.
 *
 * - Follows the existing plugin structure in this repo (Maven + shade).
 * - Tweak MOVE_THRESHOLD, ACCELERATION, MAX_MOMENTUM to change behaviour.
 */
public class MomentumSpeedPlugin extends JavaPlugin implements Listener {
    // Tweak these to change how speed builds up and the maximum extra speed
    private static final double MOVE_THRESHOLD = 0.001;   // minimum horizontal movement per event to count as "moving"
    private static final double ACCELERATION = 0.2;       // momentum increase per block moved (per move event)
    private static final double MAX_MOMENTUM = 0.5;       // max multiplier (0.5 = +50% speed)

    // Per-player state
    private final Map<UUID, Double> momentum = new ConcurrentHashMap<>();
    private final Map<UUID, Float> baseWalkSpeed = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        // store base walk speed for any online players (reloads / restarts)
        for (Player p : getServer().getOnlinePlayers()) {
            baseWalkSpeed.put(p.getUniqueId(), p.getWalkSpeed());
        }
        getLogger().info("MomentumSpeed enabled");
    }

    @Override
    public void onDisable() {
        // restore walk speeds
        for (Player p : getServer().getOnlinePlayers()) {
            Float base = baseWalkSpeed.get(p.getUniqueId());
            if (base != null) p.setWalkSpeed(base);
        }
        momentum.clear();
        baseWalkSpeed.clear();
        getLogger().info("MomentumSpeed disabled");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        baseWalkSpeed.put(p.getUniqueId(), p.getWalkSpeed());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        // restore base on quit (defensive)
        Float base = baseWalkSpeed.remove(id);
        if (base != null) {
            try { event.getPlayer().setWalkSpeed(base); } catch (Exception ignored) {}
        }
        momentum.remove(id);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        Player player = event.getPlayer();

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        UUID id = player.getUniqueId();

        if (horizontalDist > MOVE_THRESHOLD) {
            double m = momentum.getOrDefault(id, 0.0);
            m = Math.min(MAX_MOMENTUM, m + horizontalDist * ACCELERATION);
            momentum.put(id, m);

            // ensure we have the base walk speed stored
            float base = baseWalkSpeed.computeIfAbsent(id, k -> player.getWalkSpeed());
            // Walk speed must stay within valid range; default is ~0.2f.
            float newSpeed = (float) Math.min(1.0d, base * (1.0d + m));
            try {
                player.setWalkSpeed(newSpeed);
            } catch (IllegalArgumentException ignored) {
                // clamp defensively if something unexpected happens
                player.setWalkSpeed(Math.max(-1.0f, Math.min(1.0f, newSpeed)));
            }
        } else {
            // stopped moving: remove momentum and reset speed
            momentum.remove(id);
            Float base = baseWalkSpeed.get(id);
            if (base != null) {
                try { player.setWalkSpeed(base); } catch (Exception ignored) {}
            }
        }
    }
}
