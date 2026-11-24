package zel.game.crafting

import zel.game.items.Item

class Recipe(
    val id: Int,
    val name: String,
    val product: Item,
    val hidden: Boolean,
    val achievement: String,
    val parts: Map<Item, Int>,
    val stock: RecipeStock = RecipeStock.Unlimited,
)
