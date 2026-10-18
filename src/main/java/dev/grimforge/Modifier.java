package dev.grimforge;

import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Set;

import static dev.grimforge.ToolType.AXE;
import static dev.grimforge.ToolType.HOE;
import static dev.grimforge.ToolType.PICKAXE;
import static dev.grimforge.ToolType.SHOVEL;
import static dev.grimforge.ToolType.SWORD;

/** Upgrades a player can spend modifier points on. Max levels and strengths come from config. */
public enum Modifier {
    HASTE("Haste", Material.SUGAR, EnumSet.of(PICKAXE, AXE, SHOVEL, HOE)),
    LUCKY("Lucky", Material.EMERALD, EnumSet.of(PICKAXE, AXE, SHOVEL, HOE)),
    AUTOSMELT("Autosmelt", Material.BLAZE_POWDER, EnumSet.of(PICKAXE, SHOVEL)),
    MAGNETIC("Magnetic", Material.LODESTONE, EnumSet.allOf(ToolType.class)),
    REINFORCED("Reinforced", Material.OBSIDIAN, EnumSet.allOf(ToolType.class)),
    EXPERIENCED("Experienced", Material.EXPERIENCE_BOTTLE, EnumSet.allOf(ToolType.class)),
    SHARP("Sharp", Material.QUARTZ, EnumSet.of(SWORD, AXE)),
    LIFESTEAL("Lifesteal", Material.GHAST_TEAR, EnumSet.of(SWORD, AXE));

    private final String displayName;
    private final Material icon;
    private final Set<ToolType> appliesTo;

    Modifier(String displayName, Material icon, Set<ToolType> appliesTo) {
        this.displayName = displayName;
        this.icon = icon;
        this.appliesTo = appliesTo;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }

    public boolean appliesTo(ToolType type) {
        return appliesTo.contains(type);
    }

    /** The config key, e.g. "autosmelt". */
    public String key() {
        return name().toLowerCase();
    }

    public String describe(Settings s) {
        return switch (this) {
            case HASTE -> "+1 Efficiency per level";
            case LUCKY -> "+1 Fortune per level";
            case AUTOSMELT -> "Smelts what you mine";
            case MAGNETIC -> "Drops go straight to your inventory";
            case REINFORCED -> percent(s.reinforcedChance) + " chance per level to skip durability loss";
            case EXPERIENCED -> "+" + percent(s.experiencedBonus) + " XP orbs per level";
            case SHARP -> "+" + trim(s.sharpDamage) + " attack damage per level (+" + trim(s.sharpDamagePlayers) + " vs players)";
            case LIFESTEAL -> "Heal " + percent(s.lifestealPercent) + " of damage dealt per level ("
                    + percent(s.lifestealPercentPlayers) + " vs players)";
        };
    }

    private static String percent(double fraction) {
        return trim(fraction * 100) + "%";
    }

    private static String trim(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
