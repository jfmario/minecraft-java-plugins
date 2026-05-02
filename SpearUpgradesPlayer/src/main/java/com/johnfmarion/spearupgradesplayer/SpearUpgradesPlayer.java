package com.johnfmarion.spearupgradesplayer;

import org.bukkit.plugin.java.JavaPlugin;

public class SpearUpgradesPlayer extends JavaPlugin {

    private SpearManager spearManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        spearManager = new SpearManager(this);
        spearManager.loadFromConfig();
        getServer().getPluginManager().registerEvents(new SpearListener(this, spearManager), this);
        getLogger().info("SpearUpgradesPlayer enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SpearUpgradesPlayer disabled!");
    }

    public SpearManager getSpearManager() {
        return spearManager;
    }
}
