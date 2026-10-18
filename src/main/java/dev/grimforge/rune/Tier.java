package dev.grimforge.rune;

import net.kyori.adventure.text.format.TextColor;

/** Rune rarity, coloured like ARPG loot: blue, yellow, orange. */
public enum Tier {
    MAGIC("Magic", TextColor.color(0x6969ff)),
    RARE("Rare", TextColor.color(0xffff55)),
    LEGENDARY("Legendary", TextColor.color(0xff8000));

    private final String displayName;
    private final TextColor color;

    Tier(String displayName, TextColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public TextColor color() {
        return color;
    }

    /** 0 for Magic, 1 for Rare, 2 for Legendary; indexes the per-tier config lists. */
    public int index() {
        return ordinal();
    }
}
