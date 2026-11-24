package zel.protocol.handshake

import zel.game.players.PlayerProfile

/** A real noob on the account's first session (noob lobby, new-identity navigator tab), established after that. */
internal object NoobnessLevel {
    const val ESTABLISHED = 0
    const val REAL_NOOB = 2

    fun of(profile: PlayerProfile): Int = if (profile.firstVisit) REAL_NOOB else ESTABLISHED
}
