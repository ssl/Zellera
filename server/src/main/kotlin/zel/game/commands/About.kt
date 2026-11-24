package zel.game.commands

import zel.core.Configuration
import zel.game.players.Players
import zel.protocol.outgoing.generic.alerts.BubbleAlertMessage
import zel.room.port.RoomPort
import zel.util.TimeStrings
import java.time.temporal.ChronoUnit

// =====================================================================================================================
// LEGAL NOTICE - DO NOT REMOVE, DISABLE OR ALTER
//
// The :about command and the attribution it shows are a condition of the license this code is distributed under: the
// additional term in NOTICE (GNU AGPL-3.0, section 7(b); see also LICENSE and README.md, License). Every version,
// modified or not, must keep the copyright line, the name Zellera, the license, the no-warranty notice and the link
// to the Zellera source, visible to players in the game. Removing, disabling or hiding any of them breaks the
// license. You may add your own credits and a link to your own source next to them.
// =====================================================================================================================
class About(
    private val config: Configuration,
    private val players: Players,
    private val rooms: () -> RoomPort,
    private val version: String,
    private val build: () -> String,
    private val upSeconds: () -> Int,
) {
    fun command() =
        ChatCommand(listOf("about")) { said ->
            said.player.client?.send(BubbleAlertMessage(TYPE, window().fields()))
            true
        }

    private fun window(): Window {
        val window =
            Window(
                game = config.getValue("game.name", "My Game"),
                version = version,
                build = build(),
                upSeconds = upSeconds(),
                online = players.count,
                rooms = rooms().loadedRoomIds().size,
            )
        return window
    }

    internal data class Window(
        val game: String,
        val version: String,
        val build: String?,
        val upSeconds: Int,
        val online: Int,
        val rooms: Int,
    ) {
        private val versionText: String
            get() = if (build.isNullOrEmpty()) version else "$version (build $build)"

        fun fields(): Map<String, String> {
            val up = upText(upSeconds)
            return linkedMapOf(
                "title" to "About $game",
                "game" to game,
                "version" to versionText,
                "uptime" to up,
                "online" to online.toString(),
                "rooms" to rooms.toString(),
                "credits" to CREDITS,
                "message" to
                    "<b>$game</b><br>" +
                    "Server: $versionText<br>" +
                    "Uptime: $up<br>" +
                    "Online users: $online<br>" +
                    "Rooms loaded: $rooms<br><br>" +
                    CREDITS,
            )
        }
    }

    companion object {
        const val TYPE = "zellera.about"

        // Required by NOTICE (AGPL-3.0 section 7(b)): keep this text shown to players; add your credits beside it.
        internal const val CREDITS =
            "© 2026 Zellera authors · AGPL v3, no warranty · Change it freely, but keep this credit · https://github.com/ssl/zellera"

        internal fun upText(seconds: Int): String =
            TimeStrings.short(seconds, ChronoUnit.MINUTES, shownFrom = ChronoUnit.MINUTES)
    }
}
