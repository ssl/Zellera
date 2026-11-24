package zel.game.permissions

/** One `ranks` row with the permissions it holds; the id is the security level, max values only raise game rules. */
data class Rank(
    val id: Int,
    val name: String = "",
    val badge: String = "",
    val logsCommands: Boolean = false,
    val maxRooms: Int = 0,
    val maxFriends: Int = 0,
    val maxFavouriteRooms: Int = 0,
    val holds: Set<String> = emptySet(),
) {
    fun allows(key: String?): Boolean = key != null && key in holds
}
