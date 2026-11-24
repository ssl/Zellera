@file:Suppress("MagicNumber") // Direction8 has eight values

package zel.game.games.snowball.sim

import zel.game.games.snowball.arena.ArenaDefinition.FuseObject

class ArenaTile internal constructor(
    val x: Int,
    val y: Int,
) {
    private val fuseList = mutableListOf<FuseObject>()
    private val neighbours = arrayOfNulls<ArenaTile>(8)

    var height: Int = 0
        private set

    var blocked: Boolean = false
        private set

    var occupant: GameObject? = null
        private set

    val worldX: Int get() = x * WORLD_SIZE

    val worldY: Int get() = y * WORLD_SIZE

    val fuseObjects: List<FuseObject> get() = fuseList

    fun neighbour(direction8: Int): ArenaTile? = neighbours[direction8 and 7]

    internal fun addFuseObject(fuse: FuseObject) {
        fuseList.add(fuse)
        addHeight(fuse.height)
    }

    internal fun addHeight(value: Int) {
        height = maxOf(0, height + value)
    }

    internal fun block() {
        blocked = true
    }

    internal fun link(
        other: ArenaTile,
        direction8: Int,
    ) {
        neighbours[direction8 and 7] = other
        other.neighbours[(direction8 + 4) and 7] = this
    }

    fun walkable(): Boolean {
        val fuseBlocks = if (fuseList.size == 1) !fuseList[0].canStandOn else fuseList.size > 1
        return !fuseBlocks && !blocked
    }

    fun canMoveTo(): Boolean = walkable() && occupant == null

    internal fun addGameObject(obj: GameObject): Boolean {
        if (occupant != null) {
            return false
        }
        occupant = obj
        return true
    }

    internal fun removeOccupant(): GameObject? {
        val removed = occupant
        occupant = null
        return removed
    }

    internal fun occupyingHuman(): Human? = occupant as? Human

    internal fun removeOccupyingHuman(): Human? {
        val human = occupyingHuman()
        if (human != null) {
            occupant = null
        }
        return human
    }

    internal fun contains(
        worldX: Int,
        worldY: Int,
    ): Boolean =
        Math.abs(this.worldX - worldX) < WORLD_SIZE / 2 &&
            Math.abs(this.worldY - worldY) < WORLD_SIZE / 2

    internal fun reset() {
        occupant = null
    }

    companion object {
        const val WORLD_SIZE = 3200
    }
}
