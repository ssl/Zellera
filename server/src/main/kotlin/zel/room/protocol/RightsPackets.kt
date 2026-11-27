package zel.room.protocol

import zel.game.permissions.Privilege
import zel.game.permissions.isAnyOwner
import zel.game.players.Player
import zel.protocol.generated.AssignRightsInCodec
import zel.protocol.generated.BanPlayerWithDurationInCodec
import zel.protocol.generated.GetBannedPlayersFromRoomInCodec
import zel.protocol.generated.GetFlatControllersInCodec
import zel.protocol.generated.GetRaidProtectionSettingsInCodec
import zel.protocol.generated.GetRoomSettingsInCodec
import zel.protocol.generated.IgnorePlayerInCodec
import zel.protocol.generated.KickPlayerInCodec
import zel.protocol.generated.MuteAllInRoomInCodec
import zel.protocol.generated.MutePlayerInCodec
import zel.protocol.generated.OutgoingCodec
import zel.protocol.generated.RemoveAllRightsInCodec
import zel.protocol.generated.RemoveOwnRoomRightsRoomInCodec
import zel.protocol.generated.RemoveRightsInCodec
import zel.protocol.generated.SaveRaidProtectionSettingsIn
import zel.protocol.generated.SaveRaidProtectionSettingsInCodec
import zel.protocol.generated.SaveRoomSettingsIn
import zel.protocol.generated.SaveRoomSettingsInCodec
import zel.protocol.generated.UnbanPlayerFromRoomInCodec
import zel.protocol.generated.UnignorePlayerInCodec
import zel.protocol.generated.UnmutePlayerInCodec
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.rights.Ignores
import zel.room.rights.Moderation
import zel.room.rights.RaidProtection
import zel.room.rights.RaidSettings
import zel.room.rights.Reply
import zel.room.rights.RightsStore
import zel.room.rights.RoomRights
import zel.room.rights.RoomSettings
import zel.room.rights.SettingsEditor

