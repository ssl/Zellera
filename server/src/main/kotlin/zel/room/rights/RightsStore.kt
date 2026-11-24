package zel.room.rights

import zel.room.entry.NamedPlayer

@Suppress("TooManyFunctions") // one per stored fact
interface RightsStore {
    fun hasRights(
        roomId: Int,
        playerId: Int,
    ): Boolean

    fun addRights(
        roomId: Int,
        playerId: Int,
    )

    fun removeRights(
        roomId: Int,
        playerId: Int,
    )

    fun playerName(playerId: Int): String?

    fun rightsHolders(roomId: Int): List<NamedPlayer>

    /** Stores a ban that ends at [ends] (unix seconds), replacing an older one. */
    fun ban(
        roomId: Int,
        playerId: Int,
        ends: Int,
    )

    fun unban(
        roomId: Int,
        playerId: Int,
    )

    fun bans(
        roomId: Int,
        nowSeconds: Int,
    ): List<NamedPlayer>

    /** Stores a mute that ends at [ends] (unix seconds), replacing an older one. */
    fun mute(
        roomId: Int,
        playerId: Int,
        ends: Int,
    )

    fun unmute(
        roomId: Int,
        playerId: Int,
    )

    fun mutes(
        roomId: Int,
        nowSeconds: Int,
    ): Map<Int, Int>

    /** The room's raid protection settings; null when it never saved any. */
    fun raid(roomId: Int): RaidSettings?

    fun saveRaid(
        roomId: Int,
        settings: RaidSettings,
    )

    fun settings(roomId: Int): RoomSettings?

    fun saveSettings(
        roomId: Int,
        settings: RoomSettings,
    )

    /** True when [categoryId] exists and a player of [rank] may list a room in it; staff-only ones need [staff]. */
    fun categoryOpenTo(
        categoryId: Int,
        rank: Int,
        staff: Boolean,
    ): Boolean
}
