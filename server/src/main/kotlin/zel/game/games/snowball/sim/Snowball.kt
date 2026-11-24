@file:Suppress("MagicNumber") // flight constants and variable indexes of the simulation

package zel.game.games.snowball.sim

class Snowball internal constructor(
    id: Int,
) : GameObject(id) {
    var x: Int = 0
        private set
    var y: Int = 0
        private set
    var z: Int = 0
        private set

    var direction: Int = 0
        private set
    var trajectory: Int = 0
        private set
    private var timeToLive = 0
    var thrower: Human? = null
        private set
    private var parabolaOffset = 0
    private var velocity = 0

    internal fun initialize(
        startX: Int,
        startY: Int,
        startZ: Int,
        trajectory: Int,
        targetX: Int,
        targetY: Int,
        thrower: Human?,
    ) {
        isActive = true
        x = startX
        y = startY
        z = startZ
        val dx = (targetX - startX) / 200
        val dy = (targetY - startY) / 200
        direction = Compass.normalize(Compass.headingOf(dx, dy))
        var distance = Distance.approxSqrt(dx * dx + dy * dy) * 200
        this.trajectory = resolveTrajectory(trajectory, distance)
        when (this.trajectory) {
            TRAJECTORY_QUICK -> {
                timeToLive = QUICK_RANGE / QUICK_VELOCITY
                velocity = QUICK_VELOCITY
            }

            TRAJECTORY_SHORT_LOB -> {
                distance = minOf(distance, SHORT_LOB_RANGE)
                timeToLive = (distance * SHORT_LOB_TTL_PER_UNIT).toInt()
                velocity = if (timeToLive == 0) 0 else distance / timeToLive
            }

            TRAJECTORY_LONG_LOB -> {
                distance = minOf(distance, LONG_LOB_RANGE)
                timeToLive = (distance * LONG_LOB_TTL_PER_UNIT).toInt()
                velocity = if (timeToLive == 0) 0 else distance / timeToLive
            }
        }
        parabolaOffset = timeToLive / 2
        this.thrower = thrower
    }

    private fun resolveTrajectory(
        requested: Int,
        distance: Int,
    ): Int =
        when {
            requested != TRAJECTORY_DEFAULT -> requested
            distance <= DEFAULT_QUICK_RANGE -> TRAJECTORY_QUICK
            distance <= SHORT_LOB_RANGE -> TRAJECTORY_SHORT_LOB
            else -> TRAJECTORY_LONG_LOB
        }

    internal fun restore(
        data: GameObjectData,
        thrower: Human?,
    ) {
        x = data.variable(2)
        y = data.variable(3)
        z = data.variable(4)
        direction = Compass.normalize(data.variable(5))
        trajectory = data.variable(6)
        velocity = data.variable(10)
        timeToLive = data.variable(7)
        this.thrower = thrower
        parabolaOffset = data.variable(9)
        isActive = true
    }

    override val type: Int get() = TYPE_SNOWBALL

    override val varCount: Int get() = VAR_COUNT

    override fun variable(index: Int): Int =
        when (index) {
            0 -> TYPE_SNOWBALL
            1 -> id
            2 -> x
            3 -> y
            4 -> z
            5 -> direction
            6 -> trajectory
            7 -> timeToLive
            8 -> thrower?.id ?: -1
            9 -> parabolaOffset
            10 -> velocity
            else -> throw IllegalArgumentException("No such variable: $index")
        }

    override val radius: Int get() = RADIUS

    override val locationX: Int get() = x

    override val locationY: Int get() = y

    /** One flight step, then collisions: own tile, forward neighbours, lowest-id human, ground; a hit deletes it. */
    override fun subturn(stage: SyncStage) {
        if (!isActive) {
            return
        }
        timeToLive--
        when (trajectory) {
            TRAJECTORY_QUICK -> move(GRAVITY_QUICK, true)
            TRAJECTORY_SHORT_LOB -> move(GRAVITY_SHORT_LOB, false)
            else -> move(GRAVITY_LONG_LOB, false)
        }
        val tile = stage.tile(ArenaStage.worldToTile(x), ArenaStage.worldToTile(y))
        if (testCollisions(stage, tile) || stage.groundHit(this)) {
            stage.markForDelete(this)
        }
    }

    private fun move(
        gravity: Int,
        capAtStart: Boolean,
    ) {
        val newX = x + Compass.stepX(direction) * velocity / 255
        val newY = y + Compass.stepY(direction) * velocity / 255
        val fromPeak = timeToLive - parabolaOffset
        var newZ = (parabolaOffset * parabolaOffset - fromPeak * fromPeak) * gravity + START_Z
        if (capAtStart) {
            newZ = minOf(newZ, START_Z)
        }
        x = newX
        y = newY
        z = newZ
    }

    private fun testCollisions(
        stage: SyncStage,
        tile: ArenaTile?,
    ): Boolean {
        if (tile != null && hitsAroundTile(stage, tile)) {
            return true
        }
        val target =
            stage
                .objects
                .filterIsInstance<Human>()
                .filter { it.isActive && it.testSnowballCollision(this) }
                .minByOrNull { it.id }
                ?: return false
        target.onSnowballHit(stage, this)
        return true
    }

    private fun hitsAroundTile(
        stage: SyncStage,
        tile: ArenaTile,
    ): Boolean {
        val forward = Compass.octantOf(direction)
        return testTile(stage, tile) ||
            testTile(stage, tile.neighbour(forward)) ||
            testTile(stage, tile.neighbour((forward - 1) and 7)) ||
            testTile(stage, tile.neighbour((forward + 1) and 7))
    }

    private fun testTile(
        stage: SyncStage,
        tile: ArenaTile?,
    ): Boolean {
        val occupant = tile?.occupant
        if (occupant != null && occupant.testSnowballCollision(this)) {
            occupant.onSnowballHit(stage, this)
            return true
        }
        return false
    }

    companion object {
        const val VAR_COUNT = 11

        const val TRAJECTORY_QUICK = 0
        const val TRAJECTORY_SHORT_LOB = 1
        const val TRAJECTORY_LONG_LOB = 2
        const val TRAJECTORY_DEFAULT = 3

        internal const val RADIUS = 400
        internal const val START_Z = 3000
        internal const val QUICK_VELOCITY = 2000
        internal const val QUICK_RANGE = 20_000
        internal const val SHORT_LOB_RANGE = 60_000
        internal const val LONG_LOB_RANGE = 100_000
        internal const val DEFAULT_QUICK_RANGE = 42_000
        internal const val GRAVITY_QUICK = 10
        internal const val GRAVITY_SHORT_LOB = 25
        internal const val GRAVITY_LONG_LOB = 50

        internal const val SHORT_LOB_TTL_PER_UNIT = 2.236 / 2 / QUICK_VELOCITY

        internal const val LONG_LOB_TTL_PER_UNIT = 2 / 1.414 / QUICK_VELOCITY
    }
}
