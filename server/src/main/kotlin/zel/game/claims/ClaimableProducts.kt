package zel.game.claims

import zel.db.Database

class ClaimableProducts(
    private val database: Database,
) {
    data class Choice(
        val badge: String,
        val furni: String,
        val endsAt: Int?,
    )

    fun choicesOf(claimCode: String): Map<Int, Choice> =
        database.queries
            .select(CHOICES, claimCode)
            .rows { row ->
                val endsAt = row.getLong("ends_at").takeUnless { row.wasNull() }?.toInt()
                val badge = row.getString("badge_code").orEmpty()
                val choice = Choice(badge, row.getString("furni_code").orEmpty(), endsAt)
                row.getInt("choice") to choice
            }.toMap()

    fun takenBy(
        playerId: Int,
        claimCode: String,
    ): Boolean =
        database.queries
            .select(TAKEN, playerId, claimCode)
            .first { true } ?: false

    fun take(
        playerId: Int,
        claimCode: String,
        choice: Int,
        at: Int,
    ): Boolean = database.queries.write(TAKE, playerId, claimCode, choice, at) > 0

    private companion object {
        const val CHOICES =
            "SELECT choice, badge_code, furni_code, ends_at FROM claimable_products WHERE claim_code = ?"
        const val TAKEN = "SELECT 1 FROM player_claimed_products WHERE player_id = ? AND claim_code = ?"
        const val TAKE =
            "INSERT IGNORE INTO player_claimed_products (player_id, claim_code, choice, claimed_at) VALUES (?, ?, ?, ?)"
    }
}
