package dev.grimforge.listener;

import dev.grimforge.Modifier;
import dev.grimforge.Settings;
import dev.grimforge.ToolData;
import dev.grimforge.GrimforgePlugin;
import dev.grimforge.rune.RuneListener;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** The in-game effects of each modifier. Haste and Lucky are plain enchantments and need nothing here. */
public final class ModifierListener implements Listener {

    private final GrimforgePlugin plugin;

    public ModifierListener(GrimforgePlugin plugin) {
        this.plugin = plugin;
    }

    private Settings settings() {
        return plugin.settings();
    }

    private int level(ItemStack item, Modifier modifier) {
        return settings().enabled(modifier) ? ToolData.modifierLevel(item, modifier) : 0;
    }

    // Reinforced: chance to skip durability loss.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDurability(PlayerItemDamageEvent event) {
        int level = level(event.getItem(), Modifier.REINFORCED);
        if (level > 0 && ThreadLocalRandom.current().nextDouble() < level * settings().reinforcedChance) {
            event.setCancelled(true);
        }
    }

    // Autosmelt, then Magnetic, on block drops.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrops(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (level(tool, Modifier.AUTOSMELT) > 0) {
            for (Item drop : event.getItems()) {
                ItemStack stack = drop.getItemStack();
                ItemStack smelted = plugin.smelting().result(stack.getType());
                if (smelted != null) {
                    smelted.setAmount(Math.min(smelted.getMaxStackSize(), stack.getAmount() * smelted.getAmount()));
                    drop.setItemStack(smelted);
                }
            }
        }

        if (level(tool, Modifier.MAGNETIC) > 0) {
            Iterator<Item> drops = event.getItems().iterator();
            while (drops.hasNext()) {
                Item drop = drops.next();
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(drop.getItemStack().clone());
                if (leftover.isEmpty()) {
                    drops.remove();
                } else {
                    drop.setItemStack(leftover.values().iterator().next());
                }
            }
        }
    }

    // Experienced: more XP orbs from blocks.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreakExp(BlockBreakEvent event) {
        int level = level(event.getPlayer().getInventory().getItemInMainHand(), Modifier.EXPERIENCED);
        if (level > 0 && event.getExpToDrop() > 0) {
            event.setExpToDrop((int) Math.round(event.getExpToDrop() * (1 + level * settings().experiencedBonus)));
        }
    }

    // Experienced: more XP orbs from mobs.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onKillExp(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        int level = level(killer.getInventory().getItemInMainHand(), Modifier.EXPERIENCED);
        if (level > 0 && event.getDroppedExp() > 0) {
            event.setDroppedExp((int) Math.round(event.getDroppedExp() * (1 + level * settings().experiencedBonus)));
        }
    }

    // Sharp: flat bonus melee damage.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (RuneListener.isProcDamage() || !(event.getDamager() instanceof Player player)) {
            return;
        }
        if (event.getEntity() instanceof Player target && plugin.factions().isFriendly(player, target)) {
            return;
        }
        int level = level(player.getInventory().getItemInMainHand(), Modifier.SHARP);
        if (level > 0) {
            double perLevel = event.getEntity() instanceof Player ? settings().sharpDamagePlayers : settings().sharpDamage;
            event.setDamage(event.getDamage() + level * perLevel);
        }
    }

    // Lifesteal: heal after the final damage is known.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHitHeal(EntityDamageByEntityEvent event) {
        if (RuneListener.isProcDamage() || !(event.getDamager() instanceof Player player) || player.isDead()) {
            return;
        }
        int level = level(player.getInventory().getItemInMainHand(), Modifier.LIFESTEAL);
        if (level <= 0) {
            return;
        }
        double perLevel = event.getEntity() instanceof Player ? settings().lifestealPercentPlayers : settings().lifestealPercent;
        double heal = event.getFinalDamage() * level * perLevel;
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        double cap = maxHealth == null ? 20.0 : maxHealth.getValue();
        player.setHealth(Math.min(cap, player.getHealth() + heal));
    }
}
