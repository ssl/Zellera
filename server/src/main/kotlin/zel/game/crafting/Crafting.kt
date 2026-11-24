package zel.game.crafting

import zel.db.rows
import zel.game.items.Item
import zel.game.items.Items
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.SQLException
import javax.sql.DataSource

class Crafting(
    private val dataSource: DataSource,
    private val items: Items,
) {
    @Volatile
    private var tables: Map<Item, CraftingTable> = emptyMap()

    init {
        reload()
    }

    fun reload() {
        dispose()
        try {
            tables = build(readIngredients())
        } catch (e: SQLException) {
            LOGGER.error("Reading the crafting recipes failed", e)
        }
    }

    fun recipe(name: String): Recipe? = tables.values.firstNotNullOfOrNull { it.recipeNamed(name) }

    fun table(furni: Item): CraftingTable? = tables[furni]

    fun dispose() {
        tables = emptyMap()
    }

    private fun readIngredients(): List<IngredientRow> =
        dataSource.connection.use { connection ->
            connection.rows(ENABLED_RECIPES, read = ::IngredientRow)
        }

    private fun build(rows: List<IngredientRow>): Map<Item, CraftingTable> {
        val byTable = HashMap<Item, MutableList<Recipe>>()
        rows.groupBy { it.recipeId }.values.forEach { recipeRows ->
            val table = items.getItem(recipeRows.first().altarDefinition) ?: return@forEach
            val recipes = byTable.getOrPut(table) { ArrayList() }
            buildRecipe(recipeRows)?.let(recipes::add)
        }
        return byTable.mapValues { (table, recipes) -> CraftingTable(table, recipes) }
    }

    /** A recipe with a part the game does not know is left out whole: short of a part it would be cheaper to make. */
    private fun buildRecipe(rows: List<IngredientRow>): Recipe? {
        val first = rows.first()
        val reward = items.getItem(first.rewardDefinition)
        val parts = rows.associate { items.getItem(it.ingredientDefinition) to it.amount }
        if (reward == null || null in parts || parts.values.any { it <= 0 }) {
            LOGGER.error("Crafting recipe {} names unknown furni or an empty amount; left out", first.recipeId)
            return null
        }
        return Recipe(
            first.recipeId,
            first.name,
            reward,
            first.secret,
            first.achievement,
            parts.mapKeys { (part, _) -> checkNotNull(part) },
            first.stockLeft?.let(RecipeStock::Limited) ?: RecipeStock.Unlimited,
        )
    }

    private class IngredientRow(
        row: ResultSet,
    ) {
        val altarDefinition = row.getInt("altar_definition_id")
        val recipeId = row.getInt("recipe_id")
        val name: String = row.getString("product_name")
        val rewardDefinition = row.getInt("reward_definition_id")
        val secret = row.getBoolean("secret")
        val achievement: String = row.getString("achievement_code").orEmpty()
        val stockLeft = row.getInt("stock_left").takeUnless { row.wasNull() }
        val ingredientDefinition = row.getInt("ingredient_definition_id")
        val amount = row.getInt("amount")
    }

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(Crafting::class.java)
        const val ENABLED_RECIPES =
            "SELECT r.id AS recipe_id, r.altar_definition_id, r.product_name, r.reward_definition_id, r.secret, " +
                "r.achievement_code, r.stock_left, i.definition_id AS ingredient_definition_id, i.amount " +
                "FROM crafting_recipes r INNER JOIN crafting_ingredients i ON i.recipe_id = r.id " +
                "WHERE r.enabled ORDER BY r.id"
    }
}
