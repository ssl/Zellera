package zel.game.crafting

import zel.db.write
import java.sql.Connection
import java.sql.SQLException

/** Taken from in the craft's own transaction. */
object RecipeStockStore {
    private const val TAKE = "UPDATE crafting_recipes SET stock_left = stock_left - 1 WHERE id = ? AND stock_left > 0"

    /** True for an unlimited recipe; false when none is left. */
    @Throws(SQLException::class)
    fun take(
        connection: Connection,
        recipe: Recipe,
    ): Boolean = recipe.stock !is RecipeStock.Limited || connection.write(TAKE) { int(recipe.id) } == 1
}
