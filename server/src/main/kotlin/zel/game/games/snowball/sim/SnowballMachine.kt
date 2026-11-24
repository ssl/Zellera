@file:Suppress("MagicNumber") // variable indexes of the simulation

package zel.game.games.snowball.sim

class SnowballMachine internal constructor(
    stage: SyncStage,
    data: GameObjectData,
) : SnowballGiver(
        data.id,
        data.variable(6),
        checkNotNull(stage.tile(ArenaStage.worldToTile(data.variable(2)), ArenaStage.worldToTile(data.variable(3)))) {
            "Machine ${data.id} stands on no tile"
        },
        data.variable(7),
    ) {
    val maxSnowballs: Int = data.variable(5)
    val direction8: Int = data.variable(4)

    init {
        check(direction8 in 0..7) { "Machine ${data.id} has no Direction8 $direction8" }
        stage.addToTile(this)
    }

    override val type: Int get() = TYPE_MACHINE

    override val varCount: Int get() = VAR_COUNT

    override fun variable(index: Int): Int =
        when (index) {
            0 -> TYPE_MACHINE
            1 -> id
            2 -> tile.worldX
            3 -> tile.worldY
            4 -> direction8
            5 -> maxSnowballs
            6 -> snowballs
            7 -> fuseObjectId
            else -> throw IllegalArgumentException("No such variable: $index")
        }

    override val radius: Int get() = RADIUS

    internal fun createSnowball() {
        if (snowballs < maxSnowballs) {
            snowballs++
        }
    }

    companion object {
        const val VAR_COUNT = 8
        internal const val RADIUS = 1200
    }
}
