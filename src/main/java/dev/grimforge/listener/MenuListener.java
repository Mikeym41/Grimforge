package dev.grimforge.listener;

import dev.grimforge.menu.ModifierMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Locks the modifier menu so items can't be taken out, and routes clicks to it. */
public final class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof ModifierMenu menu)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null) {
            return;
        }
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            menu.click(event.getRawSlot(), event.isShiftClick());
        } else if (event.getClickedInventory() == event.getView().getBottomInventory()) {
            menu.clickOwnInventory(event.getSlot());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof ModifierMenu) {
            event.setCancelled(true);
        }
    }
}
