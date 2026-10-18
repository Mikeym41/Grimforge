package dev.grimforge.command;

import dev.grimforge.Modifier;
import dev.grimforge.Settings;
import dev.grimforge.ToolData;
import dev.grimforge.GrimforgePlugin;
import dev.grimforge.ToolService;
import dev.grimforge.ToolType;
import dev.grimforge.menu.ModifierMenu;
import dev.grimforge.rune.Rune;
import dev.grimforge.rune.RuneItems;
import dev.grimforge.rune.RuneType;
import dev.grimforge.rune.Tier;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * /tool            open the modifier menu
 * /tool info       show the held tool's stats in chat
 * /tool addxp <n>  (admin) add XP to the held tool
 * /tool reload     (admin) reload config.yml
 * /tool rune <type> [magic|rare|legendary] [player]  (admin) give a rune
 * /tool socket <type> [magic|rare|legendary]          (admin) socket a rune straight into the held weapon
 * /tool setmod <modifier> <level>                     (admin) set a modifier level without spending points
 */
public final class ToolCommand implements TabExecutor {

    private static final String ADMIN = "grimforge.admin";

    private final GrimforgePlugin plugin;

    public ToolCommand(GrimforgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String sub = args.length == 0 ? "menu" : args[0].toLowerCase();

        if (sub.equals("reload")) {
            if (!sender.hasPermission(ADMIN)) {
                return noPermission(sender);
            }
            plugin.reloadSettings();
            sender.sendMessage(Component.text("Grimforge config reloaded.", NamedTextColor.GREEN));
            return true;
        }

        if (sub.equals("rune")) {
            if (!sender.hasPermission(ADMIN)) {
                return noPermission(sender);
            }
            return giveRune(sender, label, args);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        ToolType type = ToolType.of(item);
        if (type == null) {
            player.sendMessage(Component.text("Hold a pickaxe, axe, shovel, hoe or sword.", NamedTextColor.RED));
            return true;
        }

        switch (sub) {
            case "menu", "mods", "upgrade" -> new ModifierMenu(plugin, player).open();
            case "info" -> sendInfo(player, item, type);
            case "setmod" -> {
                if (!player.hasPermission(ADMIN)) {
                    return noPermission(player);
                }
                Modifier modifier;
                int level;
                try {
                    modifier = Modifier.valueOf(args[1].toUpperCase());
                    level = Integer.parseInt(args[2]);
                } catch (RuntimeException e) {
                    player.sendMessage(Component.text("Usage: /" + label + " setmod <modifier> <level>", NamedTextColor.RED));
                    return true;
                }
                String error = plugin.tools().setModifier(player, modifier, level);
                player.sendMessage(error == null
                        ? Component.text("Set " + modifier.displayName() + " to level " + level + ".", NamedTextColor.GREEN)
                        : Component.text(error, NamedTextColor.RED));
            }
            case "socket" -> {
                if (!player.hasPermission(ADMIN)) {
                    return noPermission(player);
                }
                Rune rune = parseRune(args);
                if (rune == null) {
                    player.sendMessage(Component.text("Usage: /" + label + " socket <type> [magic|rare|legendary]", NamedTextColor.RED));
                    return true;
                }
                String error = plugin.tools().socketDirect(player, rune);
                player.sendMessage(error == null
                        ? Component.text("Socketed " + rune.tier().displayName() + " Rune of " + rune.type().displayName() + ".", NamedTextColor.GREEN)
                        : Component.text(error, NamedTextColor.RED));
            }
            case "addxp" -> {
                if (!player.hasPermission(ADMIN)) {
                    return noPermission(player);
                }
                int amount;
                try {
                    amount = Integer.parseInt(args.length > 1 ? args[1] : "");
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Usage: /" + label + " addxp <amount>", NamedTextColor.RED));
                    return true;
                }
                plugin.tools().addXp(player, item, amount);
            }
            default -> player.sendMessage(Component.text("Usage: /" + label + " [info|addxp <amount>|rune <type>|reload]", NamedTextColor.RED));
        }
        return true;
    }

    /** Parses args[1] (type) and optional args[2] (tier, default magic). */
    private static Rune parseRune(String[] args) {
        if (args.length < 2) {
            return null;
        }
        try {
            RuneType type = RuneType.valueOf(args[1].toUpperCase().replace('-', '_'));
            Tier tier = args.length > 2 ? Tier.valueOf(args[2].toUpperCase()) : Tier.MAGIC;
            return new Rune(type, tier);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean giveRune(CommandSender sender, String label, String[] args) {
        String usage = "Usage: /" + label + " rune <type> [magic|rare|legendary] [player]";
        if (args.length < 2) {
            sender.sendMessage(Component.text(usage, NamedTextColor.RED));
            return true;
        }
        RuneType type;
        Tier tier;
        try {
            type = RuneType.valueOf(args[1].toUpperCase().replace('-', '_'));
            tier = args.length > 2 ? Tier.valueOf(args[2].toUpperCase()) : Tier.MAGIC;
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text(usage, NamedTextColor.RED));
            return true;
        }
        Player target = args.length > 3 ? Bukkit.getPlayerExact(args[3]) : (sender instanceof Player p ? p : null);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }
        ItemStack rune = RuneItems.create(new Rune(type, tier), plugin.settings().runes);
        target.getInventory().addItem(rune).values().forEach(left -> target.getWorld().dropItem(target.getLocation(), left));
        sender.sendMessage(Component.text("Gave " + tier.displayName() + " Rune of " + type.displayName() + " to " + target.getName() + ".", NamedTextColor.GREEN));
        return true;
    }

    private void sendInfo(Player player, ItemStack item, ToolType type) {
        Settings s = plugin.settings();
        ToolData data = ToolData.read(item.getItemMeta());
        String progress = data.level >= s.maxLevel ? "MAX" : data.xp + "/" + s.xpToNext(data.level) + " XP";

        player.sendMessage(Component.text(type.displayName() + " · Level " + data.level + "/" + s.maxLevel + " · " + progress, NamedTextColor.GOLD));
        player.sendMessage(Component.text("Unspent points: " + data.points, NamedTextColor.AQUA));
        List<String> mods = new ArrayList<>();
        for (Modifier modifier : Modifier.values()) {
            int level = data.modifier(modifier);
            if (level > 0) {
                mods.add(modifier.displayName() + " " + ToolService.roman(level));
            }
        }
        player.sendMessage(Component.text("Modifiers: " + (mods.isEmpty() ? "none" : String.join(", ", mods)), NamedTextColor.GRAY));
        int sockets = plugin.tools().socketCount(type, data.level);
        if (sockets > 0) {
            List<String> runes = data.runes.stream().map(r -> r.tier().displayName() + " " + r.type().displayName()).toList();
            player.sendMessage(Component.text("Runes (" + data.runes.size() + "/" + sockets + "): "
                    + (runes.isEmpty() ? "none" : String.join(", ", runes)), NamedTextColor.LIGHT_PURPLE));
        }
    }

    private static boolean noPermission(CommandSender sender) {
        sender.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(List.of("info", "mods"));
            if (sender.hasPermission(ADMIN)) {
                options.addAll(List.of("addxp", "reload", "rune", "socket", "setmod"));
            }
        } else if (args[0].equalsIgnoreCase("setmod") && args.length == 2 && sender.hasPermission(ADMIN)) {
            for (Modifier modifier : Modifier.values()) {
                options.add(modifier.key());
            }
        } else if ((args[0].equalsIgnoreCase("rune") || args[0].equalsIgnoreCase("socket")) && sender.hasPermission(ADMIN)) {
            if (args.length == 2) {
                for (RuneType type : RuneType.values()) {
                    options.add(type.key());
                }
            } else if (args.length == 3) {
                for (Tier tier : Tier.values()) {
                    options.add(tier.name().toLowerCase());
                }
            } else if (args.length == 4 && args[0].equalsIgnoreCase("rune")) {
                Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
            }
        }
        String typed = args[args.length - 1].toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(typed)).toList();
    }
}
