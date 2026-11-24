package zel.protocol.incoming.moderation

import zel.game.moderation.SanctionKind
import zel.game.permissions.Privilege
import zel.game.players.Player

internal object ModToolRights {
    /** Any of these opens the sanction window. */
    val SANCTION_WINDOW =
        listOf(
            Privilege.MODERATION_PLAYER_ALERT,
            Privilege.MODERATION_PLAYER_KICK,
            Privilege.MODERATION_PLAYER_MUTE,
            Privilege.MODERATION_PLAYER_TRADE_LOCK,
            Privilege.MODERATION_PLAYER_BAN,
        )

    /** A ladder step that mutes or bans needs that right; an alert needs none beyond the window's. */
    fun mayGive(
        player: Player,
        kind: SanctionKind?,
    ): Boolean =
        when (kind) {
            SanctionKind.MUTE -> player.hasPermission(Privilege.MODERATION_PLAYER_MUTE)
            SanctionKind.BAN -> player.hasPermission(Privilege.MODERATION_PLAYER_BAN)
            else -> true
        }

    fun allows(
        player: Player,
        anyOf: Collection<Privilege>,
    ): Boolean =
        player.hasPermission(Privilege.MODERATION_TOOL) && (anyOf.isEmpty() || anyOf.any(player::hasPermission))
}
