package dev.grimforge;

import dev.grimforge.rune.Rune;
import dev.grimforge.rune.RuneItems;
import dev.grimforge.rune.RuneSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Levelling logic: adding XP, levelling up, spending points and keeping the lore in sync. */
public final class ToolService {

    private static final int BAR_SEGMENTS = 10;

    private final GrimforgePlugin plugin;

    public ToolService(GrimforgePlugin plugin) {
        this.plugin = plugin;
    }

    /** Adds XP to a tool. The item must be a live inventory stack (e.g. the main hand). */
    public void addXp(Player player, ItemStack item, int amount) {
        ToolType type = ToolType.of(item);
        if (type == null || amount <= 0) {
            return;
        }
        Settings s = plugin.settings();
        ItemMeta meta = item.getItemMeta();
        ToolData data = ToolData.read(meta);
        if (data.level >= s.maxLevel) {
            return;
        }

        int socketsBefore = socketCount(type, data.level);
        int stageBefore = type == ToolType.SWORD ? s.swordStage(data.level) : 0;
        data.xp += amount;
        int levelsGained = 0;
        while (data.level < s.maxLevel && data.xp >= s.xpToNext(data.level)) {
            data.xp -= s.xpToNext(data.level);
            data.level++;
            data.points += s.pointsPerLevel;
            levelsGained++;
        }
        if (data.level >= s.maxLevel) {
            data.xp = 0;
        }

        data.write(meta);
        refreshLore(meta, type, data);
        item.setItemMeta(meta);

        if (levelsGained > 0) {
            announceLevelUp(player, type, data);
            int stageNow = type == ToolType.SWORD ? s.swordStage(data.level) : 0;
            if (stageNow > stageBefore) {
                String[] names = {"", "Tempered", "Runed", "Infernal"};
                player.sendMessage(Component.text("⚔ Your sword evolved: " + names[stageNow] + "!", NamedTextColor.GOLD));
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 0.7f);
            }
            if (socketCount(type, data.level) > socketsBefore) {
                player.sendMessage(Component.text("◇ A rune socket opened on your " + type.displayName().toLowerCase()
                        + "! Socket runes with /tool.", NamedTextColor.LIGHT_PURPLE));
            }
        } else if (s.actionBar) {
            player.sendActionBar(Component.text("+" + amount + " " + type.displayName() + " XP  ", NamedTextColor.GREEN)
                    .append(Component.text(data.xp + "/" + s.xpToNext(data.level), NamedTextColor.GRAY)));
        }
    }

    /**
     * Spends one point on a modifier for the tool in the given inventory slot.
     *
     * @return null on success, otherwise a message explaining why it failed
     */
    public String upgrade(Player player, int slot, Modifier modifier) {
        ItemStack item = player.getInventory().getItem(slot);
        ToolType type = ToolType.of(item);
        if (type == null) {
            return "You're not holding a tool.";
        }
        Settings s = plugin.settings();
        if (!s.enabled(modifier) || !modifier.appliesTo(type)) {
            return modifier.displayName() + " doesn't fit a " + type.displayName().toLowerCase() + ".";
        }

        ItemMeta meta = item.getItemMeta();
        ToolData data = ToolData.read(meta);
        int current = data.modifier(modifier);
        if (current >= s.maxLevel(modifier)) {
            return modifier.displayName() + " is already maxed.";
        }
        if (data.points <= 0) {
            return "No modifier points left. Level up your tool to earn more.";
        }
        if (modifier == Modifier.LUCKY && meta.hasEnchant(Enchantment.SILK_TOUCH)) {
            return "Lucky doesn't work on a Silk Touch tool.";
        }

        data.modifiers.put(modifier, current + 1);
        data.points--;
        applyEnchantments(meta, modifier);
        data.write(meta);
        refreshLore(meta, type, data);
        item.setItemMeta(meta);
        player.getInventory().setItem(slot, item);
        return null;
    }

    public static boolean canSocket(ToolType type) {
        return type == ToolType.SWORD || type == ToolType.AXE;
    }

    /** Sockets a tool has at a level; 0 for tools that can't hold runes or when runes are off. */
    public int socketCount(ToolType type, int level) {
        RuneSettings rs = plugin.settings().runes;
        return rs.enabled && canSocket(type) ? rs.socketsAt(level) : 0;
    }

    /**
     * Moves one rune from the player's inventory slot into the first open socket of the tool in handSlot.
     *
     * @return null on success, otherwise a message explaining why it failed
     */
    public String socket(Player player, int handSlot, int runeSlot) {
        ItemStack tool = player.getInventory().getItem(handSlot);
        ItemStack runeItem = player.getInventory().getItem(runeSlot);
        Rune rune = RuneItems.read(runeItem);
        ToolType type = ToolType.of(tool);
        if (rune == null) {
            return null; // Not a rune; ignore the click silently.
        }
        if (type == null || !canSocket(type)) {
            return "Runes only fit swords and axes.";
        }
        if (!plugin.settings().runes.enabled(rune.type())) {
            return "That rune is disabled on this server.";
        }

        ItemMeta meta = tool.getItemMeta();
        ToolData data = ToolData.read(meta);
        int sockets = socketCount(type, data.level);
        if (sockets == 0) {
            return "No sockets yet. The first opens at level " + plugin.settings().runes.socketLevels.get(0) + ".";
        }
        if (data.runes.size() >= sockets) {
            return "All sockets are full. Shift-click a rune in the menu to shatter it.";
        }
        if (data.hasRune(rune.type())) {
            return "This weapon already has a Rune of " + rune.type().displayName() + ".";
        }

        data.runes.add(rune);
        data.write(meta);
        refreshLore(meta, type, data);
        tool.setItemMeta(meta);
        player.getInventory().setItem(handSlot, tool);

        runeItem.setAmount(runeItem.getAmount() - 1);
        player.getInventory().setItem(runeSlot, runeItem.getAmount() > 0 ? runeItem : null);
        return null;
    }

    /** Admin/testing shortcut: sockets a rune straight into the held weapon, skipping the menu. */
    public String socketDirect(Player player, Rune rune) {
        int slot = player.getInventory().firstEmpty();
        if (slot < 0) {
            return "Make room in your inventory first.";
        }
        player.getInventory().setItem(slot, RuneItems.create(rune, plugin.settings().runes));
        String error = socket(player, player.getInventory().getHeldItemSlot(), slot);
        if (error != null) {
            player.getInventory().setItem(slot, null);
        }
        return error;
    }

    /** Destroys the rune in a socket, freeing it. */
    public String shatter(Player player, int handSlot, int socketIndex) {
        ItemStack tool = player.getInventory().getItem(handSlot);
        ToolType type = ToolType.of(tool);
        if (type == null) {
            return "You're not holding a tool.";
        }
        ItemMeta meta = tool.getItemMeta();
        ToolData data = ToolData.read(meta);
        if (socketIndex < 0 || socketIndex >= data.runes.size()) {
            return null;
        }
        data.runes.remove(socketIndex);
        data.write(meta);
        refreshLore(meta, type, data);
        tool.setItemMeta(meta);
        player.getInventory().setItem(handSlot, tool);
        return null;
    }

    /** Admin/testing shortcut: sets a modifier's level on the held tool without spending points. */
    public String setModifier(Player player, Modifier modifier, int level) {
        int slot = player.getInventory().getHeldItemSlot();
        ItemStack item = player.getInventory().getItem(slot);
        ToolType type = ToolType.of(item);
        if (type == null) {
            return "You're not holding a tool.";
        }
        if (!modifier.appliesTo(type)) {
            return modifier.displayName() + " doesn't fit a " + type.displayName().toLowerCase() + ".";
        }
        ItemMeta meta = item.getItemMeta();
        ToolData data = ToolData.read(meta);
        int current = data.modifier(modifier);
        int target = Math.max(0, level);
        for (int i = current; i < target; i++) {
            applyEnchantments(meta, modifier);
        }
        if (target == 0) {
            data.modifiers.remove(modifier);
        } else {
            data.modifiers.put(modifier, target);
        }
        data.write(meta);
        refreshLore(meta, type, data);
        item.setItemMeta(meta);
        player.getInventory().setItem(slot, item);
        return null;
    }

    /** Modifiers that piggyback on vanilla enchantments add one level per upgrade. */
    private void applyEnchantments(ItemMeta meta, Modifier modifier) {
        Enchantment enchantment = switch (modifier) {
            case HASTE -> Enchantment.EFFICIENCY;
            case LUCKY -> Enchantment.FORTUNE;
            default -> null;
        };
        if (enchantment != null) {
            meta.addEnchant(enchantment, meta.getEnchantLevel(enchantment) + 1, true);
        }
    }

    private void announceLevelUp(Player player, ToolType type, ToolData data) {
        player.showTitle(Title.title(
                Component.text("Level Up!", NamedTextColor.GOLD),
                Component.text(type.displayName() + " reached level " + data.level + " · /tool to upgrade", NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2000), Duration.ofMillis(500))));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
    }

    /** Rewrites the lines this plugin owns at the top of the lore, keeping anything else below. */
    public void refreshLore(ItemMeta meta, ToolType type, ToolData data) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int owned = pdc.getOrDefault(Keys.LORE_LINES, PersistentDataType.INTEGER, 0);
        List<Component> existing = meta.hasLore() ? meta.lore() : null;
        List<Component> others = new ArrayList<>();
        if (existing != null && existing.size() > owned) {
            others.addAll(existing.subList(owned, existing.size()));
        }

        List<Component> lines = buildLore(type, data);
        List<Component> combined = new ArrayList<>(lines);
        combined.addAll(others);
        meta.lore(combined);
        pdc.set(Keys.LORE_LINES, PersistentDataType.INTEGER, lines.size());
        applySwordLook(meta, type, data);
    }

    /**
     * Sets custom model data 1/2/3 on swords at each evolution stage, which the optional resource
     * pack turns into the Tempered/Runed/Infernal textures. Leaves alone any sword whose model data
     * another plugin set.
     */
    private void applySwordLook(ItemMeta meta, ToolType type, ToolData data) {
        if (type != ToolType.SWORD) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean ours = pdc.has(Keys.VISUAL_STAGE, PersistentDataType.INTEGER);
        CustomModelDataComponent modelData = meta.getCustomModelDataComponent();
        if (!ours && !modelData.getFloats().isEmpty()) {
            return;
        }
        int stage = plugin.settings().swordStage(data.level);
        if (stage == 0) {
            if (ours) {
                modelData.setFloats(List.of());
                meta.setCustomModelDataComponent(modelData);
                pdc.remove(Keys.VISUAL_STAGE);
            }
            return;
        }
        modelData.setFloats(List.of((float) stage));
        meta.setCustomModelDataComponent(modelData);
        pdc.set(Keys.VISUAL_STAGE, PersistentDataType.INTEGER, stage);
    }

    private List<Component> buildLore(ToolType type, ToolData data) {
        Settings s = plugin.settings();
        List<Component> lines = new ArrayList<>();
        lines.add(plain(Component.text("Level " + data.level + " " + type.displayName(), NamedTextColor.GOLD)));

        if (data.level >= s.maxLevel) {
            lines.add(plain(Component.text("MAX LEVEL", NamedTextColor.LIGHT_PURPLE)));
        } else {
            int needed = s.xpToNext(data.level);
            int filled = Math.min(BAR_SEGMENTS, data.xp * BAR_SEGMENTS / needed);
            lines.add(plain(Component.text("■".repeat(filled), NamedTextColor.GREEN)
                    .append(Component.text("■".repeat(BAR_SEGMENTS - filled), NamedTextColor.DARK_GRAY))
                    .append(Component.text(" " + data.xp + "/" + needed + " XP", NamedTextColor.GRAY))));
        }

        if (data.points > 0) {
            lines.add(plain(Component.text("✦ " + data.points + " modifier point" + (data.points == 1 ? "" : "s")
                    + " · /tool", NamedTextColor.AQUA)));
        }
        for (Modifier modifier : Modifier.values()) {
            int level = data.modifier(modifier);
            if (level > 0) {
                lines.add(plain(Component.text("• " + modifier.displayName() + " " + roman(level),
                        TextColor.color(0x9ad0ff))));
            }
        }

        int sockets = socketCount(type, data.level);
        for (int i = 0; i < sockets; i++) {
            if (i < data.runes.size()) {
                Rune rune = data.runes.get(i);
                lines.add(plain(Component.text("◆ Rune of " + rune.type().displayName() + " ", rune.tier().color())
                        .append(Component.text("(" + rune.tier().displayName() + ")", NamedTextColor.DARK_GRAY))));
            } else {
                lines.add(plain(Component.text("◇ Empty socket", NamedTextColor.DARK_GRAY)));
            }
        }
        return lines;
    }

    public static Component plain(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    public static String roman(int number) {
        String[] numerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return number >= 0 && number < numerals.length ? numerals[number] : String.valueOf(number);
    }
}
