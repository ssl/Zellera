package zel.game.achievements

object AchievementEvents {
    @Volatile
    private var hooks = AchievementHooks.inert()

    // @JvmStatic: an instance method of the object would write the static field (SpotBugs ST).
    @JvmStatic
    fun install(installed: AchievementHooks?) {
        hooks = installed ?: AchievementHooks.inert()
    }

    fun tally(): AchievementTally = hooks.tally

    fun explore(): ExploreActivityAchievements = hooks.explore

    fun roomTime(): RoomTimeAchievements = hooks.roomTime

    fun gamePresence(): GamePresenceAchievement = hooks.gamePresence

    fun identity(): IdentityAchievements = hooks.identity
}
