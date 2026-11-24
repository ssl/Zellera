package zel.wired.effect.bot

import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.behaviour.state.StateHooks
import zel.room.map.Point
import zel.room.tick.TickPart
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection
import zel.room.unit.bot.RoomBot
import zel.room.unit.bot.RoomBots
import zel.room.unit.path.PathResult
import zel.room.unit.walk.Walking
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

class BotErrands(
    private val bots: RoomBots,
    private val walking: Walking,
    private val hooks: StateHooks,
    private val random: Random = Random.Default,
) : RoomPart {
    private val rooms = ConcurrentHashMap<Room, MutableMap<Int, Errands>>()

    val tick: TickPart =
        TickPart { scope ->
            rooms[scope.room]?.entries?.toList()?.forEach { (index, errands) -> run(scope, index, errands) }
        }

    override fun load(room: Room) {
        rooms[room] = HashMap()
    }

    override fun unload(scope: RoomScope) {
        rooms.remove(scope.room)
    }

    fun follow(
        scope: RoomScope,
        bot: RoomBot,
        user: RoomUnit,
    ) {
        val errands = errandsOf(scope, bot)
        errands.following = user.index
        val unit = scope.room.units[bot.unitIndex]
        if (unit == null || !next(unit.tile, user.tile)) errands.reached = null
        bot.following = user.index
        chase(scope, bot, user, errands)
    }

    fun stopFollowing(
        scope: RoomScope,
        bot: RoomBot,
    ) {
        errandsOf(scope, bot).following = null
        bot.following = null
    }

    fun serve(
        scope: RoomScope,
        bot: RoomBot,
        botUnit: RoomUnit,
        user: RoomUnit,
        item: Int,
        then: (RoomScope) -> Unit = {},
    ) {
        if (item > 0) hooks.holdItem(scope, botUnit, item)
        errandsOf(scope, bot).queue(scope.room, Serving(user.index, item, then = then))
        bot.following = user.index
        walkNextTo(scope, botUnit, user)
    }

    /** The bot shows the item in its hand again, without an errand. */
    fun show(
        scope: RoomScope,
        botUnit: RoomUnit,
        item: Int,
    ) {
        if (item > 0) hooks.holdItem(scope, botUnit, item)
    }

    /** Puts the item straight in the user's hand, no bot involved; item 0 empties it. */
    fun give(
        scope: RoomScope,
        user: RoomUnit,
        item: Int,
    ) = hooks.holdItem(scope, user, item)

    @Suppress("LongParameterList") // who serves whom, what, when and what comes after
    fun serveNear(
        scope: RoomScope,
        bot: RoomBot,
        botUnit: RoomUnit,
        user: RoomUnit,
        item: Int,
        delayMillis: Long,
        then: (RoomScope) -> Unit,
    ) {
        hooks.holdItem(scope, botUnit, item)
        errandsOf(scope, bot).queue(scope.room, Serving(user.index, item, scope.now() + delayMillis, then))
    }

    private fun run(
        scope: RoomScope,
        index: Int,
        errands: Errands,
    ) {
        val bot = bots.byIndex(scope, index)
        val unit = scope.room.units[index]
        if (bot == null || unit == null) {
            rooms[scope.room]?.remove(index)
            return
        }
        if (!serve(scope, bot, unit, errands)) follow(scope, bot, unit, errands)
    }

    private fun serve(
        scope: RoomScope,
        bot: RoomBot,
        unit: RoomUnit,
        errands: Errands,
    ): Boolean {
        val serving = errands.serving.firstOrNull() ?: return false
        val user = scope.room.units[serving.userIndex]
        when {
            user == null -> {
                errands.serving.removeFirst()
                if (serving.item > 0) hooks.holdItem(scope, unit, 0)
            }

            serving.handAt != null -> {
                if (scope.now() >= serving.handAt) {
                    errands.serving.removeFirst()
                    hand(scope, unit, user, serving)
                }
            }

            next(unit.tile, user.tile) -> {
                errands.serving.removeFirst()
                hand(scope, unit, user, serving)
                scope.raise(RoomEvent.BotReachedUnit(unit.index, user.index))
            }

            !unit.isWalking -> {
                walkNextTo(scope, unit, user)
            }
        }
        if (errands.serving.isEmpty()) bot.following = errands.following
        return errands.serving.isNotEmpty()
    }

    private fun hand(
        scope: RoomScope,
        unit: RoomUnit,
        user: RoomUnit,
        serving: Serving,
    ) {
        hooks.holdItem(scope, user, serving.item)
        if (serving.item > 0) hooks.holdItem(scope, unit, 0)
        serving.then(scope)
    }

    private fun follow(
        scope: RoomScope,
        bot: RoomBot,
        unit: RoomUnit,
        errands: Errands,
    ) {
        val index = errands.following ?: return
        val user = scope.room.units[index]
        if (user == null) {
            stopFollowing(scope, bot)
            return
        }
        errands.stillTicks = if (unit.isWalking) 0 else errands.stillTicks + 1
        val near = next(unit.tile, user.tile)
        if (near && errands.stillTicks >= REACHED_TICKS && errands.reached != user.index) {
            scope.raise(RoomEvent.BotReachedUnit(unit.index, user.index))
        }
        when {
            !near -> errands.reached = null
            errands.stillTicks >= REACHED_TICKS -> errands.reached = user.index
        }
        chase(scope, bot, user, errands)
    }

    /**
     * A follower heads for a free tile next to its user, picked at random, also for a user who stands still, and
     * stops there. It plans no new walk while it walks; after the step onto its pick it waits [REPLAN_TICKS] ticks
     * before it plans again, and reached fires on the [REACHED_TICKS]th. A walk that was refused or ended short (a
     * freeze, a teleport's line or landing, a blocked step) is planned again on the next tick.
     */
    private fun chase(
        scope: RoomScope,
        bot: RoomBot,
        user: RoomUnit,
        errands: Errands,
    ) {
        val unit = scope.room.units[bot.unitIndex] ?: return
        if (unit.isWalking || next(unit.tile, user.tile)) return
        if (walking.teleportLinePending(scope, unit)) return
        if (unit.tile == errands.goal && errands.stillTicks < REPLAN_TICKS) return
        errands.goal = walkNextTo(scope, unit, user, around(user).shuffled(random))
        if (errands.goal != null) errands.stillTicks = 0
    }

    private fun walkNextTo(
        scope: RoomScope,
        unit: RoomUnit,
        user: RoomUnit,
        tiles: List<Point> =
            around(user).sortedBy {
                (it.x - unit.tile.x).let { x -> x * x } +
                    (it.y - unit.tile.y).let { y -> y * y }
            },
    ): Point? {
        if (next(unit.tile, user.tile)) return null
        return tiles.firstOrNull { walking.walkTo(scope, unit, it) is PathResult.Found }
    }

    private fun around(user: RoomUnit): List<Point> =
        (0 until UnitDirection.COUNT).map { Point(user.tile.x + DX[it], user.tile.y + DY[it]) }

    private fun next(
        a: Point,
        b: Point,
    ): Boolean = max(abs(a.x - b.x), abs(a.y - b.y)) <= 1

    private fun errandsOf(
        scope: RoomScope,
        bot: RoomBot,
    ): Errands = rooms.getOrPut(scope.room) { HashMap() }.getOrPut(bot.unitIndex) { Errands() }

    private class Errands {
        var following: Int? = null

        /** The user the bot stands next to since it got there: the reached trigger fires once per arrival. */
        var reached: Int? = null

        /** The tile next to the user the follower's walk was planned to. */
        var goal: Point? = null

        /** Ticks the bot has stood still. */
        var stillTicks = 0
        val serving = ArrayDeque<Serving>()

        /** At most as many wait as there are players in the room. */
        fun queue(
            room: Room,
            next: Serving,
        ) {
            if (serving.size < room.units.playerIds().size) serving += next
        }
    }

    private data class Serving(
        val userIndex: Int,
        val item: Int,
        val handAt: Long? = null,
        val then: (RoomScope) -> Unit = {},
    )

    private companion object {
        const val REPLAN_TICKS = 3
        const val REACHED_TICKS = 2
        val DX = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
        val DY = intArrayOf(-1, -1, 0, 1, 1, 1, 0, -1)
    }
}
