package zel.wired.effect.user

import zel.game.effects.EffectRoles
import zel.protocol.generated.CantConnectOut
import zel.protocol.generated.CantConnectOutCodec
import zel.protocol.generated.PlayerEffectOut
import zel.protocol.generated.PlayerEffectOutCodec
import zel.protocol.generated.YouArePlayingGameOut
import zel.protocol.generated.YouArePlayingGameOutCodec
import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.behaviour.state.StateHooks
import zel.room.furni.place.Interactions
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets
import zel.wired.timing.RoomPoster

class TeleportToRoom(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val state: UserEffectState,
    private val host: UserEffectHost,
    private val hooks: StateHooks,
    private val poster: RoomPoster,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val scope = firing.scope
        val linker = Targets.furni(firing, this).firstOrNull { Interactions.of(it.definition) == LINKER } ?: return
        val pair = host.linkerPair(linker.id)?.takeIf { it.roomId != scope.room.info.id } ?: return
        val forwards = state.of(scope.room).forwards
        val now = scope.now()
        val room = scope.room
        UserTargets.players(firing, this).take(MAX_USERS).forEach { (unit, playerId) ->
            val last = forwards[playerId]
            if (last != null && now - last < INTERVAL_MILLIS) return@forEach
            // A user banned from the pair's room is refused at once: no effect, no trip.
            if (host.bannedFrom(pair.roomId, playerId)) {
                val banned = CantConnectOut(BANNED, ByteArray(0))
                scope.out.send(Lane.Now, Audience.Player(playerId), CantConnectOutCodec, banned)
                return@forEach
            }
            forwards[playerId] = now
            // The room sees the effect; the user stays in the room as usual for the wait, leaves the game halfway and
            // then the server opens the pair's room.
            scope.out.send(
                Lane.Now,
                Audience.Room,
                PlayerEffectOutCodec,
                PlayerEffectOut(unit.index, EffectRoles.of(EffectRoles.TELEPORT_ROOM), 0),
            )
            poster.post(room.info.id, GAME_OFF_AFTER_EFFECT_MILLIS) { later -> gameOff(later, room, unit, playerId) }
            poster.post(room.info.id, LEAVE_AFTER_EFFECT_MILLIS) { later ->
                leave(later, room, unit, playerId, linker.id)
            }
        }
    }

    private fun gameOff(
        scope: RoomScope,
        room: Room,
        unit: RoomUnit,
        playerId: Int,
    ) {
        if (scope.room !== room || room.units[unit.index] !== unit) return
        scope.out.send(Lane.Now, Audience.Player(playerId), YouArePlayingGameOutCodec, YouArePlayingGameOut(false))
    }

    /**
     * The wait is over: the user travels while online, also after leaving the room meanwhile; a pair taken out of its
     * room during the wait sends a user still here out of the room instead.
     */
    private fun leave(
        scope: RoomScope,
        room: Room,
        unit: RoomUnit,
        playerId: Int,
        linkerId: Int,
    ) {
        if (scope.room !== room) return
        val here = room.units[unit.index] === unit
        val pair = host.linkerPair(linkerId)?.takeIf { it.roomId != room.info.id }
        if (pair == null) {
            if (here) {
                if (host.kickException(scope, playerId) == null) host.kickOut(scope, unit) else wornBack(scope, unit)
            }
            return
        }
        val leaving =
            host.travel(scope, playerId, pair.roomId, pair.furniId) {
                poster.post(room.info.id, 0) { later -> if (later.room === room) wornBack(later, unit) }
            }
        if (!leaving && here) wornBack(scope, unit)
    }

    /** A user still standing here after the trip was answered shows their own effect again, not the trip's. */
    private fun wornBack(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        if (scope.room.units[unit.index] !== unit) return
        val worn = PlayerEffectOut(unit.index, hooks.effect(unit), 0)
        scope.out.send(Lane.Now, Audience.Room, PlayerEffectOutCodec, worn)
    }

    companion object {
        const val LINKER = "wf_room_linker"
        private const val MAX_USERS = 50
        private const val INTERVAL_MILLIS = 2000L
        private const val BANNED = 4

        /** From AvatarEffect 234 to YouArePlayingGame(false). */
        const val GAME_OFF_AFTER_EFFECT_MILLIS = 500L

        /**
         * From AvatarEffect 234 to the room change, whatever the box delay before the effect; the user can still walk
         * and the room keeps ticking meanwhile.
         */
        const val LEAVE_AFTER_EFFECT_MILLIS = 950L
    }
}
