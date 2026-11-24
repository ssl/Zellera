package zel.game.webcommands

import zel.db.allOrNothing
import zel.game.economy.Currencies
import zel.game.economy.EconomyLedger
import zel.game.economy.LedgerEntry
import zel.game.economy.UnknownLedgerPlayer
import zel.game.players.Player
import zel.game.players.WalletWrites
import zel.protocol.generated.ActivityPointNotificationOut
import zel.protocol.generated.ActivityPointNotificationOutCodec
import zel.protocol.outgoing.users.UserCredits
import zel.protocol.outgoing.users.UserCurrency
import java.sql.Connection
import java.sql.SQLException

/** An online user gets the wallet change under the wallet's lock, database first. */
internal class WalletWebCommands(
    private val services: WebCommandApplier.Services,
) {
    private data class Change(
        val before: Int,
        val after: Int,
        val applied: Boolean,
        val refusal: String? = null,
    )

    @Suppress("ReturnCount") // one early answer per refusal
    @Throws(SQLException::class)
    fun apply(
        command: WebCommand,
        payload: WebCommandPayload,
        set: Boolean,
    ): WebCommandResult {
        val currency = payload.int("currency")
        val amount = payload.int("amount")
        if (currency == null || amount == null) return WebCommandResult.failed(NEEDS_NUMBERS)
        problemWith(currency, amount, set)?.let { return WebCommandResult.failed(it) }

        val name = currencyName(currency)
        val (change, shown) = change(command, currency, set, amount, name)
        return when {
            change.refusal != null -> {
                WebCommandResult.failed(change.refusal)
            }

            !change.applied -> {
                WebCommandResult.done("$name unchanged: already ${change.before}.")
            }

            else -> {
                if (shown != null) showBalance(shown, currency, change.after, change.after - change.before)
                WebCommandResult.done("$name ${change.before} -> ${change.after}.")
            }
        }
    }

    private fun problemWith(
        currency: Int,
        amount: Int,
        set: Boolean,
    ): String? =
        when {
            currency != EconomyLedger.CREDITS && currency !in services.pointTypes() -> {
                "This game has no currency $currency."
            }

            set && amount < 0 -> {
                "A balance can not be below 0."
            }

            !set && amount == 0 -> {
                "Adding 0 changes nothing."
            }

            else -> {
                null
            }
        }

    private fun change(
        command: WebCommand,
        currency: Int,
        set: Boolean,
        amount: Int,
        name: String,
    ): Pair<Change, Player?> {
        val online = services.online(command.userId)
        if (online != null) {
            val done =
                WalletWrites.locked(online) {
                    ledger(command, currency, set, amount, name).also {
                        if (it.applied) WalletWrites.publish(online, currency, it.after)
                    }
                }
            return done to online
        }

        val done = ledger(command, currency, set, amount, name)
        // logged in while this was written: the server may have loaded the old balance
        val now = if (done.applied) services.online(command.userId) else null
        if (now != null) {
            WalletWrites.locked(now) {
                WalletWrites.publish(now, currency, done.after)
            }
        }
        return done to now
    }

    private fun ledger(
        command: WebCommand,
        currency: Int,
        set: Boolean,
        amount: Int,
        name: String,
    ): Change =
        services.dataSource.connection.use { connection ->
            var change: Change? = null
            // only an applied change commits; a refusal or no change rolls back
            connection.allOrNothing {
                lockedChange(
                    connection,
                    command,
                    currency,
                    set,
                    amount,
                    name,
                ).also { change = it }.takeIf { it.applied }
            }
            checkNotNull(change)
        }

    @Suppress("ReturnCount") // one early answer per refusal
    private fun lockedChange(
        connection: Connection,
        command: WebCommand,
        currency: Int,
        set: Boolean,
        amount: Int,
        name: String,
    ): Change {
        val before =
            try {
                services.ledger.lockedBalance(connection, command.userId, currency)
            } catch (_: UnknownLedgerPlayer) {
                return Change(0, 0, false, "No user has id ${command.userId}.")
            }
        val target = if (set) amount.toLong() else before.toLong() + amount
        val refusal =
            when {
                target < 0 -> "$name: $before $amount would be below 0."
                target > Int.MAX_VALUE -> "$name: $before + $amount would be above ${Int.MAX_VALUE}."
                else -> null
            }
        if (refusal != null) return Change(0, 0, false, refusal)

        val delta = (target - before).toInt()
        if (delta == 0) return Change(before, before, false)
        val result = services.ledger.apply(connection, operation(command, currency, delta))
        return Change(result.before, result.after, true)
    }

    private fun operation(
        command: WebCommand,
        currency: Int,
        delta: Int,
    ) = LedgerEntry(
        key = "web_command:${command.id}",
        playerId = command.userId,
        currency = currency,
        amount = delta,
        cause = "web_command.${command.type}",
        actorId = command.createdBy.takeIf { it != command.userId },
    )

    private fun showBalance(
        player: Player,
        currency: Int,
        balance: Int,
        delta: Int,
    ) {
        val client = player.client ?: return
        when (currency) {
            EconomyLedger.CREDITS -> {
                client.send(UserCredits(player))
            }

            EconomyLedger.POINTS -> {
                client.send(UserCurrency(player))
            }

            else -> {
                client.send(
                    ActivityPointNotificationOutCodec,
                    ActivityPointNotificationOut(balance, delta, currency),
                )
            }
        }
    }

    internal companion object {
        private const val NEEDS_NUMBERS = "The payload needs currency and amount as whole numbers."

        fun currencyName(currency: Int): String = Currencies.name(currency)
    }
}
