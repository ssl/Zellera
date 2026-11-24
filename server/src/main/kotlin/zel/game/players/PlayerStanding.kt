package zel.game.players

import java.sql.ResultSet

class PlayerStanding internal constructor(
    private val now: () -> Int,
) {
    var maxFriends = 0
    var maxRooms = 0
    var mayChangeName = false

    /** The name last checked, the only one the player may then take. */
    var checkedNewName = ""
    var safetyLocked = false

    var welcomeGiftTaken = false

    var tourStep = 1

    var mutedUntil = 0
        private set

    @Volatile
    var tradePass = true

    @Volatile
    var tradeLockedUntil = 0
        private set

    @Synchronized
    fun mute(seconds: Int): Int {
        mutedUntil = maxOf(mutedUntil, now()) + seconds
        return muteSecondsLeft()
    }

    fun muteSecondsLeft(): Int = maxOf(0, mutedUntil - now())

    fun mayTalk(): Boolean = muteSecondsLeft() == 0

    @Synchronized
    fun unmute() {
        mutedUntil = 0
    }

    fun mayTrade(): Boolean = mayTrade(tradePass, tradeLockedUntil, now())

    /** A lock only ever grows. */
    @Synchronized
    fun lockTrade(until: Int) {
        tradeLockedUntil = maxOf(tradeLockedUntil, until)
    }

    internal fun read(row: ResultSet) {
        safetyLocked = row.getBoolean("safety_locked")
        tradePass = row.getBoolean("can_trade")
        tradeLockedUntil = row.getInt("trade_locked_until")
        mutedUntil = row.getInt("muted_until")
        mayChangeName = row.getBoolean("may_change_name")
        welcomeGiftTaken = row.getBoolean("welcome_gift_taken")
        maxRooms = row.getInt("max_rooms")
        maxFriends = row.getInt("max_friends")
    }

    companion object {
        /** A trade lock runs while its end is still ahead; one that has run out (until <= now) is over. */
        fun lockedAt(
            lockedUntil: Int,
            now: Int,
        ): Boolean = lockedUntil > now

        /**
         * The one rule for "may this player trade now": the trade pass, and no trade lock running at [now]. Room
         * trades, the marketplace and wired chests all ask it, online from the standing, offline from its row.
         */
        fun mayTrade(
            tradePass: Boolean,
            lockedUntil: Int,
            now: Int,
        ): Boolean = tradePass && !lockedAt(lockedUntil, now)
    }
}
