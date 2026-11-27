package zel.room.unit.bot

import zel.core.rules.rules
import zel.game.bots.Bot
import zel.game.bots.BotLimits
import zel.game.permissions.Privilege
import zel.game.permissions.isAnyOwner
import zel.game.players.Player
import zel.protocol.generated.BotAddedToInventoryOut
import zel.protocol.generated.BotAddedToInventoryOutCodec
import zel.protocol.generated.BotErrorOut
import zel.protocol.generated.BotErrorOutCodec
import zel.protocol.generated.BotRemovedFromInventoryOut
import zel.protocol.generated.BotRemovedFromInventoryOutCodec
import zel.protocol.generated.DanceOut
import zel.protocol.generated.DanceOutCodec
import zel.protocol.generated.RoomUnitRemoveOut
import zel.protocol.generated.RoomUnitRemoveOutCodec
import zel.protocol.generated.RoomUnitsOut
import zel.protocol.generated.RoomUnitsOutCodec
import zel.protocol.outgoing.inventory.BotGenderLetter
import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.RoomWrites
import zel.room.chat.ChatKind
import zel.room.event.RoomEvent
import zel.room.furni.Steppable
import zel.room.map.Point
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.tick.TickPart
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection
import zel.room.unit.UnitKind
import zel.room.unit.UnitStatus
import zel.room.unit.path.TileState
import zel.room.unit.path.WalkTiles
import zel.room.unit.walk.Walking
import zel.util.MILLIS_PER_SECOND
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

