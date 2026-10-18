package dev.grimforge.listener;

import dev.grimforge.Settings;
import dev.grimforge.GrimforgePlugin;
import dev.grimforge.ToolType;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/** Awards tool XP for mining with the right tool, harvesting grown crops, and killing mobs. */
public final class XpListener implements Listener {

    private final GrimforgePlugin plugin;

    public XpListener(GrimforgePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!earnsXp(player)) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        ToolType type = ToolType.of(item);
        if (type == null || !type.isMiningTool()) {
            return;
        }

        Settings s = plugin.settings();
        Block block = event.getBlock();
        int xp;
        if (type == ToolType.HOE && block.getBlockData() instanceof Ageable crop) {
            // Crops are always player-planted, so they skip the placed-block check, but must be fully grown.
            if (crop.getAge() < crop.getMaximumAge()) {
                return;
            }
            xp = s.cropXp;
        } else {
            if (!block.isPreferredTool(item)) {
                return;
            }
            if (s.ignorePlayerPlaced && plugin.placedBlocks().isPlaced(block)) {
                return;
            }
            xp = s.blockXp.getOrDefault(block.getType(), s.blockDefaultXp);
        }
        plugin.tools().addXp(player, item, xp);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer == null || !earnsXp(killer) || dead instanceof Player || dead instanceof ArmorStand) {
            return;
        }
        Settings s = plugin.settings();
        if (s.ignoreSpawnerMobs && dead.fromMobSpawner()) {
            return;
        }
        ItemStack item = killer.getInventory().getItemInMainHand();
        if (ToolType.of(item) == null) {
            return;
        }
        plugin.tools().addXp(killer, item, s.mobXp.getOrDefault(dead.getType(), s.mobDefaultXp));
    }

    private static boolean earnsXp(Player player) {
        GameMode mode = player.getGameMode();
        return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
    }
}
