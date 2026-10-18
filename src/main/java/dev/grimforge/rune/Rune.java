package dev.grimforge.rune;

/** A rune of a given type and tier, either as an item or socketed in a tool. */
public record Rune(RuneType type, Tier tier) {

    public String encode() {
        return type.name() + ":" + tier.name();
    }

    /** Returns null for malformed or removed rune types. */
    public static Rune decode(String encoded) {
        String[] parts = encoded.split(":");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new Rune(RuneType.valueOf(parts[0]), Tier.valueOf(parts[1]));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
