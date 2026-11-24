package zel.game.players.level

import zel.db.Database

class AccountStandings(
    private val store: AccountLevelStore,
    private val hostingDivisor: () -> Int,
) {
    constructor(database: Database) : this(JdbcAccountLevelStore(database.dataSource), { defaultDivisor() })

    fun of(playerId: Int): AccountStanding =
        store.read(playerId) ?: AccountStanding.first(playerId, store.inputs(playerId)).let { first ->
            first.recounted(first.inputs, hostingDivisor()).also(store::save)
        }

    fun recounted(playerId: Int): AccountStanding {
        val inputs = store.inputs(playerId)
        val before = store.read(playerId) ?: AccountStanding.first(playerId, inputs)
        return before.recounted(inputs, hostingDivisor()).also(store::save)
    }

    private companion object {
        fun defaultDivisor(): Int = zel.core.rules.rules.accountLevelHostingDivisor
    }
}
