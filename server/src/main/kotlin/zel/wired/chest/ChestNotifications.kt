package zel.wired.chest

import zel.protocol.generated.SetChestNotificationSettingsIn

data class ChestNotifications(
    val full: Boolean = true,
    val donation: Boolean = true,
    val withdraw: Boolean = true,
    val empty: Boolean = true,
    val wired: Boolean = true,
    val mode: Int = ALWAYS,
) {
    val onlyWhileAway: Boolean get() = mode == OWNER_AWAY

    fun wants(notice: ChestNotice): Boolean =
        when (notice) {
            ChestNotice.FULL -> full
            ChestNotice.DONATION -> donation
            ChestNotice.WITHDRAW -> withdraw
            ChestNotice.EMPTY -> empty
            ChestNotice.WIRED -> wired
            ChestNotice.EARNINGS -> true
        }

    companion object {
        const val ALWAYS = 0
        const val OWNER_AWAY = 1

        fun of(m: SetChestNotificationSettingsIn): ChestNotifications =
            ChestNotifications(
                full = m.full,
                donation = m.donation,
                withdraw = m.withdraw,
                empty = m.empty,
                wired = m.wired,
                mode = if (m.mode == OWNER_AWAY) OWNER_AWAY else ALWAYS,
            )
    }
}
