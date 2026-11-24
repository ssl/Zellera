package zel.wired.chest

import zel.protocol.generated.StuffData

object ChestStuff {
    private const val MAP_FORMAT = 1

    fun of(
        chest: Chest,
        state: String,
    ): StuffData =
        StuffData(
            MAP_FORMAT,
            StuffData.Value.MapData(
                buildList {
                    val look = chest.preferences
                    val notify = chest.notifications
                    add("state" to state)
                    add("chest_name" to look.name)
                    add("chest_desc" to look.description)
                    add("locked" to flag(chest.locked))
                    add("auto_lock" to flag(chest.autoLock))
                    add("capacity" to chest.capacity.toString())
                    if (chest.placed || chest.contents > 0) add("contents_count" to chest.contents.toString())
                    add("capacity_level" to chest.level.toString())
                    add("everyone_can_open" to flag(look.everyoneOpens))
                    add("everyone_can_donate" to flag(look.everyoneDonates))
                    add("state_control_mode" to look.lid.toString())
                    add("notify_mode" to notify.mode.toString())
                    add("is_wired_enabled" to flag(look.wiredEnabled))
                    if (!chest.isCoins) {
                        add("preview_mode" to look.previewMode.toString())
                        add("preview_amount" to look.previewAmount.toString())
                        add("visuals" to "")
                    }
                    add("notification_chest_full" to flag(notify.full))
                    add("notification_donation" to flag(notify.donation))
                    add("notification_someone_withdraws" to flag(notify.withdraw))
                    add("notification_chest_empty" to flag(notify.empty))
                    add("notification_wired_transaction" to flag(notify.wired))
                },
            ),
        )

    private fun flag(on: Boolean): String = if (on) "1" else "0"
}
