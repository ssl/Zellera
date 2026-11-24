package zel.room.port

import zel.game.players.Player
import zel.protocol.generated.WhisperOut
import zel.protocol.generated.WhisperOutCodec
import zel.room.RoomRegistry
import zel.room.chat.ChatBubbleStyle
import zel.room.entry.RoomLeaving
import zel.room.furni.place.FurniPickup
import zel.room.life.RoomLife
import zel.room.rights.Moderation
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.UnitStatus
import zel.room.unit.pet.RoomPets
import zel.room.unit.walk.Walking
import zel.util.MILLIS_PER_SECOND
import zel.wired.score.Highscores

@Suppress("TooManyFunctions", "LongParameterList") // one member per room command, one part each needs
class EngineRoomCommands(
    private val registry: RoomRegistry,
    private val leaving: RoomLeaving,
    private val walking: Walking,
    private val life: RoomLife,
    private val pets: RoomPets,
    private val moderation: Moderation,
    private val highscores: Highscores,
    private val pickups: FurniPickup,
    private val player: (Int) -> Player?,
) : RoomCommands {
    override fun rightsLevel(playerId: Int): Int =
        registry.withPlayer(playerId) { scope, _ -> leaving.doorOf(scope.room).levelOf(playerId) } ?: 0

    override fun changePosture(
        playerId: Int,
        sit: Boolean,
    ) {
        registry.withPlayer(playerId) { scope, unit ->
            if (walking.horseOf(scope, unit) == null) walking.changePosture(scope, unit, if (sit) SIT else STAND)
        }
    }

    override fun giveEffect(
        playerId: Int,
        effectId: Int,
        seconds: Int,
    ) {
        registry.withPlayer(playerId) { scope, unit ->
            val endsAt = if (seconds < 0) null else scope.now() + seconds * MILLIS_PER_SECOND
            life.giveEffect(scope, unit, effectId, endsAt)
        }
    }

    override fun effectOf(playerId: Int): Int = registry.withPlayer(playerId) { _, unit -> life.effect(unit) } ?: 0

    override fun whisper(
        playerId: Int,
        text: String,
        bubble: ChatBubbleStyle,
    ) {
        registry.withPlayer(playerId) { scope, unit ->
            val line =
                WhisperOut(unit.index, text, 0, bubble.type, emptyList(), NO_TRACKING, 0, NO_WIDTH, null, null)
            scope.out.send(Lane.Now, Audience.Player(playerId), WhisperOutCodec, line)
        }
    }

    override fun isIdle(playerId: Int): Boolean = registry.withPlayer(playerId) { _, unit -> life.asleep(unit) } == true

    override fun isSitting(playerId: Int): Boolean =
        registry.withPlayer(playerId) { _, unit -> unit.showsStatus(UnitStatus.SIT) } ?: false

    override fun pickAll(playerId: Int) {
        registry.withPlayer(playerId) { scope, _ -> pickups.sweep(scope, playerId) { true } }
    }

    override fun ejectGuestFurni(playerId: Int) {
        registry.withPlayer(playerId) { scope, _ ->
            pickups.sweep(scope, playerId) { it.ownerId != scope.room.info.ownerId }
        }
    }

    override fun pickBuildPass(playerId: Int) {
        registry.withPlayer(playerId) { scope, _ -> pickups.sweep(scope, playerId) { it.isBuildPass } }
    }

    override fun ejectPets(playerId: Int) {
        val speaker = player(playerId) ?: return
        registry.withPlayer(playerId) { scope, _ ->
            pets
                .all(scope)
                .filter { it.pet.userId != scope.room.info.ownerId }
                .forEach { pets.pickUp(scope, speaker, it.pet.id) }
        }
    }

    override fun resetScores(playerId: Int) {
        registry.withPlayer(playerId) { scope, _ -> highscores.command(scope, playerId, RESET_LINE) }
    }

    override fun playerNamed(
        playerId: Int,
        name: String,
    ): Int? =
        registry.withPlayer(playerId) { scope, _ ->
            leaving
                .doorOf(scope.room)
                .present.values
                .firstOrNull { it.card.name.equals(name, ignoreCase = true) }
                ?.card
                ?.id
        }

    override fun staffKick(
        staffId: Int,
        targetId: Int,
    ) {
        registry.withPlayer(staffId) { scope, _ -> moderation.staffKick(scope, staffId, targetId) }
    }

    override fun staffMute(
        staffId: Int,
        targetId: Int,
        minutes: Int,
    ) {
        registry.withPlayer(staffId) { scope, _ -> moderation.staffMute(scope, targetId, minutes) }
    }

    private companion object {
        const val SIT = 1
        const val STAND = 0
        const val RESET_LINE = ":resetscores"
        const val NO_TRACKING = -1
        const val NO_WIDTH = -1
    }
}
