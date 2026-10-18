package dev.grimforge.rune;

import dev.grimforge.GrimforgePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Boss;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Rune drops from hostile mobs and bosses, and stops runes being used up in crafting or anvils. */
public final class RuneDropListener implements Listener {

    private final GrimforgePlugin plugin;

    public RuneDropListener(GrimforgePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        RuneSettings rs = plugin.settings().runes;
        LivingEntity dead = event.getEntity();
        Player killer = dead.getKiller();
        if (!rs.enabled || killer == null || dead instanceof Player || !(dead instanceof Enemy)) {
            return;
        }
        if (plugin.settings().ignoreSpawnerMobs && dead.fromMobSpawner()) {
            return;
        }
        List<RuneType> types = rs.enabledTypes();
        if (types.isEmpty()) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        Tier tier;
        if (isBoss(dead)) {
            tier = random.nextDouble() < rs.bossLegendaryChance ? Tier.LEGENDARY : Tier.RARE;
        } else {
            double roll = random.nextDouble();
            if (roll < rs.dropLegendary) {
                tier = Tier.LEGENDARY;
            } else if (roll < rs.dropLegendary + rs.dropRare) {
                tier = Tier.RARE;
            } else if (roll < rs.dropLegendary + rs.dropRare + rs.dropMagic) {
                tier = Tier.MAGIC;
            } else {
                return;
            }
        }

        Rune rune = new Rune(types.get(random.nextInt(types.size())), tier);
        event.getDrops().add(RuneItems.create(rune, rs));
        killer.sendMessage(Component.text("A ", NamedTextColor.GRAY)
                .append(Component.text(tier.displayName() + " Rune of " + rune.type().displayName(), tier.color()))
                .append(Component.text(" dropped!", NamedTextColor.GRAY)));
        if (tier != Tier.MAGIC) {
            killer.playSound(killer.getLocation(), tier == Tier.LEGENDARY
                    ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
        }
    }

    private static boolean isBoss(LivingEntity entity) {
        return entity instanceof Boss || entity instanceof Warden || entity instanceof ElderGuardian;
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (RuneItems.isRune(item)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        if (RuneItems.isRune(event.getInventory().getFirstItem()) || RuneItems.isRune(event.getInventory().getSecondItem())) {
            event.setResult(null);
        }
    }
}
