package dev.grimforge;

import dev.grimforge.command.ToolCommand;
import dev.grimforge.hook.FactionsHook;
import dev.grimforge.listener.MenuListener;
import dev.grimforge.listener.ModifierListener;
import dev.grimforge.listener.PlacedBlockTracker;
import dev.grimforge.listener.XpListener;
import dev.grimforge.rune.RuneDropListener;
import dev.grimforge.rune.RuneListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class GrimforgePlugin extends JavaPlugin {

    private Settings settings;
    private ToolService tools;
    private PlacedBlockTracker placedBlocks;
    private SmeltingLookup smelting;
    private FactionsHook factions;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Keys.init(this);

        settings = new Settings(getConfig(), getLogger());
        factions = new FactionsHook(settings.factionsEnabled, settings.factionsTruceFriendly, getLogger());
        placedBlocks = new PlacedBlockTracker();
        smelting = new SmeltingLookup();
        tools = new ToolService(this);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new XpListener(this), this);
        pm.registerEvents(new ModifierListener(this), this);
        pm.registerEvents(new MenuListener(), this);
        pm.registerEvents(placedBlocks, this);
        pm.registerEvents(new RuneListener(this), this);
        pm.registerEvents(new RuneDropListener(this), this);

        PluginCommand command = getCommand("tool");
        if (command != null) {
            ToolCommand executor = new ToolCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    public void reloadSettings() {
        reloadConfig();
        settings = new Settings(getConfig(), getLogger());
        factions = new FactionsHook(settings.factionsEnabled, settings.factionsTruceFriendly, getLogger());
    }

    public Settings settings() {
        return settings;
    }

    public ToolService tools() {
        return tools;
    }

    public PlacedBlockTracker placedBlocks() {
        return placedBlocks;
    }

    public FactionsHook factions() {
        return factions;
    }

    public SmeltingLookup smelting() {
        return smelting;
    }
}
