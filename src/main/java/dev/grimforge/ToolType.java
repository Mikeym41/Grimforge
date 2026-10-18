package dev.grimforge;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum ToolType {
    PICKAXE("_PICKAXE", "Pickaxe"),
    AXE("_AXE", "Axe"),
    SHOVEL("_SHOVEL", "Shovel"),
    HOE("_HOE", "Hoe"),
    SWORD("_SWORD", "Sword");

    private final String suffix;
    private final String displayName;

    ToolType(String suffix, String displayName) {
        this.suffix = suffix;
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isMiningTool() {
        return this != SWORD;
    }

    public static ToolType of(Material material) {
        if (material == null) {
            return null;
        }
        String name = material.name();
        for (ToolType type : values()) {
            if (name.endsWith(type.suffix)) {
                return type;
            }
        }
        return null;
    }

    public static ToolType of(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        return of(item.getType());
    }
}
