package zel.room.rights

import zel.game.achievements.GameEvents
import zel.game.permissions.Privilege
import zel.game.players.Player
import zel.protocol.generated.BannedPlayersFromRoomOut
import zel.protocol.generated.BannedPlayersFromRoomOutCodec
import zel.protocol.generated.GenericErrorOut
import zel.protocol.generated.GenericErrorOutCodec
import zel.protocol.generated.MuteAllInRoomOut
import zel.protocol.generated.MuteAllInRoomOutCodec
import zel.protocol.generated.PlayerUnbannedFromRoomOut
import zel.protocol.generated.PlayerUnbannedFromRoomOutCodec
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.entry.DoorCheck
import zel.room.entry.RoomLeaving
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.util.SECONDS_PER_DAY
import zel.util.SECONDS_PER_HOUR
import zel.util.SECONDS_PER_MINUTE

@Suppress("TooManyFunctions") // one per moderation action
class Moderation(
    private val registry: RoomRegistry,
    private val leaving: RoomLeaving,
    private val store: RightsStore,
    private val host: ModerationHost,
    private val mutes: RoomMutes,
) {
    fun kick(
        kicker: Player,
        targetId: Int,
    ) {
        inRoomOf(kicker) { scope, level ->
            if (ModerationSetting.mayKick(level, scope.room.info)) kickOut(scope, kicker.profile.id, targetId)
        }
    }

    fun ban(
        banner: Player,
        targetId: Int,
        roomId: Int,
        length: String,
    ) {
        val seconds = BAN_LENGTHS[length] ?: return
        if (targetId == banner.profile.id || unkickable(targetId)) return
        inRoomOf(banner) { scope, level ->
            if (scope.room.info.id != roomId || !ModerationSetting.mayBan(level, scope.room.info)) return@inRoomOf
            val present = leaving.doorOf(scope.room).present[targetId] ?: return@inRoomOf
            if (protects(scope, targetId, present.level)) return@inRoomOf
            val unit = scope.room.units[present.unitIndex] ?: return@inRoomOf
            val now = host.nowSeconds()
            val ends = if (seconds == PERMANENT) Int.MAX_VALUE else now + seconds
            host.persistInOrder(Key(roomId), Runnable { store.ban(roomId, targetId, ends) })
            host.walkOut(scope, unit)
        }
    }

    fun unban(
        targetId: Int,
        roomId: Int,
        isOwner: Boolean,
        reply: Reply,
    ) {
        if (!isOwner || store.bans(roomId, host.nowSeconds()).none { it.id == targetId }) return
        store.unban(roomId, targetId)
        reply.send(PlayerUnbannedFromRoomOutCodec, PlayerUnbannedFromRoomOut(roomId, targetId))
    }

    fun bans(
        playerId: Int,
        roomId: Int,
        isOwner: Boolean,
        reply: Reply,
    ) {
        val mayList = isOwner || (registry.roomOf(playerId) == roomId && controller(playerId, roomId))
        if (!mayList) return
        val banned = store.bans(roomId, host.nowSeconds()).map { BannedPlayersFromRoomOut.List1Item(it.id, it.name) }
        reply.send(BannedPlayersFromRoomOutCodec, BannedPlayersFromRoomOut(roomId, banned))
        if (isOwner) host.progress(playerId, GameEvents.ROOM_DOOR_MODE_SEEN, 1)
    }

    fun mute(
        muter: Player,
        targetId: Int,
        roomId: Int,
        minutes: Int,
    ) {
        if (minutes !in MUTE_MINUTES || unkickable(targetId)) return
        inRoomOf(muter) { scope, level ->
            if (scope.room.info.id != roomId || !ModerationSetting.mayMute(level, scope.room.info)) return@inRoomOf
            muteInRoom(scope, targetId, minutes)
        }
    }

    fun unmuteUser(
        player: Player,
        targetId: Int,
        roomId: Int,
    ) {
        val mayLift = player.hasPermission(Privilege.MODERATION_PLAYER_MUTE) && host.reaches(player, targetId)
        if (mayLift && targetId != player.profile.id) host.player(targetId)?.liftMute()
        inRoomOf(player) { scope, level ->
            val mayMute = ModerationSetting.mayMute(level, scope.room.info)
            if (scope.room.info.id == roomId && mayMute) unmute(scope, targetId)
        }
    }

    fun muteAll(player: Player) {
        inRoomOf(player) { scope, level ->
            if (level < DoorCheck.OWNER) return@inRoomOf
            val muted = mutes.of(scope.room)
            muted.all = !muted.all
            scope.out.send(Lane.Now, Audience.Room, MuteAllInRoomOutCodec, MuteAllInRoomOut(muted.all))
        }
    }

    fun muteLeft(
        scope: RoomScope,
        playerId: Int,
    ): Int {
        val muted = mutes.of(scope.room)
        val ends = muted.players[playerId] ?: return 0
        val left = ends - host.nowSeconds()
        if (left <= 0) muted.players.remove(playerId)
        return left.coerceAtLeast(0)
    }

    fun allMuted(scope: RoomScope): Boolean = mutes.of(scope.room).all

    fun staffKick(
        scope: RoomScope,
        staffId: Int,
        targetId: Int,
    ) = kickOut(scope, staffId, targetId)

    fun staffMute(
        scope: RoomScope,
        targetId: Int,
        minutes: Int,
    ) = muteInRoom(scope, targetId, minutes)

    private fun unmute(
        scope: RoomScope,
        playerId: Int,
    ): Boolean {
        val muted = muteLeft(scope, playerId) > 0
        if (!muted) return false
        mutes.of(scope.room).players.remove(playerId)
        val roomId = scope.room.info.id
        host.persistInOrder(Key(roomId), Runnable { store.unmute(roomId, playerId) })
        return true
    }

    private fun kickOut(
        scope: RoomScope,
        kickerId: Int,
        targetId: Int,
    ) {
        if (targetId == kickerId || unkickable(targetId)) return
        val present = leaving.doorOf(scope.room).present[targetId] ?: return
        if (protects(scope, targetId, present.level)) return
        val unit: RoomUnit = scope.room.units[present.unitIndex] ?: return
        scope.out.send(Lane.Now, Audience.Player(targetId), GenericErrorOutCodec, GenericErrorOut(KICKED))
        host.walkOut(scope, unit)
        host.progress(kickerId, GameEvents.ROOM_KICKED_SOMEONE, 1)
    }

    private fun muteInRoom(
        scope: RoomScope,
        targetId: Int,
        minutes: Int,
    ) {
        val present = leaving.doorOf(scope.room).present[targetId] ?: return
        if (protects(scope, targetId, present.level)) return
        val ends =
            (host.nowSeconds() + minutes.toLong() * SECONDS_PER_MINUTE).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        mutes.of(scope.room).players[targetId] = ends
        val roomId = scope.room.info.id
        host.persistInOrder(Key(roomId), Runnable { store.mute(roomId, targetId, ends) })
    }

    /** The room's owner and anyone at the owner's level or above are never kicked, banned or muted. */
    private fun protects(
        scope: RoomScope,
        targetId: Int,
        targetLevel: Int,
    ): Boolean = targetLevel >= DoorCheck.OWNER || targetId == scope.room.info.ownerId

    private fun controller(
        playerId: Int,
        roomId: Int,
    ): Boolean =
        registry.withRoom(roomId) { scope -> leaving.doorOf(scope.room).levelOf(playerId) >= DoorCheck.RIGHTS } ?: false

    private fun unkickable(playerId: Int): Boolean =
        host.player(playerId)?.hasPermission(Privilege.ROOM_KICK_IMMUNE) == true

    private fun inRoomOf(
        player: Player,
        block: (RoomScope, Int) -> Unit,
    ) {
        val playerId = player.profile.id
        val roomId = registry.roomOf(playerId) ?: return
        registry.withRoom(roomId) { scope -> block(scope, leaving.doorOf(scope.room).levelOf(playerId)) }
    }

    /** One write lane per room, so a mute and a later unmute reach the database in that order. */
    private data class Key(
        val roomId: Int,
    )

    companion object {
        const val KICKED = 4008
        private const val PERMANENT = -1

        /** The mute lengths in minutes the user menus offer: the room menu's three and the ambassador's. */
        val MUTE_MINUTES = setOf(2, 5, 10, 15, 60, 1080, 2160, 4320)

        val BAN_LENGTHS =
            mapOf(
                "RWUAM_BAN_USER_HOUR" to SECONDS_PER_HOUR,
                "RWUAM_BAN_USER_DAY" to SECONDS_PER_DAY,
                "RWUAM_BAN_USER_PERM" to PERMANENT,
            )
    }
}
