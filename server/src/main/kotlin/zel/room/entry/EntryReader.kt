package zel.room.entry

import zel.game.permissions.Privilege
import zel.game.permissions.SecurityLevel
import zel.game.permissions.isAnyOwner
import zel.game.players.Player
import zel.game.players.level.AccountLevelTable

class EntryReader(
    private val store: EntryStore,
) : VisitorReader {
    override fun read(
        player: Player,
        room: RoomCard,
        nowSeconds: Int,
    ): Pending {
        val visitor = visitor(player, room, nowSeconds)
        return Pending(card(player), visitor, room, facts(player.profile.id, room, nowSeconds))
    }

    fun visitor(
        player: Player,
        room: RoomCard,
        nowSeconds: Int,
    ): Visitor {
        val id = player.profile.id
        val level = level(player, room)
        return Visitor(
            level = level,
            entersAnyRoom = player.hasPermission(Privilege.ROOM_ENTER_ANY),
            entersFullRooms = player.hasPermission(Privilege.ROOM_ENTER_FULL),
            banned = level != DoorCheck.OWNER && banned(room.id, id, nowSeconds),
            groupMember = groupMember(room.groupId, id),
        )
    }

    fun level(
        player: Player,
        room: RoomCard,
    ): Int {
        val id = player.profile.id
        return when {
            room.ownerId == id -> DoorCheck.OWNER
            player.isAnyOwner -> DoorCheck.STAFF
            else -> maxOf(if (store.hasRights(room.id, id)) DoorCheck.RIGHTS else 0, groupLevel(room.groupId, id))
        }
    }

    private fun groupMember(
        groupId: Int,
        playerId: Int,
    ): Boolean = store.group(groupId)?.let { store.memberLevel(it.id, playerId) in MEMBER_LEVELS } == true

    private fun groupLevel(
        groupId: Int,
        playerId: Int,
    ): Int {
        val group = store.group(groupId) ?: return 0
        return when (store.memberLevel(group.id, playerId)) {
            GroupLevels.OWNER, GroupLevels.ADMIN -> DoorCheck.GROUP_ADMIN
            GroupLevels.MEMBER -> if (group.membersDecorate) DoorCheck.GROUP_MEMBER else 0
            else -> 0
        }
    }

    private fun banned(
        roomId: Int,
        playerId: Int,
        nowSeconds: Int,
    ): Boolean {
        val end = store.banEnd(roomId, playerId) ?: return false
        if (end > nowSeconds) return true
        store.deleteBan(roomId, playerId)
        return false
    }

    fun facts(
        playerId: Int,
        room: RoomCard,
        nowSeconds: Int,
    ): EntryFacts {
        val group = store.group(room.groupId)
        val memberLevel = group?.let { store.memberLevel(it.id, playerId) }
        return EntryFacts(
            group = group,
            isGroupMember = memberLevel != null && memberLevel < GroupLevels.REQUESTED,
            promotion = store.promotion(room.id, nowSeconds),
            voted = store.hasVoted(room.id, playerId),
        )
    }

    fun card(player: Player): PlayerCard {
        val profile = player.profile
        val favourite =
            store.group(player.stats.favouriteGuild)?.let {
                FavouriteGroup(it.id, FAVOURITE_STATUS, it.name, it.badge)
            }
        return PlayerCard(
            id = profile.id,
            name = profile.name,
            motto = profile.motto,
            figure = profile.figure,
            gender = profile.gender.name,
            group = favourite,
            achievementScore = player.stats.tally.achievementScore,
            badgesRank =
                player.services.badgeCaches.leaderboard
                    .totalBadgesRank(profile.id),
            isModerator = SecurityLevel.has(player, SecurityLevel.MODERATOR),
            accountLevel = AccountLevelTable.levelOf(player),
        )
    }

    private companion object {
        val MEMBER_LEVELS = setOf(GroupLevels.OWNER, GroupLevels.ADMIN, GroupLevels.MEMBER)
        const val FAVOURITE_STATUS = 2
    }
}
