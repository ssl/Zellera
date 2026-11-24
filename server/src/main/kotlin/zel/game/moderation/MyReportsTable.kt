package zel.game.moderation

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.orFallback
import zel.util.MILLIS_PER_SECOND
import java.sql.ResultSet
import javax.sql.DataSource

class MyReportsTable(
    dataSource: DataSource,
) {
    private val sql = Sql(dataSource)

    fun ofReporter(playerId: Int): List<FiledReport> =
        log.orFallback("reports of player $playerId", emptyList()) {
            sql
                .select("$SELECT WHERE t.reporter_id = ? ORDER BY t.id DESC LIMIT $LISTED", playerId)
                .rows(::report)
                .asReversed()
        }

    fun ofReporter(
        playerId: Int,
        reportId: Int,
    ): FiledReport? =
        log.orFallback("report $reportId of player $playerId", null) {
            sql.select("$SELECT WHERE t.reporter_id = ? AND t.id = ?", playerId, reportId).first(::report)
        }

    /** Asks for a second look at a report closed without action; false when it is not the player's or not such one. */
    fun appeal(
        playerId: Int,
        reportId: Int,
        now: Int,
    ): Boolean =
        log.orFallback("appeal of report $reportId", false) {
            sql.write(APPEAL) {
                int(now)
                int(reportId)
                int(playerId)
            } == 1
        }

    private fun report(row: ResultSet): FiledReport {
        val resolution = TicketResolution.fromStored(row.getString("resolution"))
        return FiledReport(
            id = row.getInt("id"),
            createdAt = row.millis("created_at"),
            message = row.getString("message").orEmpty(),
            category = row.getInt("topic_id"),
            reportedName = row.getString("accused_name").orEmpty(),
            closedAt = row.millis("closed_at"),
            sanctioned = resolution == TicketResolution.HANDLED,
            sanctionedAutomatically = resolution == TicketResolution.AUTOMATIC,
            appeal = FiledReport.Appeal.fromStored(row.getString("appeal")),
            appealedAt = row.millis("appealed_at"),
            appealResolvedAt = row.millis("appeal_resolved_at"),
        )
    }

    /** Unix seconds as epoch milliseconds, -1 for none. */
    private fun ResultSet.millis(column: String): Long {
        val seconds = getLong(column)
        return if (wasNull() || seconds == 0L) -1 else seconds * MILLIS_PER_SECOND
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(MyReportsTable::class.java)
        const val LISTED = 25
        const val SELECT =
            "SELECT t.id, t.created_at, t.message, t.topic_id, t.closed_at, t.resolution, t.appeal, t.appealed_at, " +
                "t.appeal_resolved_at, accused.name AS accused_name " +
                "FROM support_tickets t LEFT JOIN players accused ON accused.id = t.accused_id"
        const val APPEAL =
            "UPDATE support_tickets SET appeal = 'pending', appealed_at = ? " +
                "WHERE id = ? AND reporter_id = ? AND status = 'closed' AND appeal = 'none' " +
                "AND resolution <> 'handled'"
    }
}
