package dev.grimforge.hook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Optional link to FactionsUUID (and forks that keep its API, such as SaberFactions).
 * Uses reflection so Grimforge has no hard dependency: without a factions plugin every
 * check simply answers "not friendly" and "not a safezone".
 */
public final class FactionsHook {

    private final boolean active;
    private final boolean truceIsFriendly;

    private Object fPlayers;
    private Method getByPlayer;
    private Method getRelationTo;
    private Object board;
    private Method getFactionAt;
    private Constructor<?> fLocationFromLocation;
    private Method isSafeZone;

    public FactionsHook(boolean enabled, boolean truceIsFriendly, Logger logger) {
        this.truceIsFriendly = truceIsFriendly;
        Plugin factions = Bukkit.getPluginManager().getPlugin("Factions");
        if (!enabled || factions == null || !factions.isEnabled()) {
            active = false;
            return;
        }
        boolean ok;
        try {
            ClassLoader loader = factions.getClass().getClassLoader();
            Class<?> fPlayersClass = Class.forName("com.massivecraft.factions.FPlayers", true, loader);
            Class<?> fPlayerClass = Class.forName("com.massivecraft.factions.FPlayer", true, loader);
            Class<?> boardClass = Class.forName("com.massivecraft.factions.Board", true, loader);
            Class<?> fLocationClass = Class.forName("com.massivecraft.factions.FLocation", true, loader);
            Class<?> factionClass = Class.forName("com.massivecraft.factions.Faction", true, loader);

            fPlayers = fPlayersClass.getMethod("getInstance").invoke(null);
            getByPlayer = fPlayersClass.getMethod("getByPlayer", Player.class);
            getRelationTo = findRelationMethod(fPlayerClass);
            board = boardClass.getMethod("getInstance").invoke(null);
            getFactionAt = boardClass.getMethod("getFactionAt", fLocationClass);
            fLocationFromLocation = fLocationClass.getConstructor(Location.class);
            isSafeZone = factionClass.getMethod("isSafeZone");
            ok = getRelationTo != null;
        } catch (ReflectiveOperationException | LinkageError e) {
            logger.warning("Found a Factions plugin but couldn't use its API (" + e + "). Faction checks are off.");
            ok = false;
        }
        active = ok;
        if (active) {
            logger.info("Hooked into " + factions.getName() + " " + factions.getPluginMeta().getVersion()
                    + ": runes won't hit allies and are off in safezones.");
        }
    }

    /** The one-argument getRelationTo, whose parameter type differs between Factions forks. */
    private static Method findRelationMethod(Class<?> fPlayerClass) {
        for (Method method : fPlayerClass.getMethods()) {
            if (method.getName().equals("getRelationTo") && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isAssignableFrom(fPlayerClass)) {
                return method;
            }
        }
        return null;
    }

    public boolean isActive() {
        return active;
    }

    /** True if the two players are in the same faction, allied, or (if configured) at truce. */
    public boolean isFriendly(Player a, Player b) {
        if (!active || a.equals(b)) {
            return active && a.equals(b);
        }
        try {
            Object fa = getByPlayer.invoke(fPlayers, a);
            Object fb = getByPlayer.invoke(fPlayers, b);
            if (fa == null || fb == null) {
                return false;
            }
            Object relation = getRelationTo.invoke(fa, fb);
            String name = relation instanceof Enum<?> e ? e.name() : String.valueOf(relation);
            return name.equals("MEMBER") || name.equals("ALLY") || (truceIsFriendly && name.equals("TRUCE"));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    public boolean isSafeZone(Location location) {
        if (!active || location == null) {
            return false;
        }
        try {
            Object fLocation = fLocationFromLocation.newInstance(location);
            Object faction = getFactionAt.invoke(board, fLocation);
            return faction != null && (Boolean) isSafeZone.invoke(faction);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }
}
