package zel.game.games.snowball.sim

/** A stage's game objects in insertion order; a subturn steps a copy of the list, then removes the delete list. */
class SyncStage internal constructor(
    private val arena: SyncArena,
    val tiles: ArenaStage,
) {
    private val byId = LinkedHashMap<Int, GameObject>()
    private val deleteList = mutableListOf<GameObject>()

    internal var onStat: (StatEvent) -> Unit = {}

    fun tile(
        x: Int,
        y: Int,
    ): ArenaTile? = tiles.tile(x, y)

    internal val isDeathMatch: Boolean get() = arena.isDeathMatch

    internal fun addObject(obj: GameObject) {
        byId.putIfAbsent(obj.id, obj)
        obj.isActive = true
    }

    fun objectOrNull(id: Int): GameObject? = byId[id]

    val objects: List<GameObject> get() = byId.values.toList()

    internal fun markForDelete(obj: GameObject) {
        deleteList.add(obj)
        obj.isActive = false
    }

    private fun remove(id: Int) {
        byId.remove(id)?.onRemove()
    }

    internal fun addToTile(obj: GameObject) {
        tile(ArenaStage.worldToTile(obj.locationX), ArenaStage.worldToTile(obj.locationY))?.addGameObject(obj)
    }

    /** Rebuilds the objects in order with construction side effects; a snowball's thrower must come before it. */
    fun createObjects(list: List<GameObjectData>) {
        objects.forEach { it.onRemove() }
        byId.clear()
        for (data in list) {
            when (data.type) {
                GameObject.TYPE_HUMAN -> {
                    addObject(Human(this, data))
                }

                GameObject.TYPE_SNOWBALL -> {
                    val ball = Snowball(data.id)
                    ball.restore(data, byId[data.variable(8)] as? Human)
                    addObject(ball)
                }

                GameObject.TYPE_MACHINE -> {
                    addObject(SnowballMachine(this, data))
                }

                GameObject.TYPE_PILE -> {
                    addObject(SnowballPile(this, data))
                }

                GameObject.TYPE_TREE -> {
                    addObject(Tree(this, data))
                }

                else -> {
                }
            }
        }
    }

    internal fun resetTiles() = tiles.resetTiles()

    internal fun subturn() {
        objects.forEach { it.subturn(this) }
        if (deleteList.isNotEmpty()) {
            val deleted = deleteList.toList()
            deleteList.clear()
            deleted.forEach { remove(it.id) }
        }
    }

    internal fun groundHit(ball: Snowball): Boolean {
        if (ball.z < 1) {
            return true
        }
        val tile = tile(ArenaStage.worldToTile(ball.x), ArenaStage.worldToTile(ball.y))
        return tile != null && ball.z < tile.height
    }

    fun checksum(turn: Int): Long =
        ChecksumSeed.next(turn).toLong() +
            byId.values.filter { it.isActive }.sumOf { obj ->
                (0 until obj.varCount).sumOf { i -> obj.variable(i).toLong() * (i + 1) }
            }

    /** False while a departed human's ball flies: the rebuilt ball would lose its thrower and desync checksums. */
    fun snapshotRebuildsExactly(): Boolean =
        byId.values.none { obj ->
            obj is Snowball && obj.isActive && obj.thrower?.let { byId[it.id] !== it } == true
        }

    fun snapshot(): List<GameObjectData> = byId.values.filter { it.isActive }.map { it.data() }
}
