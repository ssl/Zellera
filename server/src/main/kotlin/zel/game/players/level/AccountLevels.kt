package zel.game.players.level

import zel.game.earnings.IncomeRewards
import zel.game.earnings.LevelProgressionEarnings
import zel.game.players.Player
import org.slf4j.LoggerFactory
import java.sql.SQLException
import java.util.concurrent.ConcurrentHashMap

private val log = LoggerFactory.getLogger(AccountLevels::class.java)

class AccountLevels(
    private val store: AccountLevelStore,
    private val rewards: () -> IncomeRewards?,
    private val online: (Int) -> Player?,
    private val schedule: (Runnable, Long) -> Unit,
    hostingDivisor: () -> Int,
) {
    private val standings = AccountStandings(store, hostingDivisor)
    private val locks = Array(LOCK_STRIPES) { Any() }
    private val waiting = ConcurrentHashMap.newKeySet<Int>()

    fun recount(playerId: Int): AccountStanding =
        synchronized(lockOf(playerId)) {
            val counted = standings.recounted(playerId)
            val rewardedUpTo =
                if (counted.level > counted.rewardedLevel) {
                    rewards()?.let {
                        LevelProgressionEarnings.levelsReached(it, playerId, counted.rewardedLevel, counted.level)
                    } ?: counted.level
                } else {
                    counted.rewardedLevel
                }
            val paid =
                if (rewardedUpTo > counted.rewardedLevel) {
                    counted.copy(rewardedLevel = rewardedUpTo).also(store::save)
                } else {
                    counted
                }
            online(playerId)?.let { player ->
                val changed = player.accountLevel != paid.level
                player.accountLevel = paid.level
                if (changed) player.rooms.cardChanged(playerId)
            }
            paid
        }

    fun inputChanged(playerId: Int) {
        if (playerId <= 0 || !waiting.add(playerId)) return
        schedule(
            {
                waiting.remove(playerId)
                recountLogged(playerId)
            },
            RECOUNT_DELAY_MS,
        )
    }

    fun visitEnded(
        ownerId: Int,
        visitorLevel: Int,
        minutes: Int,
    ) {
        if (ownerId <= 0 || minutes <= 0) return
        try {
            synchronized(lockOf(ownerId)) {
                standings.of(ownerId)
                store.addHosting(ownerId, minutes.toLong() * visitorLevel.coerceAtLeast(1))
                recount(ownerId)
            }
        } catch (e: SQLException) {
            log.error("Could not count a visit for player {}", ownerId, e)
        }
    }

    fun recountLogged(playerId: Int): AccountStanding? =
        try {
            recount(playerId)
        } catch (e: SQLException) {
            log.error("Could not count the level of player {}", playerId, e)
            null
        }

    private fun lockOf(playerId: Int): Any = locks[Math.floorMod(playerId, LOCK_STRIPES)]

    private companion object {
        const val LOCK_STRIPES = 64
        const val RECOUNT_DELAY_MS = 1000L
    }
}
