package zel.game.economy

import java.security.SecureRandom
import java.util.Base64

/**
 * One change of one balance. The [key] names the event behind it: posting the same key again changes nothing.
 * An [actorId] is another player who caused the change; the player's own and the game's changes carry none.
 */
data class LedgerEntry(
    val key: String,
    val playerId: Int,
    val currency: Int,
    val amount: Int,
    val cause: String,
    val actorId: Int? = null,
    val furniId: Int? = null,
    val note: String = "",
) {
    init {
        require(isToken(key, MAX_KEY)) { "a ledger key is 1 to $MAX_KEY visible ASCII characters" }
        require(isToken(cause, MAX_CAUSE)) { "a ledger cause is 1 to $MAX_CAUSE visible ASCII characters" }
        require(playerId > 0) { "a ledger entry needs a player" }
        require(currency >= EconomyLedger.CREDITS) { "unknown currency $currency" }
        require(amount != 0) { "a ledger entry changes the balance" }
        require(actorId == null || actorId > 0) { "an actor is a player" }
        require(actorId != playerId) { "an actor is another player" }
        require(furniId == null || furniId > 0) { "a ledger entry's furni is a furni id" }
    }

    companion object {
        const val MAX_KEY = 64
        const val MAX_CAUSE = 64
        const val MAX_NOTE = 160
        private const val TOKEN_BYTES = 15
        private const val TOKEN_LENGTH = 20
        private const val MAX_SCOPE = MAX_KEY - TOKEN_LENGTH - 1
        private val random = SecureRandom()
        private val encoder = Base64.getUrlEncoder().withoutPadding()

        /** A key for an event that happens once and is never retried under the same name. */
        fun newKey(scope: String): String {
            require(isToken(scope, MAX_SCOPE)) { "a ledger key scope is 1 to $MAX_SCOPE visible ASCII characters" }
            val token = ByteArray(TOKEN_BYTES).also(random::nextBytes)
            return "$scope/${encoder.encodeToString(token)}"
        }

        /** Keys and causes are compared byte by byte in the database, so nothing in them may need trimming. */
        private fun isToken(
            value: String,
            maxLength: Int,
        ): Boolean = value.length in 1..maxLength && value.all { it in '!'..'~' }
    }
}
