package zel.game.catalog

import zel.game.players.PlayerStats

/** One purchase at a time per player: a second request while one runs is refused instead of queued. */
object PurchaseGate {
    fun enter(stats: PlayerStats): Boolean =
        synchronized(stats) {
            if (stats.isPurchasingFurniture) return false
            stats.isPurchasingFurniture = true
            true
        }

    fun leave(stats: PlayerStats) {
        synchronized(stats) { stats.isPurchasingFurniture = false }
    }
}
