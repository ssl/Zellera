package zel.game.players

class RecipeBook internal constructor(
    private val store: PlayerStatsStore,
) {
    private val ids = HashSet<Int>()

    fun knows(recipeId: Int): Boolean = synchronized(ids) { recipeId in ids }

    fun learn(recipeId: Int): Boolean {
        if (knows(recipeId)) return false
        store.addRecipe(recipeId)
        return synchronized(ids) { ids.add(recipeId) }
    }

    internal fun restore(recipeId: Int) {
        synchronized(ids) { ids.add(recipeId) }
    }
}
