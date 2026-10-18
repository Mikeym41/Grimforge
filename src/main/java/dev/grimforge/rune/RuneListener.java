package dev.grimforge.rune;

import dev.grimforge.ToolData;
import dev.grimforge.GrimforgePlugin;
import dev.grimforge.hook.FactionsHook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Rune effects. PvP balance rules:
 * - Runes only proc on (nearly) fully charged attacks, so spam-clicking can't fish for procs.
 * - Every rune has its own cooldown per player.
 * - Bonus damage against players is multiplied by runes.pvp.damage-multiplier.
 * - Status effects are level I only, poison can't kill, and percentage damage is hard-capped.
 * - Proc damage is dealt as a normal hit from the player, so armour, PvP toggles and
 *   region plugins (e.g. WorldGuard) all still apply.
 */
public final class RuneListener implements Listener {

    /** True while a rune is dealing its own damage, so that damage doesn't trigger more procs. */
    private static boolean dealingProcDamage;

    private final GrimforgePlugin plugin;
    /** Server tick of each player's last swing (a hit or a swing at air), for working out attack charge. */
    private final Map<UUID, Integer> lastSwingTick = new HashMap<>();
    private final Map<UUID, Map<RuneType, Long>> cooldowns = new HashMap<>();
    private final Set<UUID> bleeding = new HashSet<>();

    public RuneListener(GrimforgePlugin plugin) {
        this.plugin = plugin;
    }

    public static boolean isProcDamage() {
        return dealingProcDamage;
    }

    private RuneSettings rs() {
        return plugin.settings().runes;
    }

