package dev.grimforge.menu;

import dev.grimforge.Modifier;
import dev.grimforge.Settings;
import dev.grimforge.ToolData;
import dev.grimforge.GrimforgePlugin;
import dev.grimforge.ToolService;
import dev.grimforge.ToolType;
import dev.grimforge.rune.Rune;
import dev.grimforge.rune.RuneItems;
import dev.grimforge.rune.RuneSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.grimforge.ToolService.plain;
import static dev.grimforge.ToolService.roman;

/** Chest GUI for spending modifier points on the tool in the player's hand. */
public final class ModifierMenu implements InventoryHolder {

    private static final int SOCKET_HEADER_SLOT = 40;
    private static final int TOOL_SLOT = 4;
    private static final int POINTS_SLOT = 13;
    private static final int[] MODIFIER_SLOTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

    private final GrimforgePlugin plugin;
    private final Player player;
    private final int handSlot;
    private final Inventory inventory;
    private final int size;
    private final Map<Integer, Modifier> slotModifiers = new HashMap<>();
    /** Menu slot -> socket index. */
    private final Map<Integer, Integer> slotSockets = new HashMap<>();

    public ModifierMenu(GrimforgePlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.handSlot = player.getInventory().getHeldItemSlot();
        ToolType type = ToolType.of(player.getInventory().getItemInMainHand());
        boolean sockets = type != null && ToolService.canSocket(type) && plugin.settings().runes.enabled;
        this.size = sockets ? 54 : 36;
        this.inventory = Bukkit.createInventory(this, size, Component.text(sockets ? "Modifiers & Runes" : "Tool Modifiers"));
        render();
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    /** A click in the menu itself: upgrade a modifier, or shatter a socketed rune. */
    public void click(int rawSlot, boolean shift) {
        Modifier modifier = slotModifiers.get(rawSlot);
        if (modifier != null) {
            feedback(plugin.tools().upgrade(player, handSlot, modifier), Sound.BLOCK_ANVIL_USE);
            return;
        }

        Integer socket = slotSockets.get(rawSlot);
        if (socket == null) {
            return;
        }
        ToolData data = ToolData.read(player.getInventory().getItem(handSlot));
        boolean filled = data != null && socket < data.runes.size();
        if (!filled) {
            player.sendMessage(Component.text("Click a rune in your inventory to socket it.", NamedTextColor.GRAY));
        } else if (!shift) {
            player.sendMessage(Component.text("Shift-click to shatter this rune. It will be destroyed.", NamedTextColor.GRAY));
        } else {
            feedback(plugin.tools().shatter(player, handSlot, socket), Sound.BLOCK_GLASS_BREAK);
        }
    }

    /** A click on the player's own inventory while the menu is open: socket a rune. */
    public void clickOwnInventory(int inventorySlot) {
        if (inventorySlot == handSlot || !RuneItems.isRune(player.getInventory().getItem(inventorySlot))) {
            return;
        }
        feedback(plugin.tools().socket(player, handSlot, inventorySlot), Sound.BLOCK_AMETHYST_BLOCK_RESONATE);
    }

    private void feedback(String error, Sound success) {
        if (error == null) {
            player.playSound(player.getLocation(), success, 0.7f, 1.2f);
        } else {
            player.sendMessage(Component.text(error, NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
        }
        render();
    }

    private void render() {
        inventory.clear();
        slotModifiers.clear();
        slotSockets.clear();

        ItemStack filler = icon(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), List.of());
        for (int i = 0; i < size; i++) {
            inventory.setItem(i, filler);
        }

        ItemStack tool = player.getInventory().getItem(handSlot);
        ToolType type = ToolType.of(tool);
        if (type == null) {
            return;
        }
        Settings s = plugin.settings();
        ToolData data = ToolData.read(tool.getItemMeta());

        inventory.setItem(TOOL_SLOT, tool.clone());
        inventory.setItem(POINTS_SLOT, icon(Material.NETHER_STAR,
                Component.text(data.points + " modifier point" + (data.points == 1 ? "" : "s"), NamedTextColor.AQUA),
                List.of(Component.text("Earn 1 each time your tool levels up.", NamedTextColor.GRAY))));

        int next = 0;
        for (Modifier modifier : Modifier.values()) {
            if (!s.enabled(modifier) || !modifier.appliesTo(type) || next >= MODIFIER_SLOTS.length) {
                continue;
            }
            int slot = MODIFIER_SLOTS[next++];
            slotModifiers.put(slot, modifier);
            inventory.setItem(slot, modifierIcon(modifier, data, s));
        }

        if (size == 54) {
            renderSockets(type, data, s.runes);
        }
    }

    private void renderSockets(ToolType type, ToolData data, RuneSettings rs) {
        inventory.setItem(SOCKET_HEADER_SLOT, icon(Material.ECHO_SHARD, Component.text("Rune Sockets", NamedTextColor.LIGHT_PURPLE),
                List.of(Component.text("Runes drop from hostile mobs and bosses.", NamedTextColor.GRAY),
                        Component.text("Click a rune in your inventory to socket it.", NamedTextColor.GRAY),
                        Component.text("Shift-click a socketed rune to shatter it.", NamedTextColor.GRAY))));

        int total = rs.socketLevels.size();
        int open = plugin.tools().socketCount(type, data.level);
        int[] slots = socketSlots(total);
        for (int i = 0; i < slots.length; i++) {
            slotSockets.put(slots[i], i);
            ItemStack item;
            if (i < data.runes.size()) {
                Rune rune = data.runes.get(i);
                item = RuneItems.create(rune, rs);
                ItemMeta meta = item.getItemMeta();
                List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
                lore.subList(Math.max(0, lore.size() - 2), lore.size()).clear();
                lore.add(plain(Component.text("Shift-click to shatter", NamedTextColor.RED)));
                meta.lore(lore);
                item.setItemMeta(meta);
            } else if (i < open) {
                item = icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Component.text("Empty socket", NamedTextColor.WHITE),
                        List.of(Component.text("Click a rune in your inventory", NamedTextColor.GRAY)));
            } else {
                item = icon(Material.BLACK_STAINED_GLASS_PANE, Component.text("Locked socket", NamedTextColor.DARK_GRAY),
                        List.of(Component.text("Opens at level " + rs.socketLevels.get(i), NamedTextColor.GRAY)));
            }
            inventory.setItem(slots[i], item);
        }
    }

    /** Spreads up to 9 sockets across the bottom row, centred. */
    private static int[] socketSlots(int count) {
        count = Math.min(count, 9);
        return switch (count) {
            case 1 -> new int[]{49};
            case 2 -> new int[]{48, 50};
            case 3 -> new int[]{47, 49, 51};
            case 4 -> new int[]{46, 48, 50, 52};
            case 5 -> new int[]{45, 47, 49, 51, 53};
            default -> {
                int[] slots = new int[count];
                int start = 45 + (9 - count) / 2;
                for (int i = 0; i < count; i++) {
                    slots[i] = start + i;
                }
                yield slots;
            }
        };
    }

    private ItemStack modifierIcon(Modifier modifier, ToolData data, Settings s) {
        int level = data.modifier(modifier);
        int max = s.maxLevel(modifier);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(modifier.describe(s), NamedTextColor.GRAY));
        lore.add(Component.text("Level " + level + "/" + max, NamedTextColor.DARK_AQUA));
        lore.add(Component.empty());
        if (level >= max) {
            lore.add(Component.text("Maxed", NamedTextColor.GREEN));
        } else if (data.points > 0) {
            lore.add(Component.text("Click to upgrade (1 point)", NamedTextColor.YELLOW));
        } else {
            lore.add(Component.text("No points available", NamedTextColor.RED));
        }

        String title = modifier.displayName() + (level > 0 ? " " + roman(level) : "");
        ItemStack icon = icon(modifier.icon(), Component.text(title, level > 0 ? NamedTextColor.GOLD : NamedTextColor.WHITE), lore);
        if (level > 0) {
            ItemMeta meta = icon.getItemMeta();
            meta.setEnchantmentGlintOverride(true);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private static ItemStack icon(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain(name));
        meta.lore(lore.stream().map(line -> plain(line)).toList());
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }
}
