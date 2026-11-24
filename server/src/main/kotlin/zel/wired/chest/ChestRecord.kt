package zel.wired.chest

data class ChestRecord(
    val preferences: ChestPreferences,
    val notifications: ChestNotifications,
    val locked: Boolean,
    val autoLock: Boolean,
    val capacity: Int,
    val capacityMax: Int,
    val placed: Boolean,
    val credits: Int,
    val furni: List<StoredFurni>,
) {
    fun toChest(
        id: Int,
        type: ChestType,
    ): Chest =
        Chest(id, type).also { chest ->
            chest.preferences = preferences
            chest.notifications = notifications
            chest.locked = locked
            chest.autoLock = autoLock
            chest.capacityMax = capacityMax
            chest.capacity = capacity
            chest.placed = placed
            chest.credits = credits
            chest.furni += furni
        }

    companion object {
        fun of(chest: Chest): ChestRecord =
            ChestRecord(
                preferences = chest.preferences,
                notifications = chest.notifications,
                locked = chest.locked,
                autoLock = chest.autoLock,
                capacity = chest.capacity,
                capacityMax = chest.capacityMax,
                placed = chest.placed,
                credits = chest.credits,
                furni = chest.furni.toList(),
            )
    }
}
