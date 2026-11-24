package zel.game.players.inventory

data class AvatarEffect(
    val id: Int,
    val playerId: Int,
    val duration: Int = TEMPORARY_SECONDS,
    val activatedAt: Int = NOT_ACTIVATED,
    val copies: Int = 1,
) {
    val isActivated: Boolean get() = activatedAt >= 0

    val isPermanent: Boolean get() = duration <= 0

    val isRunning: Boolean get() = !isPermanent && isActivated

    /** One more copy of [duration]: a permanent copy takes over; only a permanent effect takes none. */
    fun addCopy(duration: Int): AvatarEffect? =
        when {
            isPermanent -> null
            duration <= 0 -> copy(duration = PERMANENT, copies = 1, activatedAt = NOT_ACTIVATED)
            else -> copy(copies = copies + 1)
        }

    fun wornFrom(now: Int): AvatarEffect = if (isActivated) this else copy(activatedAt = now)

    fun secondsLeft(now: Int): Int =
        when {
            isPermanent -> Int.MAX_VALUE
            !isActivated -> duration
            else -> maxOf(0, activatedAt + duration - now)
        }

    /** A running copy whose time is up is spent: one copy less, and the next copy waits to be worn. */
    fun spentBy(now: Int): AvatarEffect =
        if (isRunning && secondsLeft(now) == 0) {
            copy(copies = maxOf(0, copies - 1), activatedAt = NOT_ACTIVATED)
        } else {
            this
        }

    fun secondsRunning(now: Int): Int = if (isRunning) secondsLeft(now) else -1

    fun idleCopies(): Int = if (isPermanent) 1 else maxOf(0, copies - if (isRunning) 1 else 0)

    companion object {
        const val NOT_ACTIVATED = -1
        const val TEMPORARY_SECONDS = 86400
        const val PERMANENT = 0
    }
}
