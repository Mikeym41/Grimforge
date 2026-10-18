package dev.grimforge;

import dev.grimforge.rune.RuneSettings;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** A read-only snapshot of config.yml, rebuilt on /tool reload. */
public final class Settings {

    public final int maxLevel;
    public final double curveBase;
    public final double curveMultiplier;
    public final int pointsPerLevel;
    public final boolean actionBar;

    public final int blockDefaultXp;
    public final int cropXp;
    public final boolean ignorePlayerPlaced;
    public final Map<Material, Integer> blockXp = new EnumMap<>(Material.class);

    public final int mobDefaultXp;
    public final boolean ignoreSpawnerMobs;
    public final Map<EntityType, Integer> mobXp = new EnumMap<>(EntityType.class);

    private final Map<Modifier, Integer> modifierMax = new EnumMap<>(Modifier.class);
    public final double reinforcedChance;
    public final double experiencedBonus;
    public final double sharpDamage;
    public final double sharpDamagePlayers;
    public final double lifestealPercent;
    public final double lifestealPercentPlayers;

    public final RuneSettings runes;
    public final boolean swordVisuals;
    public final boolean factionsEnabled;
    public final boolean factionsTruceFriendly;
    public final List<Integer> swordStageLevels;

    public Settings(FileConfiguration config, Logger logger) {
        runes = new RuneSettings(config, logger);
        swordVisuals = config.getBoolean("visuals.evolving-swords", true);
        factionsEnabled = config.getBoolean("factions.enabled", true);
        factionsTruceFriendly = config.getBoolean("factions.truce-is-friendly", true);
        List<Integer> stages = new ArrayList<>(config.getIntegerList("visuals.sword-stage-levels"));
        if (stages.isEmpty()) {
            stages.addAll(List.of(10, 15, 20));
        }
        stages.sort(Integer::compare);
        swordStageLevels = List.copyOf(stages.subList(0, Math.min(3, stages.size())));
        maxLevel = Math.max(1, config.getInt("max-level", 20));
        curveBase = config.getDouble("xp-curve.base", 50);
        curveMultiplier = config.getDouble("xp-curve.multiplier", 1.2);
        pointsPerLevel = config.getInt("points-per-level", 1);
        actionBar = config.getBoolean("show-xp-actionbar", true);

        blockDefaultXp = config.getInt("xp.blocks.default", 1);
        cropXp = config.getInt("xp.blocks.grown-crop", 2);
        ignorePlayerPlaced = config.getBoolean("xp.blocks.ignore-player-placed", true);
        ConfigurationSection blocks = config.getConfigurationSection("xp.blocks.overrides");
        if (blocks != null) {
            for (String key : blocks.getKeys(false)) {
                Material material = Material.matchMaterial(key);
                if (material == null) {
                    logger.warning("Unknown block in xp.blocks.overrides: " + key);
                } else {
                    blockXp.put(material, blocks.getInt(key));
                }
            }
        }

        mobDefaultXp = config.getInt("xp.mobs.default", 5);
        ignoreSpawnerMobs = config.getBoolean("xp.mobs.ignore-spawner-mobs", true);
        ConfigurationSection mobs = config.getConfigurationSection("xp.mobs.overrides");
        if (mobs != null) {
            for (String key : mobs.getKeys(false)) {
                try {
                    mobXp.put(EntityType.valueOf(key.toUpperCase()), mobs.getInt(key));
                } catch (IllegalArgumentException e) {
                    logger.warning("Unknown mob in xp.mobs.overrides: " + key);
                }
            }
        }

        for (Modifier modifier : Modifier.values()) {
            String path = "modifiers." + modifier.key();
            boolean enabled = config.getBoolean(path + ".enabled", true);
            modifierMax.put(modifier, enabled ? Math.max(0, config.getInt(path + ".max-level", 1)) : 0);
        }
        reinforcedChance = config.getDouble("modifiers.reinforced.chance-per-level", 0.10);
        experiencedBonus = config.getDouble("modifiers.experienced.bonus-per-level", 0.50);
        sharpDamage = config.getDouble("modifiers.sharp.damage-per-level", 1.0);
        sharpDamagePlayers = config.getDouble("modifiers.sharp.damage-per-level-vs-players", 0.2);
        lifestealPercent = config.getDouble("modifiers.lifesteal.heal-per-level", 0.05);
        lifestealPercentPlayers = config.getDouble("modifiers.lifesteal.heal-per-level-vs-players", 0.02);
    }

    /** XP needed to go from {@code level} to {@code level + 1}. */
    public int xpToNext(int level) {
        return (int) Math.max(1, Math.round(curveBase * Math.pow(curveMultiplier, level - 1)));
    }

    /** 0 = vanilla look, 1 = Tempered, 2 = Runed, 3 = Infernal. */
    public int swordStage(int level) {
        if (!swordVisuals) {
            return 0;
        }
        int stage = 0;
        for (int required : swordStageLevels) {
            if (level >= required) {
                stage++;
            }
        }
        return stage;
    }

    /** 0 means the modifier is disabled. */
    public int maxLevel(Modifier modifier) {
        return modifierMax.getOrDefault(modifier, 0);
    }

    public boolean enabled(Modifier modifier) {
        return maxLevel(modifier) > 0;
    }
}