@Suppress("TooManyFunctions") // the bots' room life in one place
class RoomBots(
    private val store: BotStore,
    private val walking: Walking,
    private val host: UnitHost,
    /** Listeners' heads turn toward a bot's talk or shout as toward a user's. */
    private val heard: (RoomScope, RoomUnit) -> Unit = { _, _ -> },
    private val writes: RoomWrites,
) : RoomPart {
    /** Shared by all rooms; a room's entry is only touched under that room's lock. */
    private val rooms = ConcurrentHashMap<Room, MutableMap<Int, RoomBot>>()

    val tick: TickPart = TickPart { scope -> botsOf(scope.room).values.toList().forEach { live(scope, it) } }

    override fun load(room: Room) {
        val bots = LinkedHashMap<Int, RoomBot>()
        store.loadRoom(room.info.id, host::bot).forEach { stored ->
            val unit =
                RoomUnit(
                    room.units.nextIndex(),
                    UnitKind.Bot(stored.bot.id),
                    stored.tile,
                    WalkTiles.at(room, stored.tile).height,
                    stored.direction,
                    stored.direction,
                )
            room.units.add(unit)
            walking.rest(room, unit)
            unit.changed = false // the entry burst carries the posture
            bots[unit.index] =
                RoomBot(
                    stored.bot,
                    unit.index,
                    stored.tile,
                    stored.z,
                    stored.direction,
                ).also { it.dance = stored.dance }
        }
        rooms[room] = bots
    }

    override fun unload(scope: RoomScope) {
        val bots = rooms.remove(scope.room) ?: return
        // The unload saves where each bot stands, wherever wired last moved or turned it.
        bots.values.forEach { bot ->
            val unit = scope.room.units[bot.unitIndex] ?: return@forEach
            bot.respot(unit.tile, unit.z, unit.bodyDirection)
        }
        val rows = bots.values.filterNot { isPasserBy(it.bot) }.map { bot -> rowIn(scope, bot) }
        bots.values.forEach { bot -> scope.room.units.remove(bot.unitIndex) }
        if (rows.isNotEmpty()) writes.write(scope.room.info.id, Runnable { store.saveAll(rows) })
    }

    fun all(scope: RoomScope): List<Pair<RoomBot, RoomUnit>> =
        botsOf(scope.room).values.mapNotNull { bot -> scope.room.units[bot.unitIndex]?.let { bot to it } }

    fun byIndex(
        scope: RoomScope,
        index: Int,
    ): RoomBot? = botsOf(scope.room)[index]

    fun byId(
        scope: RoomScope,
        botId: Int,
    ): RoomBot? = botsOf(scope.room).values.firstOrNull { it.bot.id == botId }

    fun named(
        scope: RoomScope,
        name: String,
    ): List<RoomBot> = botsOf(scope.room).values.filter { it.bot.name == name }

    fun entries(scope: RoomScope): RoomUnitsOut? = all(scope).takeIf { it.isNotEmpty() }?.let(BotEntries::users)

    fun dances(
        scope: RoomScope,
        playerId: Int,
    ) = botsOf(scope.room).values.filter { it.dance > 0 }.forEach {
        scope.out.send(
            Lane.Now,
            Audience.Player(playerId),
            DanceOutCodec,
            DanceOut(it.unitIndex, it.dance),
        )
    }

    fun place(
        scope: RoomScope,
        player: Player,
        botId: Int,
        tile: Point,
    ) {
        val bot = player.inventory.bots[botId] ?: return
        val room = scope.room
        val playerId = player.profile.id
        if (!mayPlace(room, player)) return
        val bots = botsOf(room)
        if (bots.size >= rules.roomMaxBots &&
            !player.hasPermission(Privilege.BOTS_UNLIMITED)
        ) {
            return error(scope, playerId, MAX_BOTS)
        }
        when (tileFor(room, tile, null)) {
            TileAnswer.Free -> Unit
            TileAnswer.Refused -> return
            TileAnswer.BotThere -> return error(scope, playerId, TILE_TAKEN)
        }

        val unit = RoomUnit(room.units.nextIndex(), UnitKind.Bot(bot.id), tile, 0.0, SOUTH, SOUTH)
        room.units.add(unit)
        walking.place(scope, unit, tile)
        val placed = RoomBot(bot, unit.index, tile, unit.z, unit.bodyDirection)
        placed.nextChatAt = scope.now() + bot.speech.delaySeconds * MILLIS_PER_SECOND
        bots[unit.index] = placed
        scope.out.send(Lane.Now, Audience.Room, RoomUnitsOutCodec, BotEntries.users(listOf(placed to unit)))
        host
            .text(GREETING_KEY, "")
            .trim()
            .takeIf { it.isNotEmpty() }
            ?.let { say(scope, placed, it, shout = false) }
        player.inventory.bots.remove(bot)
        scope.out.send(
            Lane.Now,
            Audience.Player(playerId),
            BotRemovedFromInventoryOutCodec,
            BotRemovedFromInventoryOut(bot.id),
        )
        walking.sendNow(scope, listOf(unit))
        save(scope, placed)
        room.furni
            .at(tile)
            .topItem()
            ?.let { scope.raise(RoomEvent.StepOn(unit.index, it.id)) }
    }

    /**
     * The owner's move or turn of a placed bot (PlaceRentableBot: room index, tile, direction). The bot is put on the
     * tile as a placed bot is: at its walk height, sitting on a seat, lying on a bed, else standing; its old tile is
     * free at once and the tile is its new spot. One line for the room. A refused move gives the room the bot's line
     * where it stands, so the mover's client, which already shows the bot on the new tile, puts it back.
     */
    fun move(
        scope: RoomScope,
        player: Player,
        unitIndex: Int,
        tile: Point,
        direction: Int,
    ) {
        val placed = byIndex(scope, unitIndex)?.takeUnless { isPasserBy(it.bot) } ?: return
        val unit = scope.room.units[unitIndex] ?: return
        if (!mayPlace(scope.room, player)) return
        val answer = tileFor(scope.room, tile, unit)
        if (direction !in 0 until UnitDirection.COUNT || answer != TileAnswer.Free) {
            if (answer == TileAnswer.BotThere) error(scope, player.profile.id, TILE_TAKEN)
            return walking.sendNow(scope, listOf(unit))
        }
        val moved = tile != unit.tile
        val fromTop =
            scope.room.furni
                .at(unit.tile)
                .stepItem()
        unit.dropStatus(UnitStatus.MOVE)
        unit.face(direction)
        walking.place(scope, unit, tile)
        placed.respot(tile, unit.z, unit.bodyDirection)
        walking.sendNow(scope, listOf(unit))
        save(scope, placed)
        if (!moved) return
        val toTop =
            scope.room.furni
                .at(tile)
                .stepItem()
        if (fromTop != null && fromTop !== toTop) {
            (fromTop.behaviour as? Steppable)?.onStepOff(scope, fromTop, unit)
            scope.raise(RoomEvent.StepOff(unit.index, fromTop.id))
        }
        if (toTop != null && toTop !== fromTop) scope.raise(RoomEvent.StepOn(unit.index, toTop.id))
    }

    fun pickUp(
        scope: RoomScope,
        player: Player,
        botId: Int,
    ) {
        val placed = byId(scope, botId)?.takeUnless { isPasserBy(it.bot) } ?: return
        val bot = placed.bot
        val playerId = player.profile.id
        val mayPick =
            bot.ownerId == playerId ||
                scope.room.info.ownerId == playerId ||
                player.isAnyOwner ||
                player.hasPermission(Privilege.ROOM_PLACE_ANYWHERE)
        if (!mayPick) return
        val receiver = if (bot.ownerId == playerId) player else host.player(bot.ownerId)
        if (receiver != null && inventoryFull(receiver)) {
            val text = host.text(FULL_KEY, "").replace("%limit%", BotLimits.maxInventory.toString())
            if (receiver === player && text.isNotEmpty()) player.alert(text)
            return
        }
        remove(scope, placed)
        writes.write(scope.room.info.id, Runnable { store.save(rowOf(bot, null, null, placed.dance)) })
        receiver ?: return
        receiver.inventory.bots.put(bot)
        val added = BotAddedToInventoryOut(bot.id, bot.name, bot.motto, BotGenderLetter.of(bot), bot.figure, true)
        scope.out.send(Lane.Now, Audience.Player(receiver.profile.id), BotAddedToInventoryOutCodec, added)
    }

    fun say(
        scope: RoomScope,
        bot: RoomBot,
        text: String,
        shout: Boolean,
        width: Int = -1,
        hideName: Boolean? = null,
    ) {
        val unit = scope.room.units[bot.unitIndex] ?: return
        val says = RoomEvent.Says(unit.index, text, shout, bot.bot.bubble)
        scope.raise(says)
        if (says.hiding.hidden) return
        val kind = if (shout) ChatKind.Shout else ChatKind.Talk
        UnitSpeech.toRoom(scope, unit, kind, text, bot.bot.bubble, width = width, hideName = hideName)
        heard(scope, unit)
    }

    fun whisper(
        scope: RoomScope,
        bot: RoomBot,
        target: RoomUnit,
        text: String,
        width: Int = -1,
        hideName: Boolean? = null,
    ) {
        val unit = scope.room.units[bot.unitIndex] ?: return
        // a bot's whisper fires the says stacks as its talk does; a user's never does
        val says = RoomEvent.Says(unit.index, text, shout = false, styleId = bot.bot.bubble)
        scope.raise(says)
        if (says.hiding.hidden) return
        UnitSpeech.whisper(scope, unit, target, text, bot.bot.bubble, width, hideName)
    }

    fun changed(
        scope: RoomScope,
        bot: RoomBot,
    ) = save(scope, bot)

    /** Lives only while the room is loaded: never saved, moved or picked up. */
    fun addPasserBy(
        scope: RoomScope,
        bot: Bot,
        tile: Point,
        direction: Int,
    ): RoomUnit {
        val room = scope.room
        val unit = RoomUnit(room.units.nextIndex(), UnitKind.Bot(bot.id), tile, 0.0, direction, direction)
        room.units.add(unit)
        walking.place(scope, unit, tile)
        val placed = RoomBot(bot, unit.index, tile, unit.z, direction)
        placed.nextChatAt = scope.now() + bot.speech.delaySeconds * MILLIS_PER_SECOND
        botsOf(room)[unit.index] = placed
        scope.out.send(Lane.Now, Audience.Room, RoomUnitsOutCodec, BotEntries.users(listOf(placed to unit)))
        return unit
    }

    fun removePasserBy(
        scope: RoomScope,
        unitIndex: Int,
    ) {
        val placed = byIndex(scope, unitIndex)?.takeIf { isPasserBy(it.bot) } ?: return
        remove(scope, placed)
    }

    private fun remove(
        scope: RoomScope,
        placed: RoomBot,
    ) {
        botsOf(scope.room).remove(placed.unitIndex)
        scope.room.units.remove(placed.unitIndex)
        val removal = RoomUnitRemoveOut(placed.unitIndex.toString())
        scope.out.send(Lane.Now, Audience.Room, RoomUnitRemoveOutCodec, removal)
    }

    private fun live(
        scope: RoomScope,
        bot: RoomBot,
    ) {
        val unit = scope.room.units[bot.unitIndex] ?: return
        val now = scope.now()
        if (unit.isWalking) {
            bot.wasWalking = true
        } else if (bot.wasWalking) {
            // any walk that ends (arrival, a seat, a portal or wired move in the middle): the first try 1 s later
            bot.wasWalking = false
            bot.nextWalkAt = now + TRY_AFTER_STOP_MILLIS
        }
        if (bot.bot.roams && bot.following == null && !unit.isWalking && now >= bot.nextWalkAt) {
            idleGoal(scope, bot)?.let { walking.walkTo(scope, unit, it) }
            bot.wasWalking = unit.isWalking
            if (!unit.isWalking) bot.nextWalkAt = now + TRY_AGAIN_MILLIS
        }
        speak(scope, bot, now)
    }

    /** Lines in order, or any line by lot. */
    private fun speak(
        scope: RoomScope,
        bot: RoomBot,
        now: Long,
    ) {
        val speech = bot.bot.speech
        val gap = speech.delaySeconds * MILLIS_PER_SECOND
        if (bot.nextChatAt == 0L) bot.nextChatAt = now + gap
        if (!speech.auto || now < bot.nextChatAt) return
        val lines = speech.lines.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return
        val index = bot.chatIndex.takeIf { it in lines.indices } ?: 0
        say(scope, bot, placeholders(scope, bot, lines[index]), shout = false)
        bot.chatIndex = nextLine(index, lines.size, speech.random, host.random)
        bot.nextChatAt = now + gap
    }

    /**
     * One try: one tile drawn from the whole square of the radius around the spot, blocked and taken tiles included;
     * null (the bot stands on) when the drawn tile is no free open tile.
     */
    private fun idleGoal(
        scope: RoomScope,
        bot: RoomBot,
    ): Point? {
        val room = scope.room
        val radius = if (bot.bot.isPublic) PUBLIC_RADIUS else host.config(RADIUS_KEY, DEFAULT_RADIUS)
        val tile =
            if (radius <= WHOLE_ROOM) {
                room.floor.tiles().let { it[host.random.nextInt(it.size)] }
            } else {
                val side = 2 * radius + 1
                Point(
                    bot.start.x - radius + host.random.nextInt(side),
                    bot.start.y - radius + host.random.nextInt(side),
                )
            }
        val free =
            tile != room.floor.door.tile && WalkTiles.at(room, tile).state == TileState.Open &&
                room.units.all().none { it.tile == tile }
        return tile.takeIf { free }
    }

    private fun placeholders(
        scope: RoomScope,
        bot: RoomBot,
        line: String,
    ): String {
        val room = scope.room
        val values =
            listOf(
                room.info.ownerName,
                room.furni
                    .all()
                    .size
                    .toString(),
                bot.bot.name,
                room.info.name,
                room.units
                    .playerIds()
                    .size
                    .toString(),
            )
        return TOKENS.zip(values).fold(line) { text, (tokens, value) ->
            tokens.fold(text) { soFar, token -> soFar.replace(token, value) }
        }
    }

    private fun save(
        scope: RoomScope,
        placed: RoomBot,
    ) {
        if (isPasserBy(placed.bot)) return
        val row = rowIn(scope, placed)
        writes.write(scope.room.info.id, Runnable { store.save(row) })
    }

    /** The bot on its spot, not where a wired box last moved or turned it. */
    private fun rowIn(
        scope: RoomScope,
        placed: RoomBot,
    ): BotStore.Row {
        val spot = placed.takeIf { scope.room.units[it.unitIndex] != null }
        return rowOf(placed.bot, spot, spot?.let { scope.room.info.id }, placed.dance)
    }

    private fun mayPlace(
        room: Room,
        player: Player,
    ): Boolean =
        room.info.ownerId == player.profile.id ||
            player.isAnyOwner ||
            player.hasPermission(Privilege.ROOM_PLACE_ANYWHERE)

    /** Whether a bot may be put on [tile]: a floor, seat or bed tile with no user or other bot than [self] on it. */
    private fun tileFor(
        room: Room,
        tile: Point,
        self: RoomUnit?,
    ): TileAnswer {
        val state = WalkTiles.at(room, tile).state
        val standable = state == TileState.Open || state is TileState.Seat || state is TileState.Bed
        val others = room.units.all().filter { it !== self && it.tile == tile }
        return when {
            !standable || others.any { it.kind is UnitKind.Player } -> TileAnswer.Refused
            others.any { it.kind is UnitKind.Bot } -> TileAnswer.BotThere
            else -> TileAnswer.Free
        }
    }

    private enum class TileAnswer { Free, Refused, BotThere }

    private fun error(
        scope: RoomScope,
        playerId: Int,
        code: Int,
    ) = scope.out.send(Lane.Now, Audience.Player(playerId), BotErrorOutCodec, BotErrorOut(code))

    private fun inventoryFull(player: Player): Boolean =
        !player.hasPermission(Privilege.BOTS_UNLIMITED) &&
            player.inventory.bots.count >= BotLimits.maxInventory

    private fun botsOf(room: Room): MutableMap<Int, RoomBot> = rooms.getOrPut(room) { LinkedHashMap() }

    companion object {
        const val FIRST_PASSER_BY_ID = 0x7F000000

        private val passerByIds =
            java.util.concurrent.atomic
                .AtomicInteger(FIRST_PASSER_BY_ID)

        fun passerById(): Int = passerByIds.getAndUpdate { if (it == Int.MAX_VALUE) FIRST_PASSER_BY_ID else it + 1 }

        fun isPasserBy(bot: Bot): Boolean = bot.id >= FIRST_PASSER_BY_ID

        private const val SOUTH = 4
        private const val MAX_BOTS = 2
        private const val TILE_TAKEN = 3
        private const val DEFAULT_RADIUS = 3

        /** A radius of 0 lets a bot stroll through the whole room. */
        private const val WHOLE_ROOM = 0

        private const val PUBLIC_RADIUS = 2

        /** A roaming bot tries a stroll 1 s after it stopped, then every 10 s while it stands. */
        private const val TRY_AFTER_STOP_MILLIS = 1_000L
        private const val TRY_AGAIN_MILLIS = 10_000L
        private const val RADIUS_KEY = "bots.roaming.radius.tiles"

        /** The tokens a bot line may hold: owner, furni count, bot name, room name, player count. */
        private val TOKENS =
            listOf(
                listOf("%owner%"),
                listOf("%furni%"),
                listOf("%bot%"),
                listOf("%room%"),
                listOf("%players%"),
            )
        private const val GREETING_KEY = "room.bot.placed.greeting"
        private const val FULL_KEY = "inventory.bots.full"

        fun nextLine(
            spoken: Int,
            count: Int,
            shuffled: Boolean,
            random: Random,
        ): Int =
            when {
                count <= 1 -> 0
                shuffled -> random.nextInt(count)
                else -> (spoken + 1) % count
            }

        /** The row of [bot] on [spot]'s spot in room [roomId], or in the inventory without one. */
        fun rowOf(
            bot: Bot,
            spot: RoomBot?,
            roomId: Int?,
            dance: Int,
        ): BotStore.Row =
            BotStore.Row(
                id = bot.id,
                name = bot.name,
                motto = bot.motto,
                figure = bot.figure,
                gender = bot.gender.name,
                roomId = roomId ?: 0,
                x = spot?.start?.x ?: 0,
                y = spot?.start?.y ?: 0,
                z = spot?.spotZ ?: 0.0,
                direction = spot?.spotDirection ?: 0,
                dance = if (spot == null) 0 else dance,
                walks = bot.roams,
                chatLines = bot.speech.lines,
                chatAuto = bot.speech.auto,
                chatRandom = bot.speech.random,
                chatDelay = bot.speech.delaySeconds,
            )
    }
}
