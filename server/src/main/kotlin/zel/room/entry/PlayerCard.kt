package zel.room.entry

data class PlayerCard(
    val id: Int,
    val name: String,
    val motto: String,
    val figure: String,
    val gender: String,
    val group: FavouriteGroup?,
    val achievementScore: Int,
    val badgesRank: Int,
    val isModerator: Boolean = false,
    val accountLevel: Int = 0,
)
