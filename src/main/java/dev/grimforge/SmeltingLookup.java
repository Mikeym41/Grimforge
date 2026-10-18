package dev.grimforge;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

/** Maps an input material to its furnace result, built from the server's recipes on first use. */
public final class SmeltingLookup {

    private Map<Material, ItemStack> results;

    public ItemStack result(Material input) {
        if (results == null) {
            build();
        }
        ItemStack result = results.get(input);
        return result == null ? null : result.clone();
    }

    private void build() {
        results = new EnumMap<>(Material.class);
        Iterator<Recipe> recipes = Bukkit.recipeIterator();
        while (recipes.hasNext()) {
            Recipe recipe = recipes.next();
            if (recipe instanceof FurnaceRecipe furnace
                    && furnace.getInputChoice() instanceof RecipeChoice.MaterialChoice choice) {
                for (Material material : choice.getChoices()) {
                    results.putIfAbsent(material, furnace.getResult());
                }
            }
        }
    }
}
