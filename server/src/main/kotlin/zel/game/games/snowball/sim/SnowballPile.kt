@file:Suppress("MagicNumber") // variable indexes of the simulation

package zel.game.games.snowball.sim

/** 7 variables; circle and collision height are 100 per ball; holds its tile while it has balls, never refilled. */
class SnowballPile internal constructor(
    stage: SyncStage,
    data: GameObjectData,
) : SnowballGiver(
        data.id,
        data.variable(5),
        checkNotNull(stage.tile(ArenaStage.worldToTile(data.variable(2)), ArenaStage.worldToTile(data.variable(3)))) {
            "Pile ${data.id} stands on no tile"
        },
        data.variable(6),
    ) {
    val maxSnowballs: Int = data.variable(4)
    override var radius: Int = snowballs * RADIUS_PER_SNOWBALL
        private set

    init {
        if (snowballs > 0) {
            stage.addToTile(this)
        }
    }

    override val type: Int get() = TYPE_PILE

    override val varCount: Int get() = VAR_COUNT

    override fun variable(index: Int): Int =
        when (index) {
            0 -> TYPE_PILE
            1 -> id
            2 -> tile.worldX
            3 -> tile.worldY
            4 -> maxSnowballs
            5 -> snowballs
            6 -> fuseObjectId
            else -> throw IllegalArgumentException("No such variable: $index")
        }

    override fun afterTake() {
        radius = snowballs * RADIUS_PER_SNOWBALL
        if (snowballs <= 0) {
            tile.removeOccupant()
        }
    }

    companion object {
        const val VAR_COUNT = 7
        internal const val RADIUS_PER_SNOWBALL = 100
    }
}
