package zel.protocol.handshake

import zel.game.navigation.NavigatorSavedSearch
import zel.game.players.Player

object StarterSearches {
    val CODES: List<String> = listOf("official-root", "my", "favorites")

    /** True when this login is the one that hands them out. */
    fun handOut(player: Player): Boolean {
        val settings = player.stats.settings
        if (settings.starterSearchesGiven) return false
        settings.starterSearchesGiven = true
        val profile = player.profile
        // two sessions that both read the flag unset add each missing search only once
        CODES.filterNot { profile.hasSearch(it, "") }.forEach { profile.saveSearch(NavigatorSavedSearch(it, "")) }
        return true
    }
}
