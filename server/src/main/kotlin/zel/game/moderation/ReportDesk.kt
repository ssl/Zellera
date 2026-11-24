package zel.game.moderation

/** Who looks at a report; [NOBODY] closes it with the topic's reply. [stored] is the `report_topics.goes_to` value. */
enum class ReportDesk(
    val stored: String,
) {
    MODERATORS("moderators"),
    GUARDIANS("guardians"),
    NOBODY("nobody"),
    ;

    companion object {
        fun ofStored(value: String?): ReportDesk = entries.firstOrNull { it.stored == value } ?: MODERATORS
    }
}
