package dev.grimforge.rune;

import dev.grimforge.Keys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

import static dev.grimforge.ToolService.plain;

/** Creates and recognises rune items. All runes are echo shards tagged with persistent data. */
public final class RuneItems {

    public static final Material MATERIAL = Material.ECHO_SHARD;

    private RuneItems() {
    }

    public static ItemStack create(Rune rune, RuneSettings settings) {
        ItemStack item = new ItemStack(MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain(Component.text("Rune of " + rune.type().displayName(), rune.tier().color())));
        meta.lore(List.of(
                plain(Component.text(rune.tier().displayName() + " Rune", rune.tier().color())),
                plain(Component.text(rune.type().describe(settings.type(rune.type()), rune.tier(), settings), NamedTextColor.GRAY)),
                Component.empty(),
                plain(Component.text("Fits swords and axes.", NamedTextColor.DARK_GRAY)),
                plain(Component.text("Open /tool and click this rune to socket it.", NamedTextColor.DARK_GRAY))));
        meta.setEnchantmentGlintOverride(true);
        meta.addItemFlags(ItemFlag.values());
        meta.getPersistentDataContainer().set(Keys.RUNE, PersistentDataType.STRING, rune.encode());

        // Tells the optional Grimforge resource pack which texture to show. Without the pack,
        // players just see an echo shard.
        CustomModelDataComponent modelData = meta.getCustomModelDataComponent();
        modelData.setFloats(List.of((float) textureId(rune)));
        meta.setCustomModelDataComponent(modelData);
        item.setItemMeta(meta);
        return item;
    }

    /** 1001-1024: one per rune type and tier, in enum order. Must match the resource pack. */
    public static int textureId(Rune rune) {
        return 1001 + rune.type().ordinal() * Tier.values().length + rune.tier().ordinal();
    }

    /** Returns the rune this item represents, or null if it isn't a rune. */
    public static Rune read(ItemStack item) {
        if (item == null || item.getType() != MATERIAL || !item.hasItemMeta()) {
            return null;
        }
        String encoded = item.getItemMeta().getPersistentDataContainer().get(Keys.RUNE, PersistentDataType.STRING);
        return encoded == null ? null : Rune.decode(encoded);
    }

    public static boolean isRune(ItemStack item) {
        return read(item) != null;
    }
}
