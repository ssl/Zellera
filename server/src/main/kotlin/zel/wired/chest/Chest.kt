package zel.wired.chest

class Chest(
    val id: Int,
    val type: ChestType,
) {
    var preferences = ChestPreferences()
    var notifications = ChestNotifications()
    var locked = true
    var autoLock = false

    /** The ceiling upgrades allow; [capacity] is the owner's own limit under it. */
    var capacityMax = type.baseCapacity
    var capacity = type.baseCapacity

    var placed = false
    var credits = 0
    val furni = ArrayList<StoredFurni>()

    val wiredEnabled: Boolean get() = preferences.wiredEnabled

    val isCoins: Boolean get() = type.kind == ChestKind.COINS

    val contents: Int get() = if (isCoins) credits else furni.size

    val level: Int get() = if (type.step == 0) 0 else (capacityMax - type.baseCapacity) / type.step

    fun setUp(starterTexts: () -> Pair<String, String>) {
        placed = true
        preferences = preferences.setUp(type.starter, starterTexts)
    }

    fun lid(viewed: Boolean): String {
        val open =
            when (preferences.lid) {
                ChestPreferences.LID_ALWAYS_OPEN -> true
                ChestPreferences.LID_CLOSED -> false
                else -> viewed
            }
        return when {
            !open -> "0"
            !isCoins || credits <= 0 -> "1"
            else -> coinLevel().toString()
        }
    }

    private fun coinLevel(): Int {
        val percent = credits.toLong() * PERCENT / capacity.coerceAtLeast(1)
        return when {
            percent < HALF -> LEVEL_LOW
            percent < THREE_QUARTERS -> LEVEL_MIDDLE
            else -> LEVEL_HIGH
        }
    }

    private companion object {
        const val PERCENT = 100L
        const val HALF = 50L
        const val THREE_QUARTERS = 75L
        const val LEVEL_LOW = 2
        const val LEVEL_MIDDLE = 3
        const val LEVEL_HIGH = 4
    }
}
