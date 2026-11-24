package zel.game.games.snowball

import zel.game.games.snowball.sim.SyncArena

/** Snowman defence (mode 2) is not played. */
enum class SnowballMode(
    val id: Int,
    val numberOfTeams: Int,
) {
    TEAM(SyncArena.MODE_TEAM, 2),
    DEATHMATCH(SyncArena.MODE_DEATHMATCH, 1),
    ;

    val isDeathmatch: Boolean get() = this == DEATHMATCH

    companion object {
        fun of(id: Int): SnowballMode? = entries.firstOrNull { it.id == id }
    }
}
