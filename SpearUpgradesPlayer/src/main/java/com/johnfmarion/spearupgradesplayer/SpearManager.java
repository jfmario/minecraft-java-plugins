package com.johnfmarion.spearupgradesplayer;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpearManager {

    private final SpearUpgradesPlayer plugin;
    private final NamespacedKey spearLevelKey;

    private int minLevel = 1;
    private int maxLevel = 5;
    private final Map<Integer, SpearLevelDefinition> levelDefinitions = new HashMap<>();

    public SpearManager(SpearUpgradesPlayer plugin) {
        this.plugin = plugin;
        this.spearLevelKey = new NamespacedKey(plugin, "spear_level");
    }

    /**
     * Reads {@code config.yml} (paths {@code min-level}, {@code max-level}, {@code levels.<n>}).
     * Call after {@link org.bukkit.plugin.java.JavaPlugin#saveDefaultConfig()} on first run.
     */
    public void loadFromConfig() {
        plugin.reloadConfig();
        var cfg = plugin.getConfig();

        minLevel = Math.max(1, cfg.getInt("min-level", 1));
        maxLevel = Math.max(minLevel, cfg.getInt("max-level", 5));

        levelDefinitions.clear();
        ConfigurationSection levelsRoot = cfg.getConfigurationSection("levels");
        if (levelsRoot == null) {
            plugin.getLogger().severe("config.yml: missing 'levels' section. Using built-in defaults for all tiers.");
            for (int lv = minLevel; lv <= maxLevel; lv++) {
                levelDefinitions.put(lv, builtinDefault(lv));
            }
            return;
        }

        for (int lv = minLevel; lv <= maxLevel; lv++) {
            ConfigurationSection sec = levelsRoot.getConfigurationSection(String.valueOf(lv));
            if (sec == null) {
                plugin.getLogger().warning("config.yml: missing levels." + lv + ". Using built-in default for that tier.");
                levelDefinitions.put(lv, builtinDefault(lv));
            } else {
                levelDefinitions.put(lv, readLevelSection(sec, lv));
            }
        }
    }

    private SpearLevelDefinition readLevelSection(ConfigurationSection sec, int level) {
        String displayName = translate(sec.getString("display-name", "&7Spear"));
        String flavor = translate(sec.getString("flavor", ""));
        String tier = sec.getString("tier", "Tier " + level);
        Material material = readMaterial(sec.getString("material"), level);
        int lunge = Math.max(0, sec.getInt("lunge", 0));
        int sharpness = Math.max(0, sec.getInt("sharpness", 0));
        double bonus = sec.getDouble("bonus-damage", 0.0);
        List<String> rawLore = sec.getStringList("enchant-lore");
        List<String> enchantLore = new ArrayList<>();
        for (String line : rawLore) {
            enchantLore.add(translate(line));
        }
        return new SpearLevelDefinition(displayName, flavor, tier, material, lunge, sharpness, bonus, enchantLore);
    }

    private Material readMaterial(String rawMaterial, int level) {
        if (rawMaterial != null && !rawMaterial.isBlank()) {
            Material material = Material.matchMaterial(rawMaterial);
            if (material != null) {
                return material;
            }
            plugin.getLogger().warning("config.yml: unknown material '" + rawMaterial
                    + "' for levels." + level + ". Using built-in spear material for that tier.");
        }
        return defaultMaterial(level);
    }

    private Material defaultMaterial(int level) {
        return switch (level) {
            case 1 -> Material.WOODEN_SPEAR;
            case 2 -> Material.STONE_SPEAR;
            case 3 -> Material.IRON_SPEAR;
            case 4 -> Material.DIAMOND_SPEAR;
            case 5 -> Material.NETHERITE_SPEAR;
            default -> Material.WOODEN_SPEAR;
        };
    }

    /** Matches original plugin defaults when a tier section is absent. */
    private SpearLevelDefinition builtinDefault(int level) {
        return switch (level) {
            case 1 -> new SpearLevelDefinition(
                    ChatColor.GOLD + "Basic Wooden Spear",
                    ChatColor.GRAY + "A crude spear carved from wood.",
                    "Tier 1", Material.WOODEN_SPEAR, 0, 0, 0.0, List.of());
            case 2 -> new SpearLevelDefinition(
                    ChatColor.GRAY + "Stone Spear",
                    ChatColor.GRAY + "A sturdy stone-tipped spear.",
                    "Tier 2", Material.STONE_SPEAR, 2, 0, 0.0,
                    List.of(ChatColor.AQUA + "Lunge II"));
            case 3 -> new SpearLevelDefinition(
                    ChatColor.WHITE + "Iron Spear",
                    ChatColor.GRAY + "A razor-sharp iron spear.",
                    "Tier 3", Material.IRON_SPEAR, 2, 2, 0.0,
                    List.of(ChatColor.AQUA + "Lunge II", ChatColor.YELLOW + "Sharpness II"));
            case 4 -> new SpearLevelDefinition(
                    ChatColor.AQUA + "Diamond Spear",
                    ChatColor.GRAY + "A brilliant diamond-forged spear.",
                    "Tier 4", Material.DIAMOND_SPEAR, 3, 5, 0.0,
                    List.of(ChatColor.AQUA + "Lunge III", ChatColor.YELLOW + "Sharpness V"));
            case 5 -> new SpearLevelDefinition(
                    ChatColor.DARK_RED + "Netherite Spear",
                    ChatColor.GRAY + "An unstoppable spear forged in hellfire.",
                    "Tier 5 (MAX)", Material.NETHERITE_SPEAR, 3, 5, 3.0,
                    List.of(ChatColor.AQUA + "Lunge III", ChatColor.YELLOW + "Sharpness X"));
            default -> new SpearLevelDefinition(
                    ChatColor.GRAY + "Spear",
                    "",
                    "Tier " + level, defaultMaterial(level), 0, 0, 0.0, List.of());
        };
    }

    private static String translate(String raw) {
        if (raw == null) return "";
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public NamespacedKey getSpearLevelKey() {
        return spearLevelKey;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public boolean isSpear(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(spearLevelKey, PersistentDataType.INTEGER);
    }

    public int getSpearLevel(ItemStack item) {
        if (!isSpear(item)) return 0;
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(spearLevelKey, PersistentDataType.INTEGER, minLevel);
    }

    public ItemStack createSpear(int level) {
        level = Math.max(minLevel, Math.min(maxLevel, level));
        SpearLevelDefinition def = levelDefinitions.get(level);
        if (def == null) {
            plugin.getLogger().warning("No definition for spear level " + level + "; clamped data may be stale.");
            def = builtinDefault(Math.min(5, Math.max(1, level)));
        }

        ItemStack spear = new ItemStack(def.material());
        ItemMeta meta = spear.getItemMeta();
        meta.setDisplayName(def.displayName());
        meta.setLore(buildLore(def));

        int lunge = def.lunge();
        if (lunge > 0) {
            meta.addEnchant(Enchantment.LUNGE, lunge, true);
        }

        int sharp = def.sharpness();
        if (sharp > 0) {
            meta.addEnchant(Enchantment.SHARPNESS, sharp, true);
        }

        meta.getPersistentDataContainer().set(spearLevelKey, PersistentDataType.INTEGER, level);
        spear.setItemMeta(meta);
        return spear;
    }

    private List<String> buildLore(SpearLevelDefinition def) {
        List<String> lore = new ArrayList<>();
        if (!def.flavor().isEmpty()) {
            lore.add(def.flavor());
        }
        lore.add(ChatColor.DARK_GRAY + translate(def.tierRaw()));
        if (!def.enchantLore().isEmpty()) {
            lore.add("");
            lore.addAll(def.enchantLore());
        }
        return lore;
    }

    /** Extra damage added each hit (on top of vanilla + Sharpness). */
    public double getBonusDamage(int spearLevel) {
        SpearLevelDefinition def = levelDefinitions.get(spearLevel);
        return def != null ? def.bonusDamage() : 0.0;
    }

    public String getDisplayName(int level) {
        SpearLevelDefinition def = levelDefinitions.get(level);
        if (def != null) return def.displayName();
        return ChatColor.GRAY + "Spear";
    }

    /**
     * One tier’s name, material, lore lines, Lunge/Sharpness enchantments, and bonus hit damage.
     */
    public record SpearLevelDefinition(
            String displayName,
            String flavor,
            String tierRaw,
            Material material,
            int lunge,
            int sharpness,
            double bonusDamage,
            List<String> enchantLore
    ) {
        public SpearLevelDefinition {
            enchantLore = Collections.unmodifiableList(new ArrayList<>(enchantLore));
        }
    }
}
