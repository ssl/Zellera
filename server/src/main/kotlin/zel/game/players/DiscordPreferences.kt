package zel.game.players

data class DiscordPreferences(
    val version: Int,
    val showPlayer: Boolean,
    val shareActivity: Boolean,
    val hideInHiddenRooms: Boolean,
    val allowJoining: Boolean,
) {
    /** The four choices as one stored number, a bit each. */
    val sharing: Int
        get() =
            bit(showPlayer, SHOW_PLAYER) or bit(shareActivity, SHARE_ACTIVITY) or
                bit(hideInHiddenRooms, HIDE_IN_HIDDEN_ROOMS) or bit(allowJoining, ALLOW_JOINING)

    companion object {
        private const val SHOW_PLAYER = 1
        private const val SHARE_ACTIVITY = 2
        private const val HIDE_IN_HIDDEN_ROOMS = 4
        private const val ALLOW_JOINING = 8

        val UNSET = stored(version = 0, sharing = 0)

        fun stored(
            version: Int,
            sharing: Int,
        ) = DiscordPreferences(
            version.coerceAtLeast(0),
            sharing and SHOW_PLAYER != 0,
            sharing and SHARE_ACTIVITY != 0,
            sharing and HIDE_IN_HIDDEN_ROOMS != 0,
            sharing and ALLOW_JOINING != 0,
        )

        private fun bit(
            chosen: Boolean,
            mask: Int,
        ) = if (chosen) mask else 0
    }
}
