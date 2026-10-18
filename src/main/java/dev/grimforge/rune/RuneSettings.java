package dev.grimforge.rune;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** The runes: section of config.yml. */
public final class RuneSettings {

    public final boolean enabled;
    public final boolean debug;
    public final List<Integer> socketLevels;

    public final double pvpMultiplier;
    public final double minAttackCharge;
    public final double crushingCapPlayers;
    public final double crushingCapMobs;
    public final double retributionCap;

    public final double dropMagic;
    public final double dropRare;
    public final double dropLegendary;
    public final double bossLegendaryChance;

    private final Map<RuneType, TypeSettings> types = new EnumMap<>(RuneType.class);

    public RuneSettings(FileConfiguration config, Logger logger) {
        enabled = config.getBoolean("runes.enabled", true);
        debug = config.getBoolean("runes.debug", false);
        List<Integer> levels = new ArrayList<>(config.getIntegerList("runes.socket-levels"));
        if (levels.isEmpty()) {
            levels.addAll(List.of(5, 10, 15));
        }
        levels.sort(Integer::compare);
        socketLevels = List.copyOf(levels);

        pvpMultiplier = config.getDouble("runes.pvp.damage-multiplier", 0.6);
        minAttackCharge = config.getDouble("runes.pvp.min-attack-charge", 0.9);
        crushingCapPlayers = config.getDouble("runes.pvp.crushing-blow-cap-players", 4);
        crushingCapMobs = config.getDouble("runes.pvp.crushing-blow-cap-mobs", 20);
        retributionCap = config.getDouble("runes.pvp.retribution-cap", 4);

        dropMagic = config.getDouble("runes.drops.magic", 0.02);
        dropRare = config.getDouble("runes.drops.rare", 0.005);
        dropLegendary = config.getDouble("runes.drops.legendary", 0.001);
        bossLegendaryChance = config.getDouble("runes.drops.boss-legendary-chance", 0.3);

        for (RuneType type : RuneType.values()) {
            ConfigurationSection section = config.getConfigurationSection("runes.types." + type.key());
            types.put(type, new TypeSettings(section, type, logger));
        }
    }

    public TypeSettings type(RuneType type) {
        return types.get(type);
    }

    public boolean enabled(RuneType type) {
        return enabled && types.get(type).enabled;
    }

    public List<RuneType> enabledTypes() {
        return types.keySet().stream().filter(this::enabled).toList();
    }

    /** How many sockets a tool of this level has. */
    public int socketsAt(int level) {
        int count = 0;
        for (int required : socketLevels) {
            if (level >= required) {
                count++;
            }
        }
        return count;
    }

    public int stormTargets(Tier tier) {
        return Math.min(3, tier.index() + 2);
    }

    public static final class TypeSettings {
        public final boolean enabled;
        public final double cooldownSeconds;
        private final double[] chance;
        private final double[] power;

        TypeSettings(ConfigurationSection section, RuneType type, Logger logger) {
            if (section == null) {
                logger.warning("Missing config for rune " + type.key() + "; it is disabled.");
                enabled = false;
                cooldownSeconds = 10;
                chance = new double[]{0, 0, 0};
                power = new double[]{0, 0, 0};
                return;
            }
            enabled = section.getBoolean("enabled", true);
            cooldownSeconds = section.getDouble("cooldown", 10);
            chance = threeValues(section.getDoubleList("chance"), type, "chance", logger);
            power = threeValues(section.getDoubleList("power"), type, "power", logger);
        }

        public double chance(Tier tier) {
            return chance[tier.index()];
        }

        public double power(Tier tier) {
            return power[tier.index()];
        }

        private static double[] threeValues(List<Double> values, RuneType type, String name, Logger logger) {
            if (values.size() != 3) {
                logger.warning("runes.types." + type.key() + "." + name + " needs 3 values (magic, rare, legendary).");
            }
            double[] out = new double[3];
            for (int i = 0; i < 3; i++) {
                out[i] = values.isEmpty() ? 0 : values.get(Math.min(i, values.size() - 1));
            }
            return out;
        }
    }
}
