package zel.game.moderation

/**
 * A report reason. [urgent] puts its tickets ahead of the others on the moderators' desk; [ignoresAccused] makes the
 * reporter stop seeing the accused when the report closes itself.
 */
data class ReportTopic(
    val id: Int,
    val name: String,
    val goesTo: ReportDesk,
    val reply: String,
    val replyWithoutLink: Boolean,
    val defaultSanction: SanctionPreset?,
    val displayName: String = "",
    val urgent: Boolean = false,
    val ignoresAccused: Boolean = false,
) {
    val closesItself: Boolean get() = goesTo == ReportDesk.NOBODY

    /** The word the report form is given for the topic. */
    val consequence: String
        get() =
            when {
                closesItself -> "auto_reply"
                urgent -> "self_harm_notification"
                else -> goesTo.stored
            }
}
