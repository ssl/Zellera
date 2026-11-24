package zel.protocol.discord

import zel.game.players.DiscordPreferences
import zel.protocol.generated.DiscordPreferencesOut
import zel.protocol.generated.DiscordPreferencesOutCodec
import zel.protocol.generated.GetDiscordPreferencesInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession

class DiscordPackets : PacketPackage {
    override val name = "discord"
    override val routes =
        listOf(
            PacketRoute(GetDiscordPreferencesInCodec) { session, _ ->
                val player = session.client.player ?: return@PacketRoute
                session.sendPreferences(player.stats.settings.discord)
            },
            PacketRoute(SetPreferencesCodec) { session, m ->
                val player = session.client.player ?: return@PacketRoute
                val preferences =
                    DiscordPreferences(
                        m.version.coerceAtLeast(0),
                        m.showPlayer,
                        m.shareActivity,
                        m.hideInHiddenRooms,
                        m.allowJoining,
                    )
                val settings = player.stats.settings
                if (preferences != settings.discord) settings.update(preferences)
                session.sendPreferences(preferences)
            },
        )

    private fun PacketSession.sendPreferences(preferences: DiscordPreferences) =
        with(preferences) {
            send(
                DiscordPreferencesOutCodec,
                DiscordPreferencesOut(version, showPlayer, shareActivity, hideInHiddenRooms, allowJoining),
            )
        }
}