    // Any arm swing restarts the attack charge, just like in vanilla. When a player hits an entity the
    // client sends the attack before the swing, so the damage event still sees the previous swing.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
            lastSwingTick.put(event.getPlayer().getUniqueId(), Bukkit.getCurrentTick());
        }
    }

    /**
     * How charged this attack was, from 0 to 1: ticks since the previous swing divided by the time a full
     * charge takes at the player's attack speed. Works out the same value vanilla uses for attack strength.
     */
    private float attackCharge(Player player) {
        int now = Bukkit.getCurrentTick();
        Integer last = lastSwingTick.put(player.getUniqueId(), now);
        if (last == null) {
            return 1f;
        }
        AttributeInstance speed = player.getAttribute(Attribute.ATTACK_SPEED);
        double fullChargeTicks = 20.0 / (speed == null ? 4.0 : Math.max(0.1, speed.getValue()));
        return (float) Math.min(1.0, (now - last + 0.5) / fullChargeTicks);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lastSwingTick.remove(id);
        cooldowns.remove(id);
    }

    // Offensive runes.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (dealingProcDamage || !rs().enabled
                || event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK // not sweeps or thorns
                || !(event.getDamager() instanceof Player attacker)
                || !(event.getEntity() instanceof LivingEntity target)
                || target instanceof ArmorStand) {
            return;
        }
        if (blocked(attacker, target)) {
            return;
        }
        ToolData data = ToolData.read(attacker.getInventory().getItemInMainHand());
        if (data == null || data.runes.isEmpty()) {
            return;
        }
        float charge = attackCharge(attacker);
        if (charge < rs().minAttackCharge) {
            return;
        }

        for (Rune rune : data.runes) {
            RuneType type = rune.type();
            if (type == RuneType.SOUL_HARVEST || type == RuneType.RETRIBUTION || !tryProc(attacker, rune)) {
                continue;
            }
            double power = rs().type(type).power(rune.tier());
            int ticks = (int) Math.round(power * 20);
            String detail = switch (type) {
                case EMBER, FROST, VENOM, BLEED -> fmt(power) + "s";
                case CRUSHING_BLOW -> "+" + fmt(Math.min(target instanceof Player ? rs().crushingCapPlayers : rs().crushingCapMobs,
                        target.getHealth() * power)) + " raw damage";
                case STORM -> fmt(power * rs().pvpMultiplier) + " per arc vs players, " + fmt(power) + " vs mobs";
                default -> "";
            };
            debug(attacker, rune, target, detail);
            switch (type) {
                case EMBER -> {
                    target.setFireTicks(Math.max(target.getFireTicks(), ticks));
                    target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.02);
                }
                case FROST -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, 0));
                    target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.02);
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.5f, 1.6f);
                }
                case VENOM -> target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, ticks, 0));
                case STORM -> chainLightning(attacker, target, power, rs().stormTargets(rune.tier()));
                case BLEED -> bleed(attacker, target, (int) Math.round(power));
                case CRUSHING_BLOW -> {
                    double cap = target instanceof Player ? rs().crushingCapPlayers : rs().crushingCapMobs;
                    double bonus = Math.min(cap, target.getHealth() * power);
                    event.setDamage(event.getDamage() + bonus);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 0.7f, 0.6f);
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.2);
                }
                default -> {
                }
            }
        }
    }

    // Retribution: reflect part of the damage you take while holding the weapon.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHurt(EntityDamageByEntityEvent event) {
        if (dealingProcDamage || !rs().enabled || !(event.getEntity() instanceof Player victim)) {
            return;
        }
        LivingEntity attacker = attackerOf(event.getDamager());
        if (attacker == null || attacker == victim || blocked(victim, attacker)) {
            return;
        }
        ToolData data = ToolData.read(victim.getInventory().getItemInMainHand());
        if (data == null) {
            return;
        }
        for (Rune rune : data.runes) {
            if (rune.type() == RuneType.RETRIBUTION && tryProc(victim, rune)) {
                // Based on damage before armor: armor shrinks real hits so much that a share of the
                // final damage was negligible in testing.
                double reflected = Math.min(rs().retributionCap, event.getDamage() * rs().type(RuneType.RETRIBUTION).power(rune.tier()));
                debug(victim, rune, attacker, fmt(scaled(attacker, reflected)) + " reflected");
                // No knockback: in testing, knocking the attacker into the air gave them falling
                // critical hits, so Retribution made its owner lose more often.
                procDamage(attacker, reflected, victim, false);
                victim.getWorld().playSound(victim.getLocation(), Sound.ENCHANT_THORNS_HIT, 0.8f, 0.8f);
            }
        }
    }

    // Soul Harvest: regeneration after a kill.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || !rs().enabled || event.getEntity() instanceof ArmorStand) {
            return;
        }
        ToolData data = ToolData.read(killer.getInventory().getItemInMainHand());
        if (data == null) {
            return;
        }
        for (Rune rune : data.runes) {
            if (rune.type() == RuneType.SOUL_HARVEST && tryProc(killer, rune)) {
                int ticks = (int) Math.round(rs().type(RuneType.SOUL_HARVEST).power(rune.tier()) * 20);
                debug(killer, rune, event.getEntity(), "Regeneration " + (ticks / 20) + "s");
                killer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, ticks, 0));
                killer.getWorld().spawnParticle(Particle.SOUL, event.getEntity().getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.03);
                killer.playSound(killer.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, 1f, 1f);
            }
        }
    }

    private double scaled(LivingEntity target, double amount) {
        return target instanceof Player ? amount * rs().pvpMultiplier : amount;
    }

    /** With runes.debug on, logs each proc to the console and tells the rune's owner. */
    private void debug(Player owner, Rune rune, LivingEntity target, String detail) {
        if (!rs().debug) {
            return;
        }
        String message = owner.getName() + " " + rune.type().key() + " " + rune.tier().name().toLowerCase()
                + " -> " + (target instanceof Player p ? p.getName() : target.getType().name().toLowerCase())
                + (detail.isEmpty() ? "" : " (" + detail + ")");
        plugin.getLogger().info("[RuneDebug] " + message);
        owner.sendMessage("[RuneDebug] " + message);
    }

    private static String fmt(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    /** Rolls the proc chance and checks/starts the cooldown. */
    private boolean tryProc(Player player, Rune rune) {
        if (!rs().enabled(rune.type())) {
            return false;
        }
        RuneSettings.TypeSettings t = rs().type(rune.type());
        Map<RuneType, Long> playerCooldowns = cooldowns.computeIfAbsent(player.getUniqueId(), id -> new EnumMap<>(RuneType.class));
        long now = System.currentTimeMillis();
        if (now < playerCooldowns.getOrDefault(rune.type(), 0L)) {
            return false;
        }
        if (ThreadLocalRandom.current().nextDouble() >= t.chance(rune.tier())) {
            return false;
        }
        playerCooldowns.put(rune.type(), now + (long) (t.cooldownSeconds * 1000));
        return true;
    }

    private void chainLightning(Player attacker, LivingEntity origin, double damage, int maxTargets) {
        List<LivingEntity> targets = origin.getNearbyEntities(5, 3, 5).stream()
                .filter(e -> e instanceof LivingEntity && !(e instanceof ArmorStand))
                .map(e -> (LivingEntity) e)
                .filter(e -> e != attacker && !e.isDead() && !isPetOf(e, attacker) && !blocked(attacker, e))
                .sorted(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(origin.getLocation())))
                .limit(maxTargets)
                .toList();

        LivingEntity from = origin;
        for (LivingEntity target : targets) {
            drawArc(from.getLocation().add(0, 1, 0), target.getLocation().add(0, 1, 0));
            procDamage(target, damage, attacker, true);
            from = target;
        }
        if (!targets.isEmpty()) {
            origin.getWorld().playSound(origin.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.5f);
        }
    }

    private void bleed(Player attacker, LivingEntity target, int seconds) {
        if (!bleeding.add(target.getUniqueId())) {
            return; // Already bleeding; bleeds don't stack.
        }
        new BukkitRunnable() {
            int remaining = seconds;

            @Override
            public void run() {
                if (remaining-- <= 0 || !target.isValid() || target.isDead()
                        || plugin.factions().isSafeZone(target.getLocation())) {
                    bleeding.remove(target.getUniqueId());
                    cancel();
                    return;
                }
                procDamage(target, 1.0, attacker.isOnline() ? attacker : null, false);
                target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0), 8, 0.2, 0.3, 0.2,
                        org.bukkit.Material.REDSTONE_BLOCK.createBlockData());
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Deals rune damage, scaled down against players. With knockback it's a normal hit from the source
     * (so PvP toggles and region plugins can block it); without, it's sourceless damage credited to the
     * source for the kill. Use no knockback for damage over time like Bleed, and for anything that hits
     * someone already in a fight: knockback launches them and hands them falling critical hits.
     */
    private void procDamage(LivingEntity target, double amount, Entity source, boolean knockback) {
        if (amount <= 0 || target.isDead()) {
            return;
        }
        if (target instanceof Player) {
            amount *= rs().pvpMultiplier;
        }
        // Rune damage ignores invulnerability frames, and doesn't grant new ones: otherwise a proc
        // could shield the target from the next real hit, which matters a lot in PvP.
        int noDamageTicks = target.getNoDamageTicks();
        target.setNoDamageTicks(0);
        dealingProcDamage = true;
        try {
            if (knockback && source != null) {
                target.damage(amount, source);
            } else {
                if (source instanceof Player player) {
                    target.setKiller(player);
                }
                target.damage(amount);
            }
        } finally {
            dealingProcDamage = false;
            target.setNoDamageTicks(noDamageTicks);
        }
    }

    private static LivingEntity attackerOf(Entity damager) {
        if (damager instanceof LivingEntity living) {
            return living;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity shooter) {
            return shooter;
        }
        return null;
    }

    /**
     * True if a rune from {@code owner} must not affect {@code target}: the target is a faction
     * member, ally or truce (or one of their pets), or either of them is standing in a safezone.
     */
    private boolean blocked(Player owner, LivingEntity target) {
        FactionsHook factions = plugin.factions();
        if (!factions.isActive()) {
            return false;
        }
        if (factions.isSafeZone(owner.getLocation()) || factions.isSafeZone(target.getLocation())) {
            return true;
        }
        if (target instanceof Player player) {
            return factions.isFriendly(owner, player);
        }
        if (target instanceof Tameable pet && pet.isTamed() && pet.getOwner() instanceof Player petOwner) {
            return factions.isFriendly(owner, petOwner);
        }
        return false;
    }

    private static boolean isPetOf(LivingEntity entity, Player player) {
        return entity instanceof Tameable pet && pet.isTamed() && player.getUniqueId().equals(
                pet.getOwnerUniqueId());
    }

    private static void drawArc(Location from, Location to) {
        Vector step = to.toVector().subtract(from.toVector());
        double length = step.length();
        if (length == 0) {
            return;
        }
        step.multiply(0.4 / length);
        Location point = from.clone();
        for (double d = 0; d < length; d += 0.4) {
            point.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0.05, 0.05, 0.05, 0);
            point.add(step);
        }
    }
}
