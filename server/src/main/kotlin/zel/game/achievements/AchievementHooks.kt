package zel.game.achievements

import zel.game.players.Player

class AchievementHooks(
    val tally: AchievementTally,
    val explore: ExploreActivityAchievements,
    val roomTime: RoomTimeAchievements,
    val gamePresence: GamePresenceAchievement,
    val identity: IdentityAchievements,
) {
    companion object {
        fun inert(): AchievementHooks {
            val none = AchievementProgress.NONE
            return AchievementHooks(
                tally = AchievementTally { _: Player?, _: String, _: Int -> },
                explore = ExploreActivityAchievements(none),
                roomTime = RoomTimeAchievements(none, { null }, { 0 }),
                gamePresence = GamePresenceAchievement(none),
                identity = IdentityAchievements(none, { 0 }) { emptyList() },
            )
        }
    }
}
