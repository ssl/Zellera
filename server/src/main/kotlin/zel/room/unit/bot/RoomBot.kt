package zel.room.unit.bot

import zel.game.bots.Bot
import zel.room.map.Point

/**
 * A bot in a room. [start] with [spotZ] and [spotDirection] is its spot: where it was loaded or a user placed or
 * moved it. Wired moves, turns and strolls leave the spot alone until the room unloads, which saves where it stands.
 */
class RoomBot(
    val bot: Bot,
    val unitIndex: Int,
    start: Point,
    spotZ: Double = 0.0,
    spotDirection: Int = 0,
) {
    var start: Point = start
        private set
    var spotZ: Double = spotZ
        private set
    var spotDirection: Int = spotDirection
        private set

    /** The owner moved or turned the bot by hand: where it now stands is its spot. */
    fun respot(
        tile: Point,
        z: Double,
        direction: Int,
    ) {
        start = tile
        spotZ = z
        spotDirection = direction
    }

    var dance: Int = 0

    /** When a roaming bot next tries a stroll. */
    var nextWalkAt: Long = 0

    /** The bot was walking at its last tick, so a stop shows as the change to standing. */
    var wasWalking: Boolean = false

    var nextChatAt: Long = 0

    var chatIndex: Int = 0

    var following: Int? = null
}
