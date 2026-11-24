package zel.game.moderation

import zel.db.intOrNone
import zel.game.items.OwnedFurni
import java.sql.ResultSet

/** A report on the moderators' desk; unset places hold -1 (forum place, moderator) or 0 (player, room). */
class SupportTicket(
    val type: TicketType,
    val createdAt: Int,
    val reporterId: Int,
    val reporterName: String?,
    val accusedId: Int,
    val accusedName: String?,
    val roomId: Int,
    val message: String,
) {
    var id = 0
    var state = TicketState.OPEN
    var category = openingTopic(type)

    /** The tool hands out the ticket with the lowest number first. */
    var priority = ROUTINE

    /** -1 while nobody picked it, 0 after the moderator released it. */
    var moderatorId = UNPICKED
    var moderatorName: String? = null
    var closedAt = 0
    var resolution: TicketResolution? = null

    val sanctioned: Boolean get() = resolution == TicketResolution.HANDLED

    val sanctionedAutomatically: Boolean get() = resolution == TicketResolution.AUTOMATIC

    var appeal = FiledReport.Appeal.NONE
    var appealResolvedAt = 0

    var guildId = NO_PLACE
    var forumThreadId = NO_PLACE
    var forumPostId = NO_PLACE
    var photo: OwnedFurni? = null

    var pickedLines: List<ChatlogLine>? = null

    /** [log] with the picked lines highlighted by speaker and text; a picked line missing from it is never shown. */
    fun markPickedIn(log: List<ChatlogLine>): List<ChatlogLine> {
        val picks = pickedLines.orEmpty()
        if (picks.isEmpty()) return log
        val wanted = picks.mapTo(HashSet()) { it.key() }
        return log.map { if (it.key() in wanted) it.asHighlighted() else it }
    }

    private fun ChatlogLine.key() = playerId to message

    companion object {
        const val UNPICKED = -1
        const val URGENT = 1
        const val ROUTINE = 10
        const val NO_PLACE = -1

        /** The mod tool's topic a report of [type] opens under; most reports start under topic 1. */
        private val OPENING_TOPICS =
            mapOf(
                TicketType.AUTOMATIC to 0,
                TicketType.GUIDE to 1024,
                TicketType.GUARDIAN to 1025,
            )

        fun openingTopic(type: TicketType?): Int = OPENING_TOPICS[type] ?: 1

        @Suppress("LongParameterList") // the facts of one report
        fun filed(
            createdAt: Int,
            reporterId: Int,
            reporterName: String?,
            accusedId: Int,
            accusedName: String?,
            roomId: Int,
            message: String,
            type: TicketType,
        ) = SupportTicket(type, createdAt, reporterId, reporterName, accusedId, accusedName, roomId, message)

        fun fromRow(
            row: ResultSet,
            loadPhoto: (Int) -> OwnedFurni?,
        ): SupportTicket {
            val type = TicketType.fromCode(row.getInt("kind"))
            val facts =
                SupportTicket(
                    type = type,
                    createdAt = row.getInt("created_at"),
                    reporterId = row.intOr("reporter_id", 0),
                    reporterName = row.getString("reporter_name"),
                    accusedId = row.intOr("accused_id", 0),
                    accusedName = row.getString("accused_name"),
                    roomId = row.intOr("room_id", 0),
                    message = row.getString("message").orEmpty(),
                )
            return facts.apply {
                id = row.getInt("id")
                state = TicketState.fromStored(row.getString("status"))
                priority = row.getInt("priority")
                category = row.intOr("topic_id", openingTopic(type))
                moderatorId = row.intOr("moderator_id", if (row.getBoolean("released")) 0 else UNPICKED)
                moderatorName = row.getString("moderator_name")
                closedAt = row.intOr("closed_at", 0)
                resolution = TicketResolution.fromStored(row.getString("resolution"))
                appeal = FiledReport.Appeal.fromStored(row.getString("appeal"))
                appealResolvedAt = row.intOr("appeal_resolved_at", 0)
                guildId = row.intOr("guild_id", NO_PLACE)
                forumThreadId = row.intOr("forum_thread_id", NO_PLACE)
                forumPostId = row.intOr("forum_post_id", NO_PLACE)
                photo = row.intOr("photo_furni_id", 0).takeIf { it > 0 }?.let(loadPhoto)
            }
        }

        private fun ResultSet.intOr(
            column: String,
            ifNull: Int,
        ): Int = intOrNone(column, ifNull)
    }
}
