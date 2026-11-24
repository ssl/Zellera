package zel.room.entry

import zel.game.players.Player
import zel.protocol.generated.CantConnectOut
import zel.protocol.generated.CantConnectOutCodec
import zel.protocol.generated.CloseConnectionOut
import zel.protocol.generated.CloseConnectionOutCodec
import zel.protocol.generated.DoorbellOut
import zel.protocol.generated.DoorbellOutCodec
import zel.protocol.generated.FlatAccessibleOut
import zel.protocol.generated.FlatAccessibleOutCodec
import zel.protocol.generated.GenericErrorOut
import zel.protocol.generated.GenericErrorOutCodec
import zel.protocol.generated.OpenConnectionOut
import zel.protocol.generated.OpenConnectionOutCodec
import zel.protocol.generated.OutgoingCodec
import zel.protocol.generated.RoomForwardOut
import zel.protocol.generated.RoomForwardOutCodec
import zel.protocol.generated.RoomQueueStatusOutCodec
import zel.room.DoorMode
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.rights.Reply
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.tick.RoomClock
import zel.room.unit.walk.Walking
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RoomEntry(
    private val registry: RoomRegistry,
    private val walking: Walking,
    private val leaving: RoomLeaving,
    private val reader: VisitorReader,
    private val store: EntryStore,
    private val burst: EntryBurst,
    private val arrivals: ArrivalNotes,
    private val environment: EntryEnvironment,
    private val clock: RoomClock = RoomClock.System,
    private val logsEntries: () -> Boolean = { true },
    private val categoryPrompt: CategoryPrompt,
    private val pace: EntryPace = EntryPace(),
    private val attempts: DoorAttempts = DoorAttempts(),
) {
    init {
        leaving.promoteSpectator = { scope, playerId -> burst.promote(scope, leaving.doorOf(scope.room), playerId) }
    }

    fun open(
        player: Player,
        roomId: Int,
        password: String,
    ) {
        val playerId = player.profile.id
        if (!pace.accept(playerId)) {
            dropPending(playerId)
            registry.roomOf(playerId)?.let { leaveRoom(playerId, it) }
            environment.landing(player)
            return
        }
        val card = store.card(roomId) ?: return
        leaveElsewhere(playerId, roomId)
        val pending = reader.read(player, card, environment.nowSeconds())
        val arrival = arrivals.take(playerId, roomId, clock.now())
        val skipsDoor = arrival?.skipsDoor == true
        val passwordFits = passwordFits(pending, password, skipsDoor)
        if (turnedAwayUnloaded(player, pending, passwordFits, skipsDoor)) return
        var entered: Boolean? = null
        var tries = 0
        while (entered == null && tries < LOAD_TRIES) {
            registry.load(roomId) ?: return
            entered =
                registry.withRoom(roomId) { scope ->
                    attempt(scope, pending, passwordFits, arrival, skipsDoor)
                }
            tries++
        }
        if (entered == null) log.warn("room {}: no answer to an entry after {} tries", roomId, LOAD_TRIES)
        if (entered == true) count(player, roomId, ownRoom = card.ownerId == playerId)
    }

    fun answer(
        answerer: Player,
        name: String,
        letIn: Boolean,
    ) {
        val roomId = registry.roomOf(answerer.profile.id) ?: return
        val entered =
            registry.withRoom(roomId) { scope ->
                val door = leaving.doorOf(scope.room)
                if (door.levelOf(answerer.profile.id) < DoorCheck.RIGHTS) return@withRoom null
                val ringing = door.ringingNamed(name) ?: return@withRoom null
                val playerId = ringing.card.id
                door.ringing.remove(playerId)
                leaving.clearPending(playerId, roomId)
                hideBell(scope, door, answerer.profile.id, ringing.card.name)
                if (!letIn) {
                    leaving.deny(scope, playerId)
                    return@withRoom null
                }
                val to = Audience.Player(playerId)
                scope.out.send(Lane.Now, to, FlatAccessibleOutCodec, FlatAccessibleOut(roomId, ""))
                if (check(
                        scope,
                        ringing,
                        passwordFits = false,
                        bellAnswered = true,
                        arrival = null,
                    )
                ) {
                    environment.player(playerId)
                } else {
                    null
                }
            }
        if (entered != null) count(entered, roomId, ownRoom = false)
    }

    fun changeQueue(
        player: Player,
        target: Int,
    ) {
        val playerId = player.profile.id
        val roomId = leaving.pendingRoom(playerId) ?: registry.roomOf(playerId) ?: return
        registry.withRoom(roomId) { scope ->
            val door = leaving.doorOf(scope.room)
            if (door.present[playerId]?.spectator == true) {
                watcherChoice(scope, door, playerId, target)
            } else if (door.queue.change(playerId, target)) {
                leaving.sendQueueStatus(scope, door)
                leaving.admitSpectators(scope, door)
            }
        }
    }

    private fun watcherChoice(
        scope: RoomScope,
        door: RoomDoor,
        playerId: Int,
        target: Int,
    ) {
        if (target != EntryQueue.SPECTATOR || door.queue.remove(playerId) == null) return
        leaving.sendQueueStatus(scope, door, playerId)
        leaving.sendQueueStatus(scope, door)
    }

    fun quit(player: Player) {
        val playerId = player.profile.id
        val wasWaiting = dropPending(playerId)
        val roomId = registry.roomOf(playerId)
        if (roomId == null) {
            if (wasWaiting) environment.landing(player)
            return
        }
        registry.withRoom(roomId) { scope ->
            val present = leaving.doorOf(scope.room).present[playerId]
            val unit = present?.let { scope.room.units[it.unitIndex] }
            when {
                unit != null -> {
                    walking.quit(scope, unit)
                }

                present?.spectator == true -> {
                    leaving.remove(scope, playerId, toSelf = false)
                    scope.out.send(Lane.Now, Audience.Player(playerId), CloseConnectionOutCodec, CloseConnectionOut)
                    environment.landing(player)
                }

                else -> {
                    registry.leave(playerId, roomId)
                    environment.landing(player)
                }
            }
        }
    }

    fun rebuilt(
        saver: Int,
        roomId: Int,
        waiting: List<Int>,
    ) {
        val card = store.card(roomId) ?: return
        val now = environment.nowSeconds()
        val out = waiting.filter { registry.roomOf(it) == null }
        val back =
            out
                .takeIf { card.door == DoorMode.Doorbell }
                .orEmpty()
                .mapNotNull { id -> environment.player(id)?.let { reader.read(it, card, now) } }
        registry.load(roomId) ?: return
        registry.withRoom(roomId) { scope ->
            val forward = RoomForwardOut(roomId)
            scope.out.send(Lane.Now, Audience.Player(saver), RoomForwardOutCodec, forward)
            val door = leaving.doorOf(scope.room)
            back.forEach { ring(scope, door, it) }
            if (card.door == DoorMode.Open || card.door is DoorMode.Password) {
                out.forEach { id ->
                    val to = Audience.Player(id)
                    if (card.door is DoorMode.Password) {
                        scope.out.send(Lane.Now, to, GenericErrorOutCodec, GenericErrorOut(ERROR_WRONG_PASSWORD))
                    }
                    scope.out.send(Lane.Now, to, RoomForwardOutCodec, forward)
                }
            }
        }
    }

    /** The typed password against the stored one, only where the door asks for it and the player may still try. */
    private fun passwordFits(
        pending: Pending,
        password: String,
        skipsDoor: Boolean,
    ): Boolean {
        val door = pending.room.door as? DoorMode.Password ?: return false
        val asked = DoorCheck.asksPassword(door, pending.visitor, pending.room.buildPassLocked, skipsDoor)
        val playerId = pending.card.id
        val roomId = pending.room.id
        if (!asked || password.isEmpty() || attempts.locked(playerId, roomId)) return false
        val fits = DoorSecret.opens(door.secret, password)
        if (fits) attempts.passed(playerId, roomId) else attempts.failed(playerId, roomId)
        return fits
    }

    /** A refusal the room's stored facts already decide is answered without loading the room. */
    private fun turnedAwayUnloaded(
        player: Player,
        pending: Pending,
        passwordFits: Boolean,
        skipsDoor: Boolean,
    ): Boolean {
        val room = pending.room
        val answer =
            DoorCheck.answer(
                room.door,
                passwordFits,
                pending.visitor,
                playersInside = 0,
                maxUsers = room.maxUsers,
                bellAnswered = false,
                buildPassLocked = room.buildPassLocked,
                arrivesPastDoor = skipsDoor,
            )
        if (answer !in DECIDED_UNLOADED) return false
        val playerId = pending.card.id
        if (registry.roomOf(playerId) == room.id) leaveRoom(playerId, room.id)
        val reply =
            object : Reply {
                override fun <T> send(
                    codec: OutgoingCodec<T>,
                    message: T,
                ) = environment.send(player, codec, message)
            }
        refuse(reply, answer, room.id, room.ownerId, pending.card.name, bellAnswered = false)
        return true
    }

    private fun attempt(
        scope: RoomScope,
        pending: Pending,
        passwordFits: Boolean,
        arrival: ArrivalNotes.Arrival?,
        skipsDoor: Boolean,
    ): Boolean {
        val playerId = pending.card.id
        if (leaving.doorOf(scope.room).present.containsKey(playerId)) leaving.remove(scope, playerId, toSelf = true)
        return check(scope, pending, passwordFits, bellAnswered = false, arrival = arrival, skipsDoor = skipsDoor)
    }

    private fun check(
        scope: RoomScope,
        pending: Pending,
        passwordFits: Boolean,
        bellAnswered: Boolean,
        arrival: ArrivalNotes.Arrival?,
        skipsDoor: Boolean = false,
    ): Boolean {
        val door = leaving.doorOf(scope.room)
        val playerId = pending.card.id
        val to = Audience.Player(playerId)
        val answer =
            DoorCheck.answer(
                pending.room.door,
                passwordFits,
                pending.visitor,
                door.visitors(),
                pending.room.maxUsers,
                bellAnswered,
                scope.room.info.isBuildPassLocked,
                skipsDoor,
            )
        // A forwarded spectator's place is used or given up here, except while they ring again.
        val forwardedToWatch = answer != DoorAnswer.Ring && door.admittedSpectators.remove(playerId)
        when (answer) {
            DoorAnswer.In -> {
                val effect = arrival?.effect ?: 0
                val method = arrival?.method ?: RoomEntryMethod.DEFAULT
                burst.enter(
                    scope,
                    door,
                    pending,
                    arrival?.furniId,
                    afterBell = bellAnswered,
                    arrivalEffect = effect,
                    entryMethod = method,
                )
                categoryPrompt.afterEntry(scope, door)
                return true
            }

            DoorAnswer.Ring -> {
                opened(scope, playerId)
                ring(scope, door, pending)
            }

            DoorAnswer.Queue -> {
                return queueOrWatch(scope, door, pending, forwardedToWatch)
            }

            else -> {
                val reply =
                    object : Reply {
                        override fun <T> send(
                            codec: OutgoingCodec<T>,
                            message: T,
                        ) = scope.out.send(Lane.Now, to, codec, message)
                    }
                refuse(reply, answer, scope.room.info.id, scope.room.info.ownerId, pending.card.name, bellAnswered)
            }
        }
        return false
    }

    @Suppress("LongParameterList") // the refusal's facts, the same in and outside a loaded room
    private fun refuse(
        reply: Reply,
        answer: DoorAnswer,
        roomId: Int,
        ownerId: Int,
        visitorName: String,
        bellAnswered: Boolean,
    ) {
        when (answer) {
            DoorAnswer.Banned -> {
                if (!bellAnswered) reply.send(OpenConnectionOutCodec, OpenConnectionOut(roomId))
                reply.send(CantConnectOutCodec, CantConnectOut(BANNED, ByteArray(0)))
                reply.send(GenericErrorOutCodec, GenericErrorOut(ERROR_BANNED))
            }

            DoorAnswer.WrongPassword -> {
                reply.send(OpenConnectionOutCodec, OpenConnectionOut(roomId))
                reply.send(GenericErrorOutCodec, GenericErrorOut(ERROR_WRONG_PASSWORD))
            }

            DoorAnswer.Invisible -> {
                reply.send(OpenConnectionOutCodec, OpenConnectionOut(roomId))
                reply.send(GenericErrorOutCodec, GenericErrorOut(ERROR_BANNED))
            }

            DoorAnswer.BuildPassLocked -> {
                environment.player(ownerId)?.let { environment.buildPassVisitDenied(it, visitorName) }
            }

            DoorAnswer.In, DoorAnswer.Ring, DoorAnswer.Queue -> {
                return
            }
        }
        reply.send(CloseConnectionOutCodec, CloseConnectionOut)
    }

    private fun queueOrWatch(
        scope: RoomScope,
        door: RoomDoor,
        pending: Pending,
        forwardedToWatch: Boolean,
    ): Boolean {
        val playerId = pending.card.id
        val roomId = scope.room.info.id
        when {
            forwardedToWatch -> {
                leaving.clearPending(playerId, roomId)
                burst.enterAsSpectator(scope, door, pending)
            }

            !scope.room.info.isPublic -> {
                opened(scope, playerId)
                val full = CantConnectOut(ROOM_FULL, ByteArray(0))
                scope.out.send(Lane.Now, Audience.Player(playerId), CantConnectOutCodec, full)
                return false
            }

            leaving.spectatorPlaceFree(door) -> {
                burst.enterAsSpectator(scope, door, pending)
                door.queue.add(Queued(playerId, spectator = false, entry = pending))
                leaving.sendQueueStatus(scope, door, playerId)
            }

            else -> {
                queue(scope, door, pending)
                return false
            }
        }
        return true
    }

    /** OpenConnection: comes first, also before a refusal and before the bell. */
    private fun opened(
        scope: RoomScope,
        playerId: Int,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(playerId),
        OpenConnectionOutCodec,
        OpenConnectionOut(scope.room.info.id),
    )

    private fun ring(
        scope: RoomScope,
        door: RoomDoor,
        pending: Pending,
    ) {
        val playerId = pending.card.id
        door.ringing[playerId] = pending
        leaving.markPending(playerId, scope.room.info.id)
        scope.out.send(Lane.Now, Audience.Player(playerId), DoorbellOutCodec, DoorbellOut(""))
        door.controllers().forEach {
            scope.out.send(
                Lane.Now,
                Audience.Player(it),
                DoorbellOutCodec,
                DoorbellOut(pending.card.name),
            )
        }
    }

    private fun queue(
        scope: RoomScope,
        door: RoomDoor,
        pending: Pending,
    ) {
        val playerId = pending.card.id
        val roomId = scope.room.info.id
        val to = Audience.Player(playerId)
        door.queue.add(Queued(playerId, spectator = false, entry = pending))
        leaving.markPending(playerId, roomId)
        scope.out.send(Lane.Now, to, CloseConnectionOutCodec, CloseConnectionOut)
        scope.out.send(Lane.Now, to, FlatAccessibleOutCodec, FlatAccessibleOut(roomId, ""))
        val status = EntryMessages.queueStatus(roomId, spectator = false, ahead = door.queue.ahead(playerId))
        scope.out.send(Lane.Now, to, RoomQueueStatusOutCodec, status)
    }

    private fun hideBell(
        scope: RoomScope,
        door: RoomDoor,
        answererId: Int,
        name: String,
    ) = door.controllers().filter { it != answererId }.forEach {
        scope.out.send(
            Lane.Now,
            Audience.Player(it),
            FlatAccessibleOutCodec,
            FlatAccessibleOut(scope.room.info.id, name),
        )
    }

    private fun leaveElsewhere(
        playerId: Int,
        roomId: Int,
    ) {
        dropPending(playerId, entering = roomId)
        val current = registry.roomOf(playerId) ?: return
        if (current != roomId) leaveRoom(playerId, current)
    }

    private fun leaveRoom(
        playerId: Int,
        roomId: Int,
    ) {
        registry.withRoom(roomId) { scope -> leaving.remove(scope, playerId, toSelf = false) }
        registry.leave(playerId, roomId)
    }

    private fun dropPending(
        playerId: Int,
        entering: Int? = null,
    ): Boolean {
        val roomId = leaving.pendingRoom(playerId) ?: return false
        val comingIn =
            entering == roomId &&
                registry.withRoom(roomId) { playerId in leaving.doorOf(it.room).admittedSpectators } == true
        if (comingIn) return false
        leaving.clearPending(playerId, roomId)
        registry.withRoom(roomId) { scope ->
            val door = leaving.doorOf(scope.room)
            door.ringing.remove(playerId)
            door.admittedSpectators.remove(playerId)
            if (door.queue.remove(playerId) != null) leaving.sendQueueStatus(scope, door)
        }
        return true
    }

    private fun count(
        player: Player,
        roomId: Int,
        ownRoom: Boolean,
    ) {
        val playerId = player.profile.id
        val now = environment.nowSeconds()
        environment.entered(player, roomId)
        environment.persistInOrder(VisitKey(roomId, playerId)) {
            val firstVisit = !store.visitedBefore(roomId, playerId)
            if (logsEntries()) store.logEntry(roomId, playerId, now)
            if (!ownRoom) environment.countEntry(player, firstVisit)
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(RoomEntry::class.java)
        val DECIDED_UNLOADED =
            setOf(DoorAnswer.Banned, DoorAnswer.WrongPassword, DoorAnswer.Invisible, DoorAnswer.BuildPassLocked)
        const val LOAD_TRIES = 3
        const val ROOM_FULL = 1
        const val BANNED = 4
        const val ERROR_BANNED = -100_004
        const val ERROR_WRONG_PASSWORD = -100_002
    }
}
