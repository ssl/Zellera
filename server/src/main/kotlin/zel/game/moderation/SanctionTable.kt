package zel.game.moderation

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.orFallback
import java.sql.ResultSet
import javax.sql.DataSource

class SanctionTable(
    dataSource: DataSource,
) {
    private val sql = Sql(dataSource)

    /** The ladder by step; a row whose kind the code does not know is left out. */
    fun ladder(): Map<Int, LadderStep> =
        log.orFallback("sanction ladder", emptyMap()) {
            sql
                .select("SELECT step, kind, hours, probation_days FROM sanction_steps")
                .rows { row ->
                    SanctionKind.ofStored(row.getString("kind"))?.let { kind ->
                        LadderStep(row.getInt("step"), kind, row.getInt("hours"), row.getInt("probation_days"))
                    }
                }.filterNotNull()
                .sortedBy { it.step }
                .associateBy { it.step }
        }

    fun latestOf(playerId: Int): SanctionRecord? =
        log.orFallback("latest sanction of player $playerId", null) {
            sql.select("$SELECT WHERE player_id = ? ORDER BY id DESC LIMIT 1", playerId).first(::record)
        }

    fun endProbation(recordId: Int) {
        log.orFallback("probation end of sanction $recordId", Unit) {
            sql.write("UPDATE sanctions SET probation_ends_at = NULL WHERE id = ?", recordId)
        }
    }

    fun insert(
        record: SanctionRecord,
        issuedBy: Int,
        topicId: Int,
    ): Boolean =
        log.orFallback("sanction of player ${record.playerId}", false) {
            sql.write(INSERT) {
                int(record.playerId)
                int(record.step)
                string(record.kind.name)
                string(record.reason.take(REASON_LENGTH))
                intOrNullWhenNotPositive(topicId)
                intOrNullWhenNotPositive(issuedBy)
                int(record.issuedAt)
                intOrNullWhenNotPositive(record.endsAt)
                intOrNullWhenNotPositive(record.probationEndsAt)
            } == 1
        }

    private fun record(row: ResultSet) =
        SanctionRecord(
            id = row.getInt("id"),
            playerId = row.getInt("player_id"),
            step = row.getInt("step"),
            kind = SanctionKind.ofStored(row.getString("kind")) ?: SanctionKind.ALERT,
            reason = row.getString("reason").orEmpty(),
            issuedAt = row.seconds("issued_at"),
            endsAt = row.seconds("ends_at"),
            probationEndsAt = row.seconds("probation_ends_at"),
        )

    private fun ResultSet.seconds(column: String): Int = getLong(column).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    private companion object {
        val log: Logger = LoggerFactory.getLogger(SanctionTable::class.java)
        const val REASON_LENGTH = 255
        const val SELECT =
            "SELECT id, player_id, step, kind, reason, issued_at, ends_at, probation_ends_at FROM sanctions"
        const val INSERT =
            "INSERT INTO sanctions (player_id, step, kind, reason, topic_id, issued_by, issued_at, ends_at, " +
                "probation_ends_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
    }
}
