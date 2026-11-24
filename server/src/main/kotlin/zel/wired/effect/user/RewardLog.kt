package zel.wired.effect.user

import zel.db.Sql
import javax.sql.DataSource

class RewardLog(
    dataSource: DataSource,
) {
    private val sql = Sql(dataSource)

    /** Newest first. */
    fun given(
        furniId: Int,
        playerId: Int,
    ): List<GivenReward> =
        sql
            .select(GIVEN) {
                int(furniId)
                int(playerId)
            }.rows { GivenReward(it.getString(1), it.getInt(2)) }

    fun count(furniId: Int): Int = sql.select(COUNT) { int(furniId) }.first { it.getInt(1) } ?: 0

    fun note(
        furniId: Int,
        playerId: Int,
        reward: GivenReward,
    ) {
        sql.write(NOTE) {
            int(furniId)
            int(playerId)
            string(reward.code)
            int(reward.at)
        }
    }

    private companion object {
        const val GIVEN =
            "SELECT reward_code, given_at FROM wired_reward_claims WHERE furni_id = ? AND player_id = ? " +
                "ORDER BY given_at DESC, id DESC"
        const val COUNT = "SELECT COUNT(*) FROM wired_reward_claims WHERE furni_id = ?"
        const val NOTE =
            "INSERT INTO wired_reward_claims (furni_id, player_id, reward_code, given_at) VALUES (?, ?, ?, ?)"
    }
}
