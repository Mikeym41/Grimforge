package dev.grimforge.rune;

/**
 * Combat runes for swords and axes. Each has a proc chance, a "power" and a cooldown per tier,
 * all set in config.yml under runes.types. What "power" means is listed below.
 */
public enum RuneType {
    /** Sets the target on fire. Power: burn seconds. */
    EMBER("Ember"),
    /** Slowness I. Power: seconds. */
    FROST("Frost"),
    /** Poison I (can't kill, like vanilla poison). Power: seconds. */
    VENOM("Venom"),
    /** Arcs to nearby enemies. Power: damage per arc. */
    STORM("Storm"),
    /** 1 damage per second. Power: seconds. */
    BLEED("Bleed"),
    /** Bonus damage equal to a share of the target's current health, capped. Power: fraction. */
    CRUSHING_BLOW("Crushing Blow"),
    /** Regeneration I after a kill. Power: seconds. */
    SOUL_HARVEST("Soul Harvest"),
    /** Reflects part of the damage you take (before armor) while holding the weapon, capped. Power: fraction. */
    RETRIBUTION("Retribution");

    private final String displayName;

    RuneType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public String key() {
        return name().toLowerCase().replace('_', '-');
    }

    public String describe(RuneSettings.TypeSettings t, Tier tier, RuneSettings rs) {
        double power = t.power(tier);
        String chance = pct(t.chance(tier)) + " chance";
        String cd = " (" + fmt(t.cooldownSeconds) + "s cooldown)";
        return switch (this) {
            case EMBER -> chance + " to ignite for " + fmt(power) + "s" + cd;
            case FROST -> chance + " to slow for " + fmt(power) + "s" + cd;
            case VENOM -> chance + " to poison for " + fmt(power) + "s" + cd;
            case STORM -> chance + " to arc to " + rs.stormTargets(tier) + " enemies for " + fmt(power) + " damage" + cd;
            case BLEED -> chance + " to bleed for " + fmt(power) + "s" + cd;
            case CRUSHING_BLOW -> chance + " to deal " + pct(power) + " of current health (max "
                    + fmt(rs.crushingCapPlayers) + " vs players)" + cd;
            case SOUL_HARVEST -> "Kills grant Regeneration for " + fmt(power) + "s" + cd;
            case RETRIBUTION -> chance + " to reflect " + pct(power) + " of incoming damage before armor (max "
                    + fmt(rs.retributionCap) + ")" + cd;
        };
    }

    static String pct(double fraction) {
        return fmt(fraction * 100) + "%";
    }

    static String fmt(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
