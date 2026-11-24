// Appeal states are plain wire numbers.
@file:Suppress("MagicNumber")

package zel.game.moderation

/** Times are epoch milliseconds; closedAt and appealResolvedAt are -1 while a decision is pending. */
data class FiledReport(
    val id: Int,
    val createdAt: Long,
    val message: String,
    val category: Int,
    val reportedName: String,
    val closedAt: Long,
    val sanctioned: Boolean,
    val sanctionedAutomatically: Boolean,
    val appeal: Appeal,
    val appealedAt: Long,
    val appealResolvedAt: Long,
) {
    /** [code] is the wire number, [stored] the table's word. */
    enum class Appeal(
        val code: Int,
        val stored: String,
    ) {
        NONE(0, "none"),
        PENDING(1, "pending"),
        ACTED_ON(2, "upheld"),
        NOT_ACTED_ON(3, "rejected"),
        ;

        companion object {
            fun fromStored(stored: String?): Appeal = entries.firstOrNull { it.stored == stored } ?: NONE
        }
    }

    /** Once, against a closing that did nothing for the reporter. */
    val appealable: Boolean get() = appeal == Appeal.NONE && closedAt != -1L && !sanctioned
}
