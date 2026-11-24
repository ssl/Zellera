package zel.game.games.snowball

/** Changed only under the lobby's lock. */
class SnowballGamePlayer internal constructor(
    val user: SnowballUser,
    val skill: SnowballSkill,
    teamId: Int,
) {
    val userId: Int get() = user.userId

    /** 1 = blue, 2 = red. */
    var teamId: Int = teamId
        internal set

    var humanObjectId: Int = 0
        internal set

    var roomBeforeGame: Int = -1
        internal set

    var stageLoaded: Boolean = false
        internal set
}
