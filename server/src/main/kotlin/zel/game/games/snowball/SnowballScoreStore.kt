package zel.game.games.snowball

import java.time.LocalDate

/** One row per user per ISO week (Monday 00:00 UTC of the week the match ended): summed scores and game count. */
interface SnowballScoreStore {
    data class ScoreRow(
        val id: Int,
        val score: Long,
    )

    data class Profile(
        val name: String,
        val figure: String,
        val gender: String,
    )

    fun userScores(
        week: LocalDate?,
        friendsOf: Int,
    ): List<ScoreRow>

    /** A player's score counts for the group he has as favourite when the board is asked for, not when he played. */
    fun groupScores(week: LocalDate?): List<ScoreRow>

    fun groupScorers(
        groupId: Int,
        week: LocalDate,
    ): List<Int>

    fun userProfiles(userIds: Collection<Int>): Map<Int, Profile>

    fun groupProfiles(groupIds: Collection<Int>): Map<Int, Profile>

    fun favouriteGroup(userId: Int): Int

    fun firstWeek(): LocalDate?

    fun weekSettled(week: LocalDate): Boolean

    /** Marks the week settled (false if already); runs before paying, so a crash loses prizes, never pays twice. */
    fun settleWeek(week: LocalDate): Boolean
}
