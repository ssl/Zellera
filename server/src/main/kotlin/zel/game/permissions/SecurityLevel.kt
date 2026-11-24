package zel.game.permissions

import zel.game.players.Player
import zel.game.players.PlayerProfile

/** Security levels; a rank's id is its level and each level has what the levels below it have. */
object SecurityLevel {
    const val NONE = 0

    const val VIP = 2

    const val AMBASSADOR = 3

    const val EMPLOYEE = 4

    const val MODERATOR = 5

    const val SUPER_USER = 9

    const val VIP_BADGE = "VIP"

    fun of(player: Player?): Int = of(player?.profile)

    fun of(info: PlayerProfile?): Int = info?.rank?.id ?: NONE

    fun has(
        player: Player?,
        level: Int,
    ): Boolean = of(player) >= level

    fun sendsAmbassador(level: Int): Boolean = level == AMBASSADOR

    fun ambassador(player: Player?): Boolean =
        player?.profile != null && player.hasPermission(Privilege.AMBASSADOR_TOOLS)

    fun isVipBadge(code: String?): Boolean = VIP_BADGE.equals(code, ignoreCase = true)
}
