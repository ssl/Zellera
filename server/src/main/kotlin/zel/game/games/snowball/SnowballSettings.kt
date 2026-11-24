@file:Suppress("MagicNumber") // the literals are the defaults of the settings

package zel.game.games.snowball

import zel.content.ClientStrings
import zel.core.Configuration
import zel.core.rules.rules
import zel.game.games.snowball.arena.ArenaRepository

/** Read from settings at each decision, so `:update_config` applies to the next lobby or join. */
data class SnowballSettings(
    val enabled: Boolean,
    val arenas: List<Int>,
    val minPlayers: Int,
    val lobbyCountdownSeconds: Int,
    val maxConcurrentGames: Int,
    val leaveBlockSeconds: Int,
    val assetsUrl: String,
    val stageSeconds: Int,
    val rematchSeconds: Int,
    val loadTimeoutSeconds: Int,
    val arenasFolder: String = "",
) {
    companion object {
        /** A client in a hidden tab gets about one frame a second and needs this long to build the stage. */
        const val DEFAULT_LOAD_TIMEOUT_SECONDS = 30

        /** Root-relative, resolved against the host the page runs on; empty without a client profile. */
        internal fun defaultAssetsUrl(): String =
            ClientStrings[ClientStrings.SNOWBALL_PATH]?.let { "/assets/$it/" }.orEmpty()

        fun defaults(): SnowballSettings =
            SnowballSettings(
                true,
                ArenaRepository.FIELD_TYPES,
                2,
                15,
                20,
                120,
                defaultAssetsUrl(),
                120,
                30,
                DEFAULT_LOAD_TIMEOUT_SECONDS,
            )

        fun from(config: Configuration): SnowballSettings {
            val defaults = defaults()
            return SnowballSettings(
                rules.snowball,
                parseArenas(config.getValue("snowball.arenas", "8,9,11")),
                config
                    .getInt(
                        "snowball.min_players",
                        defaults.minPlayers,
                    ).coerceAtMost(SnowballGame.MAX_PLAYERS)
                    .coerceAtLeast(1),
                maxOf(1, config.getInt("snowball.lobby.countdown.seconds", defaults.lobbyCountdownSeconds)),
                maxOf(1, config.getInt("snowball.max_games", defaults.maxConcurrentGames)),
                maxOf(0, config.getInt("snowball.rejoin_block.seconds", defaults.leaveBlockSeconds)),
                config.getValue("snowball.assets.url", "").ifBlank { defaults.assetsUrl },
                maxOf(1, config.getInt("snowball.stage.duration.seconds", defaults.stageSeconds)),
                maxOf(1, config.getInt("snowball.rematch.window.seconds", defaults.rematchSeconds)),
                maxOf(1, config.getInt("snowball.loading.timeout.seconds", defaults.loadTimeoutSeconds)),
                config.getValue("snowball.arenas.folder", ""),
            )
        }

        internal fun parseArenas(value: String): List<Int> =
            value.split(",").mapNotNull { it.trim().toIntOrNull() }.ifEmpty { ArenaRepository.FIELD_TYPES }
    }
}
