package zel.content

import com.google.gson.JsonElement
import org.slf4j.LoggerFactory
import zel.db.executeAll
import zel.db.rows
import zel.db.write
import zel.db.writeEach
import java.sql.Connection
import java.sql.SQLException
import java.time.Instant
import java.util.Locale

internal class ContentImporter(
    private val connection: Connection,
) {
    class Result(
        val rows: Long,
        val ids: ContentIds.Outcome,
    )

    /** A refused group is reported by its last row. */
    private inner class RowBatch(
        private val sectionId: String,
        private val table: String,
    ) {
        var columns: List<String> = emptyList()
        private val pending = mutableListOf<List<String?>>()
        private var seen = 0L

        fun add(row: List<String?>) {
            seen++
            pending += row
            if (pending.size >= BATCH_ROWS) send()
        }

        fun finish() {
            if (pending.isNotEmpty()) send()
        }

        private fun send() {
            try {
                insert(table, columns, pending)
            } catch (error: SQLException) {
                throw ContentPackException(
                    "section $sectionId, somewhere in the ${pending.size} rows up to row $seen: ${error.message}",
                    error,
                )
            }
            pending.clear()
        }
    }

    fun importTables(pack: ContentPack): Result {
        val declared = pack.sections.associateBy { it.section.id }
        connection.executeAll("SET SESSION sql_mode = '$SQL_MODE'")
        val stagedColumns = declared.mapValues { (_, section) -> stage(pack, section) }
        val ids = ContentIds(connection, stagedColumns, ::stageName).assign()
        val roomIds = ids.roomIds
        val clientStrings = pack.clientStrings()
        val previous = connection.autoCommit
        connection.autoCommit = false
        try {
            connection.executeAll("SET SESSION foreign_key_checks = 0")
            for (section in ContentSections.ALL) {
                if (section.mode == ImportMode.SEED && hasRows(section.table)) continue
                clear(section, roomIds, section.id in declared)
                stagedColumns[section.id]?.let { write(section, it) }
            }
            writeClientStrings(clientStrings)
            writePackInfo(pack)
            connection.commit()
        } catch (failure: SQLException) {
            rollBack(failure)
            throw ContentPackException(
                "The import failed while replacing the tables (${failure.message}). Transactional tables were " +
                    "rolled back; ${nonTransactional().joinToString()} may be half written. Import a good pack again.",
                failure,
            )
        } catch (
            // whatever else fails, the open transaction is rolled back before the failure goes on
            @Suppress("TooGenericExceptionCaught") failure: Throwable,
        ) {
            rollBack(failure)
            throw failure
        } finally {
            connection.executeAll(
                "SET SESSION foreign_key_checks = 1",
                "DROP TEMPORARY TABLE IF EXISTS ${declared.keys.joinToString { stageName(it) }}",
            )
            connection.autoCommit = previous
        }
        return Result(declared.values.sumOf { it.rows }, ids)
    }

    private fun rollBack(failure: Throwable) {
        runCatching { connection.rollback() }.exceptionOrNull()?.let(failure::addSuppressed)
    }

    @Suppress("ThrowsCount") // one refusal per problem
    private fun stage(
        pack: ContentPack,
        declared: DeclaredSection,
    ): List<String> {
        val section = declared.section
        val known = columnsOf(section.table)
        if (known.isEmpty()) throw ContentPackException("this schema has no table for section ${section.id}")
        val stageTable = stageName(section.id)
        connection.executeAll(
            "DROP TEMPORARY TABLE IF EXISTS $stageTable",
            "CREATE TEMPORARY TABLE $stageTable LIKE `${section.table}`",
        )
        val batch = RowBatch(section.id, stageTable)
        var keptIndexes = emptyList<Int>()
        pack.readRows(
            declared,
            { header ->
                val unknown = header - known - section.retired
                if (unknown.isNotEmpty()) {
                    throw ContentPackException("section ${section.id}: this schema has no fields $unknown")
                }
                keptIndexes = header.indices.filter { header[it] !in section.retired }
                batch.columns = keptIndexes.map(header::get)
            },
        ) { row -> batch.add(keptIndexes.map { cell(row[it]) }) }
        batch.finish()
        return batch.columns
    }

    private fun cell(value: JsonElement): String? =
        when {
            value.isJsonNull -> null
            value.isJsonPrimitive && value.asJsonPrimitive.isBoolean -> if (value.asBoolean) "1" else "0"
            value.isJsonPrimitive -> value.asString
            else -> value.toString()
        }

    private fun insert(
        table: String,
        columns: List<String>,
        rows: List<List<String?>>,
    ) {
        val placeholders = columns.joinToString(",", "(", ")") { "?" }
        val sql = "INSERT INTO $table (${quoted(columns)}) VALUES " + rows.joinToString(",") { placeholders }
        connection.write(sql) { rows.flatten().forEach { string(it) } }
    }

    private fun clear(
        section: TableSection,
        roomIds: String,
        declared: Boolean,
    ) {
        val scope =
            when (section.mode) {
                ImportMode.REPLACE -> section.keep?.let { "NOT ($it)" }
                ImportMode.ROOMS -> section.roomScope?.format(Locale.ROOT, roomIds)
                ImportMode.MERGE, ImportMode.PLAYERS, ImportMode.SEED -> return
            }
        if (section.mode == ImportMode.ROOMS && !declared && roomIds == "NULL") return
        connection.executeAll("DELETE FROM `${section.table}`" + scope?.let { " WHERE $it" }.orEmpty())
    }

    private fun hasRows(table: String): Boolean =
        connection.rows("SELECT 1 FROM `$table` LIMIT 1") { true }.isNotEmpty()

    /** A pack player already in the game keeps its row and takes the pack's values. */
    private fun write(
        section: TableSection,
        columns: List<String>,
    ) {
        val list = quoted(columns)
        val from = "SELECT $list FROM ${stageName(section.id)}"
        if (section.mode == ImportMode.PLAYERS) {
            val updates = columns.filter { it != "id" }.joinToString(",") { "`$it` = VALUES(`$it`)" }
            connection.executeAll("INSERT INTO `${section.table}` ($list) $from ON DUPLICATE KEY UPDATE $updates")
        } else {
            connection.executeAll("REPLACE INTO `${section.table}` ($list) $from")
        }
    }

    private fun writeClientStrings(strings: Map<String, String>) {
        connection.executeAll("DELETE FROM client_strings")
        connection.writeEach(INSERT_CLIENT_STRING, strings.entries) { (name, value) ->
            string(name)
            string(value)
        }
    }

    private fun writePackInfo(pack: ContentPack) {
        connection.executeAll("DELETE FROM content_packs")
        val assetLocation =
            if (pack.assets.inPack) {
                "included"
            } else {
                listOf(pack.assets.dir, pack.assets.url).filter { it.isNotEmpty() }.joinToString(" ")
            }
        val importedAt = Instant.now().epochSecond
        val values =
            listOf(pack.name, pack.version, pack.format, pack.sha256, assetLocation, importedAt)
        connection.write(
            "INSERT INTO content_packs (name, version, format, sha256, asset_location, imported_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
        ) { values.forEach { value(it) } }
    }

    private fun columnsOf(table: String): Set<String> {
        val sql =
            "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?"
        return connection.rows(sql, { string(table) }) { it.getString(1) }.toSet()
    }

    private fun nonTransactional(): List<String> {
        val sql =
            "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND ENGINE <> 'InnoDB'"
        val contentTables = ContentSections.ALL.map { it.table }.toSet()
        return connection
            .rows(sql) { it.getString(1) }
            .filter { it in contentTables }
            .ifEmpty {
                LOGGER.debug("Every content table is transactional.")
                listOf("no table")
            }
    }

    private fun quoted(columns: List<String>) = columns.joinToString(",") { "`$it`" }

    private fun stageName(sectionId: String) = "content_stage_" + sectionId.replace(Regex("[^a-z0-9]"), "_")

    private companion object {
        private val LOGGER = LoggerFactory.getLogger(ContentImporter::class.java)
        private const val BATCH_ROWS = 200
        private const val INSERT_CLIENT_STRING = "INSERT INTO client_strings (name, value) VALUES (?, ?)"

        private const val SQL_MODE = "STRICT_ALL_TABLES,NO_ENGINE_SUBSTITUTION"
    }
}
