package zel.game.webcommands

import zel.db.write
import zel.game.players.Mottos
import zel.protocol.generated.ExtendedProfileChangedOut
import zel.protocol.generated.ExtendedProfileChangedOutCodec
import zel.protocol.outgoing.rooms.users.UserChange
import java.sql.SQLException

internal class ProfileWebCommands(
    private val services: WebCommandApplier.Services,
) {
    // Guard clauses; Players.changeRank (behind Ranks.set) can fail with any exception.
    @Suppress("ReturnCount", "TooGenericExceptionCaught")
    @Throws(SQLException::class)
    fun setRank(
        command: WebCommand,
        payload: WebCommandPayload,
    ): WebCommandResult {
        val level = payload.int("level") ?: return WebCommandResult.failed("The payload needs level (a whole number).")
        val ranks = services.ranks
        if (!ranks.exists(level)) return WebCommandResult.failed("There is no level $level.")
        val before = ranks.stored(command.userId) ?: return WebCommandResult.failed("No user has id ${command.userId}.")
        WebCommandApplier.senderRefusal(ranks, command, WebCommandApplier.SET_RANK)?.let {
            return WebCommandResult.failed(it)
        }
        if (before == level) return WebCommandResult.done("Unchanged: level $level.")
        val sender = command.createdBy?.let { ranks.stored(it) }
        rankRefusal(sender ?: 0, before, level)?.let { return WebCommandResult.failed(it) }

        return try {
            ranks.set(command.userId, level, command.createdBy)
            WebCommandResult.done("level $before -> $level.")
        } catch (e: SQLException) {
            throw e
        } catch (e: Exception) {
            WebCommandResult.failed("Not changed: ${e.message}")
        }
    }

    @Suppress("ReturnCount") // one early answer per refusal
    @Throws(SQLException::class)
    fun setMotto(
        command: WebCommand,
        payload: WebCommandPayload,
    ): WebCommandResult {
        val typed = payload.string("motto") ?: return WebCommandResult.failed("The payload needs a motto.")
        if (typed.length > MOTTO_MAX_SENT) return WebCommandResult.failed("That motto is far too long.")
        val max = services.mottoMaxLength()
        val motto =
            Mottos.accepted(typed, services.wordFilter, max)
                ?: return WebCommandResult.failed("With the word filter it is longer than $max characters.")

        services.dataSource.connection.use { connection ->
            val changed =
                connection.write(Mottos.STORE_SQL) {
                    string(motto)
                    int(command.userId)
                }
            if (changed == 0 && !WebCommandApplier.userExists(connection, command.userId)) {
                return WebCommandResult.failed("No user has id ${command.userId}.")
            }
        }

        showMotto(command.userId, motto)
        return WebCommandResult.done(
            if (motto == typed.trim()) "Motto saved." else "Motto saved; the word filter changed it.",
        )
    }

    private fun showMotto(
        userId: Int,
        motto: String,
    ) {
        val player = services.online(userId) ?: return
        player.profile.motto = motto
        val client = player.client
        val rooms = services.rooms()
        if (rooms.roomOf(userId) != null) {
            rooms.appearanceChanged(userId)
        } else {
            client?.send(UserChange(player))
        }
        // a profile open on the user's screen still shows the old motto
        client?.send(ExtendedProfileChangedOutCodec, ExtendedProfileChangedOut(userId))
    }

    internal companion object {
        private const val MOTTO_MAX_SENT = 255

        fun rankRefusal(
            senderLevel: Int,
            before: Int,
            level: Int,
        ): String? =
            when {
                before > senderLevel -> "A higher rank than yours."
                level >= senderLevel -> "Only a level below yours."
                else -> null
            }
    }
}
