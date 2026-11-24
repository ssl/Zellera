package zel.game.games.snowball.sim

sealed class SnowballGiver(
    id: Int,
    snowballs: Int,
    val tile: ArenaTile,
    val fuseObjectId: Int,
) : GameObject(id) {
    var snowballs: Int = snowballs
        protected set

    init {
        isActive = true
    }

    override val locationX: Int get() = tile.worldX

    override val locationY: Int get() = tile.worldY

    internal fun takeSnowballs(wanted: Int): Int {
        val count = minOf(wanted, snowballs)
        snowballs -= count
        afterTake()
        return count
    }

    protected open fun afterTake() {}
}
