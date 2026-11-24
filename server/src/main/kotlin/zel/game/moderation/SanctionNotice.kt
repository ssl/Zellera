package zel.game.moderation

import zel.protocol.outgoing.users.BanInfo
import zel.util.SECONDS_PER_HOUR

/** What a sanctioned player is told; the reason falls back to the report topic's name when the moderator gave none. */
class SanctionNotice(
    private val text: (key: String, fallback: String) -> String,
    private val topic: (Int) -> ReportTopic?,
) {
    fun reason(
        given: String?,
        topicId: Int,
    ): String {
        val named = topic(topicId)?.displayName.orEmpty().trim()
        val own = given.orEmpty().trim()
        return own.ifEmpty { named }.ifEmpty { text(NO_REASON_KEY, NO_REASON) }
    }

    /** The ban window: `{expiryDate}` in the reason is filled in on display; without a topic the default layout. */
    fun ban(
        ban: PlayerBan,
        now: Int,
    ): BanInfo = BanInfo(ban.playerId, reason(ban.reason, ban.topicId), ban.endsAt - now, worded(ban))

    /** The ban window's own text for a sanction ban, empty for a ban without a topic. */
    fun worded(ban: PlayerBan): String =
        if (ban.topicId > 0) text(BAN_KEY, BAN).replace(REASON, reason(ban.reason, ban.topicId)) else ""

    fun mute(
        reason: String,
        seconds: Int,
    ): String =
        text(MUTE_KEY, MUTE)
            .replace(REASON, reason)
            .replace(HOURS, maxOf(1, (seconds + SECONDS_PER_HOUR - 1) / SECONDS_PER_HOUR).toString())

    companion object {
        /** Notices with the built-in wording, for callers without the texts table. */
        val PLAIN = SanctionNotice({ _, fallback -> fallback }) { null }

        private const val REASON = "%reason%"
        private const val HOURS = "%hours%"
        const val NO_REASON_KEY = "moderation.sanction.reason.none"
        const val BAN_KEY = "moderation.sanction.ban"
        const val MUTE_KEY = "moderation.sanction.mute"
        private const val NO_REASON = "breaking the rules"
        private const val BAN =
            "<b>You have been banned as a sanction.</b><br>Reason: %reason%<br><b>The ban ends:</b><br>{expiryDate}"
        private const val MUTE = "You have been muted for %hours% hour(s) as a sanction. Reason: %reason%"
    }
}
