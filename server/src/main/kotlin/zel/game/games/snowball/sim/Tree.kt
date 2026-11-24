@file:Suppress("MagicNumber") // variable indexes of the simulation

package zel.game.games.snowball.sim

/** 9 variables; catches balls in a 2799 circle below its height until `maxHits`, then stops colliding. */
class Tree internal constructor(
    stage: SyncStage,
    data: GameObjectData,
) : GameObject(data.id) {
    private val tile: ArenaTile =
        checkNotNull(stage.tile(ArenaStage.worldToTile(data.variable(2)), ArenaStage.worldToTile(data.variable(3)))) {
            "Tree ${data.id} stands on no tile"
        }
    private val direction8: Int = data.variable(4)
    private val fuseObjectId: Int = data.variable(6)
    private val height: Int = data.variable(5)
    val maxHits: Int = data.variable(7)
    var hits: Int = data.variable(8)
        private set

    init {
        isActive = true
        check(direction8 in 0..7) { "Tree ${data.id} has no Direction8 $direction8" }
        if (hits < maxHits) {
            stage.addToTile(this)
        }
        tile.addHeight(-height)
        tile.block()
    }

    override val type: Int get() = TYPE_TREE

    override val varCount: Int get() = VAR_COUNT

    override fun variable(index: Int): Int =
        when (index) {
            0 -> TYPE_TREE
            1 -> id
            2 -> tile.worldX
            3 -> tile.worldY
            4 -> direction8
            5 -> height
            6 -> fuseObjectId
            7 -> maxHits
            8 -> hits
            else -> throw IllegalArgumentException("No such variable: $index")
        }

    override val radius: Int get() = if (hits < maxHits) RADIUS else 0

    override val collisionHeight: Int get() = height

    override val locationX: Int get() = tile.worldX

    override val locationY: Int get() = tile.worldY

    override fun onSnowballHit(
        stage: SyncStage,
        ball: Snowball,
    ) {
        if (hits < maxHits) {
            hits++
            ball.thrower?.let { stage.onStat(StatEvent.TreeHit(it, this)) }
        }
        if (hits >= maxHits) {
            tile.removeOccupant()
        }
    }

    companion object {
        const val VAR_COUNT = 9

        internal const val RADIUS = ArenaTile.WORLD_SIZE - Snowball.RADIUS - 1
    }
}