class RightsPackets(
    rights: RoomRights,
    moderation: Moderation,
    settings: SettingsEditor,
    raid: RaidProtection,
    ignores: Ignores,
    private val store: RightsStore,
) : PacketPackage {
    override val name = "rights"

    /** Giving rights is a click the database answers, so it gets the click gap. */
    private val giveCooldown = ClientCooldown("rights.give", GIVE_GAP_MS)

    override val routes =
        listOf(
            PacketRoute(AssignRightsInCodec) { session, m ->
                if (giveCooldown.acquire(session.client)) rights.give(id(session), m.userId)
            },
            PacketRoute(RemoveRightsInCodec) { session, m ->
                if (safetyAllows(session)) rights.take(id(session), m.userIds)
            },
            PacketRoute(RemoveAllRightsInCodec) { session, m ->
                if (safetyAllows(session)) rights.takeAll(id(session), m.int1, owns(session, m.int1), reply(session))
            },
            PacketRoute(RemoveOwnRoomRightsRoomInCodec) { session, m ->
                rights.giveUp(id(session), m.roomId)
            },
            PacketRoute(GetFlatControllersInCodec) { session, m ->
                rights.list(m.requestedRoomId, owns(session, m.requestedRoomId), reply(session))
            },
            PacketRoute(KickPlayerInCodec) { session, m ->
                player(session)?.let { moderation.kick(it, m.userId) }
            },
            PacketRoute(BanPlayerWithDurationInCodec) { session, m ->
                player(session)?.let { moderation.ban(it, m.userId, m.roomId, m.banName) }
            },
            PacketRoute(UnbanPlayerFromRoomInCodec) { session, m ->
                moderation.unban(m.userId, m.roomId, owns(session, m.roomId), reply(session))
            },
            PacketRoute(GetBannedPlayersFromRoomInCodec) { session, m ->
                moderation.bans(id(session), m.roomId, owns(session, m.roomId), reply(session))
            },
            PacketRoute(MutePlayerInCodec) { session, m ->
                player(session)?.let { moderation.mute(it, m.userId, m.roomId, m.minutes) }
            },
            PacketRoute(UnmutePlayerInCodec) { session, m ->
                player(session)?.let { moderation.unmuteUser(it, m.userId, m.roomId) }
            },
            PacketRoute(MuteAllInRoomInCodec) { session, _ -> player(session)?.let(moderation::muteAll) },
            PacketRoute(IgnorePlayerInCodec) { session, m ->
                player(session)?.let { ignores.ignore(it, m.userId, reply(session)) }
            },
            PacketRoute(UnignorePlayerInCodec) { session, m ->
                player(session)?.let { ignores.unignore(it, m.int1, reply(session)) }
            },
            PacketRoute(GetRaidProtectionSettingsInCodec) { session, m ->
                val here = rights.levelOf(id(session))
                raid.get(m.roomId, here?.first, here?.second ?: 0, reply(session))
            },
            PacketRoute(SaveRaidProtectionSettingsInCodec) { session, m ->
                val here = rights.levelOf(id(session))
                raid.save(m.roomId, here?.first, here?.second ?: 0, raidOf(m), m.confirmed, reply(session))
            },
            PacketRoute(GetRoomSettingsInCodec) { session, m ->
                player(session)?.let { settings.get(it.profile.id, m.roomId, anyRoom(it), reply(session)) }
            },
            PacketRoute(SaveRoomSettingsInCodec) { session, m ->
                player(session)?.let {
                    val staff = it.hasPermission(Privilege.MODERATION_TOOL)
                    val rank = it.profile.rank.id
                    val club = it.stats.club.isMember || anyRoom(it)
                    settings.save(
                        it.profile.id,
                        m.roomId,
                        settingsOf(m),
                        anyRoom(it),
                        staff,
                        rank,
                        reply(session),
                        club,
                    )
                }
            },
        )

    private fun player(session: PacketSession): Player? = session.client.player

    private fun id(session: PacketSession): Int =
        session.client.player
            ?.profile
            ?.id ?: 0

    private fun safetyAllows(session: PacketSession): Boolean =
        player(session)
            ?.stats
            ?.standing
            ?.safetyAllows() == true

    private fun anyRoom(player: Player): Boolean = player.isAnyOwner

    private fun owns(
        session: PacketSession,
        roomId: Int,
    ): Boolean {
        val player = player(session) ?: return false
        return anyRoom(player) || store.settings(roomId)?.ownerId == player.profile.id
    }

    private fun reply(session: PacketSession): Reply =
        object : Reply {
            override fun <T> send(
                codec: OutgoingCodec<T>,
                message: T,
            ) = session.send(codec, message)
        }

    private fun raidOf(m: SaveRaidProtectionSettingsIn) =
        RaidSettings(
            enabled = m.wanted,
            sensitivity = m.wanted2,
            actionType = m.wanted3,
            banSeconds = m.wanted4,
            guardEnabled = m.wanted5,
            guardSeconds = m.wanted6,
            guardSensitivity = m.wanted7,
        )

    private fun settingsOf(m: SaveRoomSettingsIn) =
        RoomSettings(
            ownerId = 0,
            name = m.name,
            description = m.description,
            doorMode = m.doorMode,
            password = m.password,
            maxUsers = m.maximumVisitors,
            categoryId = m.categoryId,
            tags = m.tags,
            tradeMode = m.tradeMode,
            allowPets = m.allowPets,
            allowPetsEat = m.allowFoodConsume,
            allowWalkThrough = m.allowWalkThrough,
            hideWalls = m.hideWalls,
            wallThickness = m.wallThickness,
            floorThickness = m.floorThickness,
            floodFilter = m.floodSensitivity,
            leaveOnDoorTile = m.leaveOnDoorTile,
            idleSleep = m.idleSleep,
            idleSleepSeconds = m.idleSleepTimeout,
            idleKick = m.idleAutokick,
            idleKickSeconds = m.idleAutokickTimeout,
            muteAllPets = m.muteAllPets,
            whoCanMute = m.whoCanMute,
            whoCanKick = m.whoCanKick,
            whoCanBan = m.whoCanBan,
        )

    private companion object {
        const val GIVE_GAP_MS = 300L
    }
}
