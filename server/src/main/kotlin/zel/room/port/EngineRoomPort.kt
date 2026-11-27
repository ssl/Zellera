package zel.room.port

import zel.game.players.Player
import zel.protocol.Outbound
import zel.protocol.generated.ExpressionOut
import zel.protocol.generated.ExpressionOutCodec
import zel.protocol.generated.ObjectDataUpdateOut
import zel.protocol.generated.ObjectDataUpdateOutCodec
import zel.protocol.generated.PlayerNameChangedOut
import zel.protocol.generated.PlayerNameChangedOutCodec
import zel.protocol.generated.RespectNotificationOut
import zel.protocol.generated.RespectNotificationOutCodec
import zel.protocol.generated.RoomUnitChangeOut
import zel.protocol.generated.RoomUnitChangeOutCodec
import zel.protocol.generated.RoomUseIconOut
import zel.protocol.generated.RoomUseIconOutCodec
import zel.room.RoomInfo
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.entry.PlayerCard
import zel.room.entry.RoomLeaving
import zel.room.furni.RoomFurni
import zel.room.furni.place.FurniWire
import zel.room.rights.RoomRights
import zel.room.side.RoomQuestions
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit

@Suppress("TooManyFunctions") // implements RoomPort, one member per question
class EngineRoomPort(
    private val registry: RoomRegistry,
    private val leaving: RoomLeaving,
    private val rights: RoomRights,
    private val storedLevel: (playerId: Int, roomId: Int) -> Int?,
    private val write: (playerId: Int, message: Outbound<*>) -> Unit,
    private val infos: (Int) -> RoomInfo?,
    private val storeGroup: (roomId: Int, groupId: Int) -> Unit,
    private val settingsApplied: (Int) -> Unit,
    private val card: (Int) -> PlayerCard?,
    private val muted: (RoomScope, playerId: Int) -> Boolean = { _, _ -> false },
    private val iconShown: (RoomScope, unitIndex: Int, iconId: Int) -> Unit = { _, _, _ -> },
    commands: RoomCommands = RoomCommands.None,
    private val respected: (RoomScope, RoomUnit) -> Unit = { _, _ -> },
    private val keepsRoom: (RoomScope, RoomFurni) -> Boolean = { _, _ -> false },
    private val trades: (Int) -> Unit = { },
    private val closing: RoomClosing = RoomClosing.None,
    private val buildPassRooms: (ownerId: Int) -> Int = { 0 },
    private val questions: RoomQuestions = RoomQuestions(),
    private val leftGame: (playerId: Int) -> Unit = { },
) : RoomPort,
    RoomCommands by commands {
    override fun isLoaded(roomId: Int): Boolean = registry.isLoaded(roomId)

    override fun loadedRoomIds(): Set<Int> = registry.loadedRoomIds()

    override fun roomOf(playerId: Int): Int? = registry.roomOf(playerId)

    override fun playersIn(roomId: Int): List<Int> = registry.playersIn(roomId)

    override fun groupOf(roomId: Int): Int = registry.loaded(roomId)?.info?.groupId ?: 0

    override fun categoryOf(roomId: Int): Int = registry.loaded(roomId)?.info?.categoryId ?: 0

    override fun summary(roomId: Int): RoomSummary? =
        (registry.loaded(roomId)?.info ?: infos(roomId))?.let {
            RoomSummary(it.id, it.ownerId, it.ownerName, it.name, it.groupId, it.isPublic)
        }

    override fun assignGroup(
        roomId: Int,
        groupId: Int,
    ) {
        storeGroup(roomId, groupId)
        settingsApplied(roomId)
        registry.playersIn(roomId).forEach { refreshLevel(it, roomId) }
    }

    override fun send(
        roomId: Int,
        message: Outbound<*>,
    ) = registry.playersIn(roomId).forEach { write(it, message) }

    override fun showIcon(
        playerId: Int,
        iconId: Int,
        ignoresMutes: Boolean,
    ): Boolean {
        val roomId = registry.roomOf(playerId) ?: return false
        return registry.withRoom(roomId) { scope ->
            val unit = scope.room.units.ofPlayer(playerId)
            if (unit == null || (!ignoresMutes && muted(scope, playerId))) {
                false
            } else {
                scope.out.send(
                    Lane.Now,
                    Audience.Room,
                    RoomUseIconOutCodec,
                    RoomUseIconOut(unit.index, iconId),
                )
                iconShown(scope, unit.index, iconId)
                true
            }
        } == true
    }

    override fun unitIndex(playerId: Int): Int? {
        val roomId = registry.roomOf(playerId) ?: return null
        return registry.withRoom(roomId) { leaving.doorOf(it.room).present[playerId]?.unitIndex }
    }

    override fun furniIn(
        roomId: Int,
        furniId: Int,
    ): PlacedFurni? =
        registry.withRoom(roomId) { scope ->
            scope.room.furni[furniId]?.let { PlacedFurni(it.id, it.ownerId, it.definition) }
        }

    override fun furniClasses(roomId: Int): Set<String> =
        registry
            .withRoom(roomId) { scope ->
                scope.room.furni
                    .all()
                    .mapTo(HashSet()) { it.definition.name.substringBefore('*') }
            }.orEmpty()

    override fun nameChanged(playerId: Int) {
        val roomId = registry.roomOf(playerId) ?: return
        val look = card(playerId) ?: return
        registry.post(roomId) { scope ->
            val door = leaving.doorOf(scope.room)
            val present = door.present[playerId] ?: return@post
            door.present[playerId] = present.copy(card = look)
            val changed = PlayerNameChangedOut(playerId, present.unitIndex, look.name)
            scope.out.send(Lane.Now, Audience.Room, PlayerNameChangedOutCodec, changed)
        }
    }

    override fun groupRightsChanged(playerId: Int) = refreshRights(playerId)

    override fun groupLookChanged(
        roomId: Int,
        groupId: Int,
    ) {
        if (groupId <= 0 || !registry.isLoaded(roomId)) return
        registry.post(roomId) { scope ->
            scope.room.furni.all().filter { it.groupId == groupId }.forEach { furni ->
                val update = ObjectDataUpdateOut(furni.id.toString(), FurniWire.stuff(furni))
                scope.out.send(Lane.Now, Audience.Room, ObjectDataUpdateOutCodec, update)
            }
        }
    }

    override fun syncBuildPassRooms(ownerId: Int): Int = buildPassRooms(ownerId)

    override fun leftGame(playerId: Int) {
        kick(playerId)
        leftGame.invoke(playerId)
    }

    override fun leaveForGame(playerId: Int): Int {
        val roomId = registry.roomOf(playerId) ?: return -1
        registry.withRoom(roomId) { leaving.remove(it, playerId, toSelf = false) }
        return roomId
    }

    override fun askRoomQuestion(
        roomId: Int,
        question: String,
        millis: Long,
    ): Boolean = registry.withRoom(roomId) { questions.start(it, question, millis) } == true

    override fun answerRoomQuestion(
        playerId: Int,
        questionId: Int,
        answer: String,
    ) {
        registry.withPlayer(playerId) { scope, _ -> questions.answer(scope, playerId, questionId, answer) }
    }

    override fun respectShown(
        fromId: Int,
        toId: Int,
        total: Int,
    ) {
        val roomId = registry.roomOf(fromId) ?: return
        registry.withRoom(roomId) { scope ->
            val unit = leaving.doorOf(scope.room).present[fromId]?.let { scope.room.units[it.unitIndex] }
            val notice = RespectNotificationOut(toId, total)
            scope.out.send(Lane.Now, Audience.Room, RespectNotificationOutCodec, notice)
            if (unit != null) {
                val thumbUp = ExpressionOut(unit.index, THUMB_UP)
                scope.out.send(Lane.Now, Audience.Room, ExpressionOutCodec, thumbUp)
                respected(scope, unit)
            }
        }
    }

    override fun stopTrade(playerId: Int) = trades(playerId)

    override fun cardChanged(playerId: Int) {
        val roomId = registry.roomOf(playerId) ?: return
        val look = card(playerId) ?: return
        registry.post(roomId) { scope ->
            val door = leaving.doorOf(scope.room)
            val present = door.present[playerId] ?: return@post
            door.present[playerId] = present.copy(card = look)
        }
    }

    override fun appearanceChanged(playerId: Int) {
        val roomId = registry.roomOf(playerId) ?: return
        val look = card(playerId) ?: return
        registry.post(roomId) { scope ->
            val door = leaving.doorOf(scope.room)
            val present = door.present[playerId] ?: return@post
            door.present[playerId] = present.copy(card = look)
            val change =
                RoomUnitChangeOut(
                    present.unitIndex,
                    look.figure,
                    look.gender,
                    look.motto,
                    look.achievementScore,
                    "",
                    emptyList(),
                    look.badgesRank,
                )
            scope.out.send(Lane.Now, Audience.Room, RoomUnitChangeOutCodec, change)
        }
    }

    override fun kick(playerId: Int) {
        val roomId = registry.roomOf(playerId) ?: return
        registry.post(roomId) { scope ->
            if (!leaving.drop(scope, playerId)) registry.leave(playerId, roomId)
        }
    }

    override fun refreshRights(playerId: Int) {
        registry.roomOf(playerId)?.let { refreshLevel(playerId, it) }
    }

    private fun refreshLevel(
        playerId: Int,
        roomId: Int,
    ) {
        val level = storedLevel(playerId, roomId) ?: return
        registry.post(roomId) { scope -> rights.changeLevel(scope, playerId, level) }
    }

    private companion object {
        const val THUMB_UP = 7
    }

    override fun mayDelete(roomId: Int): Boolean {
        registry.load(roomId)
        return registry.withRoom(roomId) { scope ->
            scope.room.furni
                .all()
                .none { keepsRoom(scope, it) }
        } != false
    }

    override fun emptyForDelete(
        roomId: Int,
        owner: Player,
    ): Boolean {
        registry.load(roomId)
        val furni = registry.withRoom(roomId) { closing.empty(it, owner) }
        return closing.written() && furni?.getNow(false) != false
    }

    override fun moderate(
        roomId: Int,
        newName: String?,
        lock: Boolean,
    ) {
        closing.storeModeration(roomId, newName, lock)
        settingsApplied(roomId)
    }

    override fun ownerRenamed(
        ownerId: Int,
        newName: String,
    ) {
        registry
            .loadedRoomIds()
            .filter { registry.loaded(it)?.info?.ownerId == ownerId }
            .forEach(settingsApplied)
    }

    override fun groupRemoved(roomId: Int) {
        closing.storeNoGroup(roomId)
        settingsApplied(roomId)
        registry.playersIn(roomId).forEach { refreshLevel(it, roomId) }
    }

    override fun sendRoomInfo(
        playerId: Int,
        roomId: Int,
    ) = closing.sendInfo(playerId, roomId)

    override fun ejectFurniOf(
        roomId: Int,
        ownerId: Int,
    ) {
        registry.load(roomId)
        registry.withRoom(roomId) { closing.ejectFurni(it, ownerId) }
    }

    override fun saveFurni(roomId: Int) {
        registry.withRoom(roomId) { closing.saveFurni(it) }
        closing.settle(roomId)
    }

    override fun close(roomId: Int) {
        registry.withRoom(roomId) { scope ->
            scope.room.units
                .playerIds()
                .mapNotNull { scope.room.units.ofPlayer(it) }
                .forEach { leaving.leave(scope, it) }
        }
        registry.unload(roomId)
    }
}
