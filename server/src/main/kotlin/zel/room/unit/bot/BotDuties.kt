package zel.room.unit.bot

import zel.game.players.Player
import zel.room.Room
import zel.room.RoomListener
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.util.GameZone
import zel.wired.effect.bot.BotErrands
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.max

/** Butler bots hand out what a player asks for; visitor-log bots tell the owner who came by. */
class BotDuties(
    private val bots: RoomBots,
    private val errands: BotErrands,
    private val hooks: BotHands,
    private val units: UnitHost,
) : RoomListener {
    fun interface BotHands {
        fun hold(
            scope: RoomScope,
            unit: RoomUnit,
            item: Int,
        )
    }

    /** A visitor log tells its list once for as long as its room stays loaded. */
    private class Log {
        var told = false
        var visits: List<UnitHost.Visit> = emptyList()
    }

    private val logs = WeakHashMap<Room, MutableMap<Int, Log>>()

    override fun on(
        scope: RoomScope,
        event: RoomEvent,
    ) {
        when (event) {
            is RoomEvent.Says -> says(scope, event)
            is RoomEvent.Entered -> entered(scope, event)
            else -> Unit
        }
    }

    private fun playerOf(unit: RoomUnit): Player? = (unit.kind as? UnitKind.Player)?.let { units.player(it.playerId) }

    private fun says(
        scope: RoomScope,
        event: RoomEvent.Says,
    ) {
        val unit = scope.room.units[event.unitIndex] ?: return
        val player = playerOf(unit) ?: return
        val spoken = event.text.trim().lowercase()
        val order = units.butlerServes()[spoken]?.takeIf { it > 0 }
        val asksForLog =
            player.profile.id == scope.room.info.ownerId && spoken.isNotEmpty() &&
                spoken.equals(units.text(WORD_KEY, ""), ignoreCase = true)
        if (order == null && !asksForLog) return
        bots.all(scope).forEach { (bot, botUnit) ->
            when (bot.bot.type) {
                BUTLER -> if (order != null) serve(scope, bot, botUnit, unit, Order(spoken, order, player))
                VISITOR_LOG -> if (asksForLog) tellVisits(scope, bot)
            }
        }
    }

    private class Order(
        val word: String,
        val handItem: Int,
        val by: Player,
    )

    /**
     * A butler on its way hears no order. One that stands passes the drink to anyone it hears; one that roams hands
     * it over from where it is when the player is within reach, and carries it over otherwise.
     */
    private fun serve(
        scope: RoomScope,
        bot: RoomBot,
        botUnit: RoomUnit,
        unit: RoomUnit,
        order: Order,
    ) {
        if (botUnit.isWalking) return
        val distance = max(abs(botUnit.tile.x - unit.tile.x), abs(botUnit.tile.y - unit.tile.y))
        if (distance > units.config(HEARING_KEY, DEFAULT_HEARING)) return
        val served = { at: RoomScope -> sayServed(at, bot, order) }
        when {
            !bot.bot.roams -> {
                hooks.hold(scope, unit, order.handItem)
                served(scope)
            }

            distance <= units.config(REACH_KEY, DEFAULT_REACH) -> {
                errands.serveNear(scope, bot, botUnit, unit, order.handItem, NEAR_DELAY_MILLIS, served)
            }

            else -> {
                errands.serve(scope, bot, botUnit, unit, order.handItem, served)
            }
        }
    }

    private fun sayServed(
        scope: RoomScope,
        bot: RoomBot,
        order: Order,
    ) {
        val template = units.text(SERVED_KEY, "")
        if (template.isEmpty()) return
        val line = template.replace("%item%", order.word).replace("%player%", order.by.profile.name)
        bots.say(scope, bot, line, shout = false)
    }

    private fun logOf(
        scope: RoomScope,
        bot: RoomBot,
    ): Log = synchronized(logs) { logs.getOrPut(scope.room) { HashMap() } }.getOrPut(bot.unitIndex) { Log() }

    /** Said to the room each time the owner enters, until the list has been told. */
    private fun entered(
        scope: RoomScope,
        event: RoomEvent.Entered,
    ) {
        val unit = scope.room.units[event.unitIndex] ?: return
        val player = playerOf(unit) ?: return
        val info = scope.room.info
        if (player.profile.id != info.ownerId) return
        bots.all(scope).filter { (bot, _) -> bot.bot.type == VISITOR_LOG }.forEach { (bot, _) ->
            val log = logOf(scope, bot)
            if (log.told) return@forEach
            val visits = units.roomVisits(info.id, player.profile.lastOnlineAt, info.ownerName)
            log.visits = visits
            val line =
                if (visits.isEmpty()) {
                    units.text(NOBODY_KEY, "")
                } else {
                    units
                        .text(SUMMARY_KEY, "")
                        .replace("%count%", visits.size.toString())
                        .replace("%word%", units.text(WORD_KEY, ""))
                }
            if (line.isNotEmpty()) bots.say(scope, bot, line, shout = false)
        }
    }

    private fun tellVisits(
        scope: RoomScope,
        bot: RoomBot,
    ) {
        val log = logOf(scope, bot)
        if (log.told) return
        log.told = true
        val visits = log.visits
        log.visits = emptyList()
        val format =
            DateTimeFormatter
                .ofPattern(units.textOf(DATE_FORMAT_KEY, DEFAULT_DATE_FORMAT))
                .withZone(GameZone.current)
        val entry = units.text(ENTRY_KEY, "")
        val list =
            visits.joinToString(separator = "") { visit ->
                LINE_BREAK +
                    entry
                        .replace("%player%", visit.roomName)
                        .replace("%time%", format.format(Instant.ofEpochSecond(visit.timestamp.toLong())))
            }
        val line =
            units
                .text(REPORT_KEY, "")
                .replace("%count%", visits.size.toString())
                .replace("%visits%", list)
        bots.say(scope, bot, line, shout = false)
    }

    private companion object {
        const val BUTLER = "bartender"
        const val VISITOR_LOG = "visitor_log"
        const val HEARING_KEY = "bots.butler.hearing.tiles"
        const val DEFAULT_HEARING = 5
        const val REACH_KEY = "bots.butler.reach.tiles"
        const val DEFAULT_REACH = 3
        const val NEAR_DELAY_MILLIS = 1000L
        const val LINE_BREAK = "\r"
        const val SERVED_KEY = "room.bot.butler.served"
        const val NOBODY_KEY = "room.bot.visitor.nobody"
        const val SUMMARY_KEY = "room.bot.visitor.summary"
        const val REPORT_KEY = "room.bot.visitor.report"
        const val WORD_KEY = "room.bot.visitor.word"
        const val ENTRY_KEY = "room.bot.visitor.entry"
        const val DATE_FORMAT_KEY = "bots.visitor_log.time_format"
        const val DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm"
    }
}
