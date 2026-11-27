package zel.game.webcommands

import zel.db.Params
import zel.db.write
import zel.game.players.inventory.KnownBadges
import java.sql.SQLException

/**
 * `giveBadge` / `takeBadge`: through the badge case when the player is online, on the table otherwise. Any badge the
 * game has may be given ([KnownBadges]).
 */
internal class BadgeWebCommands(
    private val services: WebCommandApplier.Services,
) {
    private enum class OfflineWrite(
        val statement: String,
    ) {
        GIVE(WebCommandApplier.GIVE_OFFLINE) {
            override fun Params.bind(
                userId: Int,
                code: String,
            ) {
                string(code)
                int(userId)
            }

            override fun changed(code: String) = WebCommandResult.done("Badge $code given.")

            override fun unchanged(code: String) = alreadyHas(code)
        },
        TAKE(WebCommandApplier.TAKE_OFFLINE) {
            override fun Params.bind(
                userId: Int,
                code: String,
            ) {
                int(userId)
                string(code)
            }

            override fun changed(code: String) = WebCommandResult.done("Badge $code taken away.")

            override fun unchanged(code: String) = lacks(code)
        },
        ;

        abstract fun Params.bind(
            userId: Int,
            code: String,
        )

        abstract fun changed(code: String): WebCommandResult

        abstract fun unchanged(code: String): WebCommandResult
    }

    @Throws(SQLException::class)
    fun give(
        command: WebCommand,
        payload: WebCommandPayload,
    ): WebCommandResult {
        val code = codeOf(payload) ?: return BAD_CODE
        if (!services.badgeExists(code)) return WebCommandResult.failed(KnownBadges.missing(code))
        val badges = badgeCase(command) ?: return offline(command.userId, code, OfflineWrite.GIVE)
        return when {
            badges.owns(code) -> alreadyHas(code)
            badges.award(code) == null -> WebCommandResult.failed("Not given.")
            else -> OfflineWrite.GIVE.changed(code)
        }
    }

    @Throws(SQLException::class)
    fun take(
        command: WebCommand,
        payload: WebCommandPayload,
    ): WebCommandResult {
        val code = codeOf(payload) ?: return BAD_CODE
        val badges = badgeCase(command) ?: return offline(command.userId, code, OfflineWrite.TAKE)
        val badge = badges.find(code) ?: return lacks(code)
        badges.discard(badge)
        return OfflineWrite.TAKE.changed(badge.code)
    }

    private fun offline(
        userId: Int,
        code: String,
        write: OfflineWrite,
    ): WebCommandResult =
        services.dataSource.connection.use { connection ->
            val changed =
                connection.write(write.statement) { with(write) { bind(userId, code) } } > 0
            when {
                changed -> {
                    services.invalidateBadgeCaches()
                    services.accountLevelChanged(userId)
                    write.changed(code)
                }

                !WebCommandApplier.userExists(connection, userId) -> {
                    WebCommandResult.failed("No user has id $userId.")
                }

                else -> {
                    write.unchanged(code)
                }
            }
        }

    private fun badgeCase(command: WebCommand) = services.online(command.userId)?.inventory?.badges

    private fun codeOf(payload: WebCommandPayload): String? =
        payload.string("badge")?.takeIf { WebCommandApplier.BADGE_CODE.matches(it) }

    private companion object {
        val BAD_CODE = WebCommandResult.failed("The payload needs a badge: 1 to 32 letters, digits or _.")

        fun alreadyHas(code: String) = WebCommandResult.failed("Already has the badge $code.")

        fun lacks(code: String) = WebCommandResult.failed("Does not have the badge $code.")
    }
}
