package zel.game.crafting

import zel.game.items.Item
import zel.game.players.Player

class CraftingTable(
    val furni: Item,
    val recipes: Collection<Recipe>,
) {
    val materials: Set<Item> = recipes.flatMapTo(HashSet()) { it.parts.keys }

    fun recipeNamed(name: String): Recipe? = recipes.firstOrNull { it.name == name }

    fun recipeMadeOf(given: Map<Item, Int>): Recipe? = recipes.firstOrNull { it.parts == given }

    fun recipesWithin(given: Map<Item, Int>): Map<Recipe, Boolean> =
        recipes
            .filter { recipe ->
                recipe.stock.available && given.all { (furni, amount) -> (recipe.parts[furni] ?: 0) >= amount }
            }.associateWithTo(HashMap()) { it.parts == given }

    fun recipesShownTo(player: Player): List<Recipe> =
        recipes.filter { !it.hidden || player.stats.recipes.knows(it.id) }
}
