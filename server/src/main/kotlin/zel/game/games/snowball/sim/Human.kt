@file:Suppress("MagicNumber") // timers, speeds and variable indexes of the simulation

package zel.game.games.snowball.sim

import kotlin.math.abs

class Human internal constructor(
    stage: SyncStage,
    data: GameObjectData,
) : GameObject(data.id) {
    var x: Int = data.variable(2)
        private set
    var y: Int = data.variable(3)
        private set
    private var targetX = data.variable(14)
    private var targetY = data.variable(15)
    var bodyDirection: Int = data.variable(6).let { if (it < 0 || it > 7) Compass.SOUTH_EAST else it }
        private set
    var hitPoints: Int = data.variable(7)
        private set
    var snowballs: Int = data.variable(8)
        private set
    private val isBot = data.variable(9)
    private var activityTimer = data.variable(10)
    var activityState: Int = data.variable(11)
        private set
    var score: Int = data.variable(16)
        private set
    val team: Int = data.variable(17)
    val userId: Int = data.variable(18)

    /** Not a variable; a full status resets it to 0 and it only gates throws. */
    var throwTimer: Int = 0
        private set

    val name: String = data.strings.getOrElse(0) { "" }
    private val motto = data.strings.getOrElse(1) { "" }
    private val figure = data.strings.getOrElse(2) { "" }
    private val sex = data.strings.getOrElse(3) { "" }

    var currentTile: ArenaTile =
        checkNotNull(stage.tile(data.variable(4), data.variable(5))) { "Human ${data.id} stands on no tile" }
        private set

    var nextTile: ArenaTile? = null
        private set

    init {
        currentTile.addGameObject(this)
        val next = stage.tile(data.variable(12), data.variable(13))
        if (next !== currentTile) {
            nextTile = next
            next?.addGameObject(this)
            currentTile.removeOccupyingHuman()
        }
    }

    override val type: Int get() = TYPE_HUMAN

    override val varCount: Int get() = VAR_COUNT

    override fun variable(index: Int): Int =
        when (index) {
            0 -> TYPE_HUMAN
            1 -> id
            2 -> x
            3 -> y
            4 -> currentTile.x
            5 -> currentTile.y
            6 -> bodyDirection
            7 -> hitPoints
            8 -> snowballs
            9 -> isBot
            10 -> activityTimer
            11 -> activityState
            12 -> (nextTile ?: currentTile).x
            13 -> (nextTile ?: currentTile).y
            14 -> targetX
            15 -> targetY
            16 -> score
            17 -> team
            18 -> userId
            else -> throw IllegalArgumentException("No such variable: $index")
        }

    override fun data(): GameObjectData = GameObjectData(vars(), listOf(name, motto, figure, sex))

    override val radius: Int get() = RADIUS

    override val collisionHeight: Int get() = COLLISION_HEIGHT

    override val locationX: Int get() = x

    override val locationY: Int get() = y

    /** Advances one subturn: activity timer (fires at exactly 1), throw timer, then walking in states 0 and 3. */
    override fun subturn(stage: SyncStage) {
        if (activityTimer > 0) {
            if (activityTimer == 1) {
                activityTimerTriggered(stage)
            }
            activityTimer--
        }
        if (throwTimer > 0) {
            throwTimer--
        }
        if (!canMove()) {
            return
        }
        if (nextTile != null) {
            moveTowardsNextTile()
            return
        }
        if (currentTile.contains(targetX, targetY)) {
            return
        }
        pickNextTile()
    }

    private fun pickNextTile() {
        val angle = Compass.headingOf(targetX - currentTile.worldX, targetY - currentTile.worldY)
        var direction = Compass.octantOf(angle)
        var next = currentTile.neighbour(direction)
        if (next == null || !next.canMoveTo()) {
            if (next != null && targetX == next.worldX && targetY == next.worldY) {
                nextTile = null
                stopMovement()
                return
            }
            direction = (direction - 1) and 7
            next = currentTile.neighbour(direction)
            if (next == null || !next.canMoveTo()) {
                direction = (direction + 2) and 7
                next = currentTile.neighbour(direction)
                if (next != null && !next.canMoveTo()) {
                    next = null
                }
            }
        }
        nextTile = next
        if (next != null) {
            currentTile.removeOccupyingHuman()
            next.addGameObject(this)
            bodyDirection = direction
            moveTowardsNextTile()
        }
    }

    private fun activityTimerTriggered(stage: SyncStage) {
        if (activityState == STATE_STUNNED) {
            hitPoints = MAX_HIT_POINTS
            activityState = STATE_INVINCIBLE
            activityTimer = INVINCIBLE_TIME
            return
        }
        if (activityState == STATE_MAKING_SNOWBALL) {
            snowballs++
            stage.onStat(StatEvent.Made(this))
        }
        activityState = STATE_NORMAL
    }

    private fun moveTowardsNextTile() {
        val next = checkNotNull(nextTile) { "no next tile" }
        val nextX = next.worldX
        val nextY = next.worldY
        x = step(x, nextX)
        y = step(y, nextY)
        if (abs(nextX - x) + abs(nextY - y) < WALK_SPEED / 2) {
            currentTile = next
            nextTile = null
        }
    }

    private fun step(
        from: Int,
        to: Int,
    ): Int {
        val d = from - to
        return when {
            d == 0 -> from
            d < 0 -> if (d > -WALK_SPEED) to else from + WALK_SPEED
            else -> if (d < WALK_SPEED) to else from - WALK_SPEED
        }
    }

    private fun stopMovement() {
        val next = nextTile
        if (next == null) {
            targetX = currentTile.worldX
            targetY = currentTile.worldY
            x = currentTile.worldX
            y = currentTile.worldY
        } else {
            currentTile = next
            x = next.worldX
            y = next.worldY
            targetX = next.worldX
            targetY = next.worldY
            nextTile = null
        }
    }

    internal fun changeMoveTarget(
        worldX: Int,
        worldY: Int,
    ) {
        if (activityState == STATE_MAKING_SNOWBALL) {
            activityState = STATE_NORMAL
            activityTimer = 0
        }
        if (activityState == STATE_NORMAL || activityState == STATE_INVINCIBLE) {
            targetX = worldX
            targetY = worldY
        }
    }

    internal fun throwSnowball(
        worldX: Int,
        worldY: Int,
    ): Boolean {
        if (snowballs < 1) {
            return false
        }
        stopMovement()
        val angle = Compass.headingOf(worldX - x, worldY - y)
        bodyDirection = Compass.octantOf(angle)
        snowballs--
        return true
    }

    internal fun startThrowTimer() {
        throwTimer = THROW_INTERVAL
    }

    internal fun startMakingSnowball() {
        if (canMakeSnowballs()) {
            activityState = STATE_MAKING_SNOWBALL
            activityTimer = MAKE_SNOWBALL_TIME
            stopMovement()
        }
    }

    internal fun snowballCapacity(): Int = MAX_SNOWBALLS - snowballs

    internal fun addSnowballs(count: Int) {
        snowballs += count
    }

    override fun testSnowballCollision(ball: Snowball): Boolean =
        activityState != STATE_STUNNED &&
            activityState != STATE_INVINCIBLE &&
            ball.thrower !== this &&
            super.testSnowballCollision(ball)

    override fun onSnowballHit(
        stage: SyncStage,
        ball: Snowball,
    ) {
        val thrower = ball.thrower ?: return
        val knockdown = playerIsHitBySnowball(stage, thrower, ball.direction)
        thrower.onHitHuman(stage, this)
        if (isEnemy(stage, thrower, this)) {
            stage.onStat(StatEvent.Hit(thrower, this, knockdown))
        } else {
            stage.onStat(StatEvent.FriendlyHit(thrower, this))
        }
    }

    /** Returns whether the hit knocked him down; a team mate's ball does nothing in the team game but is used up. */
    private fun playerIsHitBySnowball(
        stage: SyncStage,
        thrower: Human,
        ballDirection: Int,
    ): Boolean {
        if (!isEnemy(stage, thrower, this)) {
            return false
        }
        var knockdown = false
        if (hitPoints > 0) {
            if (hitPoints == 1) {
                knockdown = true
                playerFallsDown(ballDirection)
                thrower.onKnockDownHuman(stage, this)
            }
            hitPoints--
        }
        return knockdown
    }

    private fun onHitHuman(
        stage: SyncStage,
        target: Human,
    ) {
        if (isEnemy(stage, this, target)) {
            score += SCORE_HIT
        }
    }

    private fun onKnockDownHuman(
        stage: SyncStage,
        target: Human,
    ) {
        if (isEnemy(stage, this, target)) {
            score += SCORE_KNOCKDOWN
        }
    }

    private fun playerFallsDown(ballDirection: Int) {
        activityState = STATE_STUNNED
        activityTimer = STUN_TIME
        bodyDirection = (Compass.octantOf(ballDirection) + 4) and 7
        stopMovement()
    }

    override fun onRemove() {
        if (currentTile.occupyingHuman() === this) {
            currentTile.removeOccupyingHuman()
        }
        nextTile?.let {
            if (it.occupyingHuman() === this) {
                it.removeOccupyingHuman()
            }
        }
    }

    fun canMove(): Boolean = activityState == STATE_NORMAL || activityState == STATE_INVINCIBLE

    fun canThrowSnowballs(): Boolean = snowballs > 0 && throwTimer < 1 && canMove()

    fun canMakeSnowballs(): Boolean = canMove() && snowballs < MAX_SNOWBALLS

    val isStandingStill: Boolean get() = nextTile == null && currentTile.contains(targetX, targetY)

    companion object {
        const val VAR_COUNT = 19
        const val MAX_SNOWBALLS = 5
        const val MAX_HIT_POINTS = 5

        const val STATE_NORMAL = 0
        const val STATE_MAKING_SNOWBALL = 1
        const val STATE_STUNNED = 2
        const val STATE_INVINCIBLE = 3

        internal const val WALK_SPEED = 534
        internal const val MAKE_SNOWBALL_TIME = 20
        internal const val STUN_TIME = 100
        internal const val INVINCIBLE_TIME = 60
        internal const val THROW_INTERVAL = 5
        internal const val RADIUS = 1600
        internal const val COLLISION_HEIGHT = 5000
        internal const val SCORE_HIT = 1
        internal const val SCORE_KNOCKDOWN = 5

        internal fun isEnemy(
            stage: SyncStage,
            thrower: Human,
            target: Human,
        ): Boolean = thrower.team != target.team || stage.isDeathMatch
    }
}
