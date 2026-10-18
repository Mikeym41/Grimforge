package dev.grimforge;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/** Persistent data keys, stored on the item (tool data) or the chunk (placed blocks). */
public final class Keys {

    public static NamespacedKey LEVEL;
    public static NamespacedKey XP;
    public static NamespacedKey POINTS;
    public static NamespacedKey MODIFIERS;
    public static NamespacedKey LORE_LINES;
    public static NamespacedKey PLACED;
    public static NamespacedKey PLACED_BITS;
    public static NamespacedKey RUNE;
    public static NamespacedKey SOCKETS;
    public static NamespacedKey VISUAL_STAGE;

    private Keys() {
    }

    static void init(Plugin plugin) {
        LEVEL = new NamespacedKey(plugin, "level");
        XP = new NamespacedKey(plugin, "xp");
        POINTS = new NamespacedKey(plugin, "points");
        MODIFIERS = new NamespacedKey(plugin, "modifiers");
        LORE_LINES = new NamespacedKey(plugin, "lore_lines");
        PLACED = new NamespacedKey(plugin, "placed");
        PLACED_BITS = new NamespacedKey(plugin, "placed_bits");
        RUNE = new NamespacedKey(plugin, "rune");
        SOCKETS = new NamespacedKey(plugin, "sockets");
        VISUAL_STAGE = new NamespacedKey(plugin, "visual_stage");
    }
}
