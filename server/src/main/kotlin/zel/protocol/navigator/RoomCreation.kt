package zel.protocol.navigator

import zel.game.moderation.WordFilter
import zel.game.permissions.PlayerLimits
import zel.game.permissions.Privilege
import zel.game.players.Player
import zel.room.listing.NewRoomRow
import zel.room.listing.RoomCategory
import zel.room.listing.RoomListings

class RoomCreation(
    private val rooms: RoomListings,
    private val filter: WordFilter,
) {
    fun create(
        player: Player,
        request: NewRoom,
    ): RoomCreationResult {
        val categoryId = categoryFor(player, request.categoryId)
        val refused =
            !modelAllowed(player, request.modelName) ||
                categoryId == null ||
                !withinBounds(request) ||
                !textAccepted(player, request)
        if (refused) return RoomCreationResult.Refused
        val owned = rooms.countOwned(player.profile.id)
        val allowed = PlayerLimits.maxRooms(player)
        if (owned >= allowed) return RoomCreationResult.LimitReached(owned, allowed)
        val row =
            NewRoomRow(
                request.name,
                request.description,
                request.modelName,
                request.maxUsers,
                categoryId,
                request.tradeMode,
            )
        val listed = rooms.create(player, row)?.let(rooms::room) ?: return RoomCreationResult.Refused
        return RoomCreationResult.Created(listed)
    }

    private fun categoryFor(
        player: Player,
        wanted: Int,
    ): Int? =
        if (wanted == 0) {
            personalSpace(rooms.categoriesInOrder())?.id
        } else {
            rooms.category(wanted)?.takeIf { mayChoose(it, player) }?.id
        }

    private fun mayChoose(
        category: RoomCategory,
        player: Player,
    ): Boolean =
        category.minRank <= player.profile.rank.id &&
            (!category.isStaffOnly || player.hasPermission(Privilege.MODERATION_TOOL))

    private fun modelAllowed(
        player: Player,
        modelName: String,
    ): Boolean {
        val clubOnly = rooms.modelClubOnly(modelName) ?: return false
        return !clubOnly || player.stats.club.isMember
    }

    private fun withinBounds(request: NewRoom): Boolean =
        request.maxUsers in MIN_CAPACITY..MAX_CAPACITY && request.tradeMode in 0..MAX_TRADING

    private fun textAccepted(
        player: Player,
        request: NewRoom,
    ): Boolean =
        request.name.trim().length >= MIN_NAME_LENGTH &&
            request.name.length <= MAX_NAME_LENGTH &&
            unfiltered(player, request.name) &&
            request.description.length <= MAX_DESCRIPTION_LENGTH &&
            unfiltered(player, request.description)

    private fun unfiltered(
        player: Player,
        text: String,
    ): Boolean = filter.filter(text, player) == text

    companion object {
        private const val MIN_CAPACITY = 10
        private const val MAX_CAPACITY = 250
        private const val MAX_TRADING = 2
        private const val MIN_NAME_LENGTH = 3
        private const val MAX_NAME_LENGTH = 25
        private const val MAX_DESCRIPTION_LENGTH = 128

        fun personalSpace(all: Collection<RoomCategory>): RoomCategory? =
            all.firstOrNull {
                it.isVisible && !it.isAutomatic && RoomCategory.PERSONAL_KEY.equals(it.globalCategoryKey, true)
            }
    }
}
