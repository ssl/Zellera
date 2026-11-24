package zel.protocol.outgoing.users

import zel.game.players.Player
import zel.protocol.generated.AccountPreferencesOut
import zel.protocol.generated.AccountPreferencesOutCodec
import zel.protocol.outgoing.OutgoingMessage

class MeMenuSettings(
    val player: Player,
) : OutgoingMessage<AccountPreferencesOut>(AccountPreferencesOutCodec) {
    override fun message(): AccountPreferencesOut {
        val stats = player.stats
        return AccountPreferencesOut(
            int1 = stats.settings.volumes.system,
            int2 = stats.settings.volumes.furni,
            int3 = stats.settings.volumes.trax,
            boolean1 = stats.settings.classicChat,
            boolean2 = stats.settings.noRoomInvites,
            boolean3 = stats.settings.noCameraFollow,
            int4 = stats.settings.clientFlags,
            int5 = stats.settings.bubble.type,
            boolean4 = stats.settings.wiredMenu.menuButton,
            boolean5 = stats.settings.wiredMenu.inspectButton,
            boolean6 = stats.settings.wiredMenu.playtest,
            int6 = 0,
            boolean7 = stats.settings.wiredWhisperOff,
            trailing1 = true,
            trailing2 = "",
            trailing3 = 0,
            trailing4 = stats.settings.chatLayout.mode,
            trailing5 = stats.settings.chatLayout.bubbleWidth,
            trailing6 = stats.settings.chatLayout.scrollSpeed,
            trailing7 = stats.settings.onlineIndicator,
        )
    }
}
