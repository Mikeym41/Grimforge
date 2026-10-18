package dev.grimforge;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import dev.grimforge.rune.Rune;
import dev.grimforge.rune.RuneType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/** A tool's level, XP, unspent points and modifiers, stored in the item's persistent data. */
public final class ToolData {

    public int level = 1;
    public int xp = 0;
    public int points = 0;
    public final Map<Modifier, Integer> modifiers = new EnumMap<>(Modifier.class);
    /** Socketed runes, in socket order. Empty sockets aren't stored; their count comes from the level. */
    public final List<Rune> runes = new ArrayList<>();

    public boolean hasRune(RuneType type) {
        return runes.stream().anyMatch(rune -> rune.type() == type);
    }

    public int modifier(Modifier modifier) {
        return modifiers.getOrDefault(modifier, 0);
    }

    /** Reads tool data from item meta; an untracked tool comes back as a fresh level 1 tool. */
    public static ToolData read(ItemMeta meta) {
        ToolData data = new ToolData();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        data.level = pdc.getOrDefault(Keys.LEVEL, PersistentDataType.INTEGER, 1);
        data.xp = pdc.getOrDefault(Keys.XP, PersistentDataType.INTEGER, 0);
        data.points = pdc.getOrDefault(Keys.POINTS, PersistentDataType.INTEGER, 0);

        String encoded = pdc.get(Keys.MODIFIERS, PersistentDataType.STRING);
        if (encoded != null && !encoded.isEmpty()) {
            for (String part : encoded.split(",")) {
                String[] pair = part.split(":");
                if (pair.length != 2) {
                    continue;
                }
                try {
                    data.modifiers.put(Modifier.valueOf(pair[0]), Integer.parseInt(pair[1]));
                } catch (IllegalArgumentException ignored) {
                    // Unknown or removed modifier; drop it.
                }
            }
        }
        String sockets = pdc.get(Keys.SOCKETS, PersistentDataType.STRING);
        if (sockets != null && !sockets.isEmpty()) {
            for (String part : sockets.split(",")) {
                Rune rune = Rune.decode(part);
                if (rune != null) {
                    data.runes.add(rune);
                }
            }
        }
        return data;
    }

    /** Convenience for listeners: returns null when the item isn't a tool. */
    public static ToolData read(ItemStack item) {
        if (ToolType.of(item) == null || !item.hasItemMeta()) {
            return null;
        }
        return read(item.getItemMeta());
    }

    public static int modifierLevel(ItemStack item, Modifier modifier) {
        ToolData data = read(item);
        return data == null ? 0 : data.modifier(modifier);
    }

    public void write(ItemMeta meta) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(Keys.LEVEL, PersistentDataType.INTEGER, level);
        pdc.set(Keys.XP, PersistentDataType.INTEGER, xp);
        pdc.set(Keys.POINTS, PersistentDataType.INTEGER, points);

        StringJoiner joiner = new StringJoiner(",");
        modifiers.forEach((modifier, lvl) -> joiner.add(modifier.name() + ":" + lvl));
        pdc.set(Keys.MODIFIERS, PersistentDataType.STRING, joiner.toString());

        StringJoiner sockets = new StringJoiner(",");
        runes.forEach(rune -> sockets.add(rune.encode()));
        pdc.set(Keys.SOCKETS, PersistentDataType.STRING, sockets.toString());
    }
}
