package zel.game.earnings

import zel.db.allOrNothing
import zel.db.first
import zel.db.rows
import zel.db.write
import zel.game.earnings.IncomeRewards.DailyGift
import zel.game.earnings.IncomeRewards.Paid
import zel.game.earnings.IncomeRewards.Reward
import zel.game.economy.LedgerEntry
import zel.game.economy.LedgerTransaction
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

class JdbcIncomeRewardStore(
    private val dataSource: DataSource,
) : IncomeRewards.Store {
    override fun insert(
        userId: Int,
        category: Int,
        type: Int,
        amount: Int,
        productCode: String,
        created: Int,
    ) {
        dataSource.connection.use { insert(it, userId, category, type, amount, productCode, created) }
    }

    override fun list(userId: Int): List<Reward> =
        dataSource.connection.use { connection ->
            connection.rows(LIST_REWARDS, { int(userId) }) {
                Reward(
                    it.getInt("id"),
                    it.getInt("category"),
                    it.getInt("reward_type"),
                    it.getInt("amount"),
                    it.getString("product_code").orEmpty(),
                )
            }
        }

    override fun grantDailyGift(
        userId: Int,
        day: Int,
        amount: Int,
        created: Int,
    ): DailyGift =
        inTransaction { connection ->
            lockUser(connection, userId)
            val (lastGiftDay, lastStatusDay) = readDays(connection, userId)
            val firstStatusToday = lastStatusDay < day
            if (lastGiftDay >= day && !firstStatusToday) return@inTransaction DailyGift.NONE

            val added = IncomeRewards.addsDailyGift(lastGiftDay, day, giftWaiting(connection, userId))
            markDays(connection, userId, if (added) day else lastGiftDay, day)
            if (added) insert(connection, userId, IncomeRewards.DAILY_GIFT, IncomeRewards.POINTS, amount, "", created)
            DailyGift(added, firstStatusToday)
        }

    override fun pay(
        userId: Int,
        rewards: List<Reward>,
        furni: IncomeRewards.FurniMaker,
    ): List<Paid> =
        inTransaction { connection ->
            // Same lock order as the wallet ledger and the achievement rewards: the player row first.
            lockUser(connection, userId)
            val taken = deleteRows(connection, userId, rewards)
            taken.map { reward ->
                when {
                    reward.isFurni -> Paid(reward, -1, furni.make(connection, reward))
                    reward.amount <= 0 -> Paid(reward, -1)
                    else -> Paid(reward, post(connection, userId, reward))
                }
            }
        }

    private fun post(
        connection: Connection,
        userId: Int,
        reward: Reward,
    ): Int =
        LedgerTransaction(connection)
            .post(
                LedgerEntry(
                    key = "income-reward:${reward.id}",
                    playerId = userId,
                    currency = reward.currency,
                    amount = reward.amount,
                    cause = "income_reward",
                    note = "category ${reward.category}",
                ),
            ).after

    private fun markDays(
        connection: Connection,
        userId: Int,
        lastGiftDay: Int,
        lastStatusDay: Int,
    ) {
        connection.write(MARK_DAYS) {
            int(userId)
            int(lastGiftDay)
            int(lastStatusDay)
        }
    }

    private fun deleteRows(
        connection: Connection,
        userId: Int,
        rewards: List<Reward>,
    ): List<Reward> =
        rewards.filter { reward ->
            connection.write(DELETE_REWARD) {
                int(reward.id)
                int(userId)
            } == 1
        }

    private fun readDays(
        connection: Connection,
        userId: Int,
    ): Pair<Int, Int> = connection.first(READ_DAYS, { int(userId) }) { it.getInt(1) to it.getInt(2) } ?: (0 to 0)

    private fun giftWaiting(
        connection: Connection,
        userId: Int,
    ): Boolean =
        connection.first(GIFT_WAITING, {
            int(userId)
            int(IncomeRewards.DAILY_GIFT)
        }) { true } ?: false

    private fun lockUser(
        connection: Connection,
        userId: Int,
    ) {
        connection.first(LOCK_PLAYER, { int(userId) }) { true }
            ?: throw SQLException("Unknown income reward user $userId")
    }

    private fun <T : Any> inTransaction(work: (Connection) -> T): T =
        dataSource.connection.use { connection -> checkNotNull(connection.allOrNothing { work(connection) }) }

    companion object {
        private const val LIST_REWARDS =
            "SELECT id, category, reward_type, amount, product_code FROM player_income_rewards " +
                "WHERE player_id = ? ORDER BY category, id"
        private const val DELETE_REWARD = "DELETE FROM player_income_rewards WHERE id = ? AND player_id = ?"
        private const val READ_DAYS = "SELECT last_day, last_status_day FROM player_daily_gifts WHERE player_id = ?"
        private const val GIFT_WAITING =
            "SELECT 1 FROM player_income_rewards WHERE player_id = ? AND category = ? LIMIT 1"
        private const val LOCK_PLAYER = "SELECT id FROM players WHERE id = ? FOR UPDATE"
        private const val MARK_DAYS =
            "INSERT INTO player_daily_gifts (player_id, last_day, last_status_day) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE last_day = VALUES(last_day), last_status_day = VALUES(last_status_day)"
        private const val INSERT_REWARD =
            "INSERT INTO player_income_rewards (player_id, category, reward_type, amount, product_code, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)"

        @Throws(SQLException::class)
        fun insert(
            connection: Connection,
            userId: Int,
            category: Int,
            type: Int,
            amount: Int,
            productCode: String,
            created: Int,
        ) {
            connection.write(INSERT_REWARD) {
                int(userId)
                int(category)
                int(type)
                int(amount)
                string(productCode)
                int(created)
            }
        }
    }
}
