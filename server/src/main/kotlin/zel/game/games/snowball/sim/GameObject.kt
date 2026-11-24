package zel.game.games.snowball.sim

abstract class GameObject internal constructor(
    val id: Int,
) {
    var isActive: Boolean = false
        internal set

    abstract val type: Int

    abstract val varCount: Int

    abstract fun variable(index: Int): Int

    fun vars(): IntArray = IntArray(varCount) { variable(it) }

    open fun data(): GameObjectData = GameObjectData(vars(), emptyList())

    internal open fun subturn(stage: SyncStage) {}

    internal abstract val radius: Int

    /** A snowball must be below this to hit (the radius unless overridden). */
    internal open val collisionHeight: Int get() = radius

    internal abstract val locationX: Int

    internal abstract val locationY: Int

    internal open fun testSnowballCollision(ball: Snowball): Boolean = ball.z < collisionHeight && circleHit(ball)

    internal fun circleHit(ball: Snowball): Boolean =
        ball !== this && Distance.closerThan(locationX, locationY, ball.x, ball.y, radius + ball.radius)

    internal open fun onSnowballHit(
        stage: SyncStage,
        ball: Snowball,
    ) {}

    internal open fun onRemove() {}

    companion object {
        const val TYPE_SNOWBALL = 1
        const val TYPE_TREE = 2
        const val TYPE_PILE = 3
        const val TYPE_MACHINE = 4
        const val TYPE_HUMAN = 5
    }
}
