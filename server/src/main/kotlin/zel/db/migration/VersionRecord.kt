package zel.db.migration

import zel.db.first
import zel.db.rows
import zel.db.write
import java.sql.Connection

/** The version that last ran on a database, a row of `settings`: an older version never starts on it. */
class VersionRecord(
    private val running: String,
) {
    fun admit(connection: Connection) {
        refusal(running, stored(connection))?.let { throw it }
    }

    fun write(connection: Connection) {
        if (numbers(running) == null) return
        connection.write(
            "INSERT INTO settings (`name`, `value`, `description`) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE `value` = VALUES(`value`)",
        ) {
            string(KEY)
            string(running)
            string(DESCRIPTION)
        }
    }

    private fun stored(connection: Connection): String? {
        if (connection.rows(HAS_SETTINGS) { 1 }.isEmpty()) return null
        return connection.first("SELECT `value` FROM settings WHERE `name` = ?", { string(KEY) }) { it.getString(1) }
    }

    companion object {
        const val KEY = "server.version"
        private const val DESCRIPTION = "The version that last started on this database; the server writes it."
        private const val HAS_SETTINGS =
            "SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'settings'"
        private val VERSION = Regex("(\\d+)\\.(\\d+)\\.(\\d+)")

        /** A version that is not `<major>.<minor>.<patch>` on either side is not compared. */
        fun refusal(
            running: String,
            stored: String?,
        ): MigrationFailure? {
            val ours = numbers(running) ?: return null
            val theirs = numbers(stored.orEmpty()) ?: return null
            val newer = ours.zip(theirs).firstOrNull { (a, b) -> a != b }?.let { (a, b) -> b > a } ?: false
            if (!newer) return null
            return MigrationFailure(
                "this is version $running, and version $stored ran on this database last: restore the backup made " +
                    "before that upgrade, or install version $stored or a newer one",
            )
        }

        private fun numbers(version: String): List<Int>? =
            VERSION
                .matchEntire(version.trim())
                ?.groupValues
                ?.drop(1)
                ?.map { it.toIntOrNull() ?: return null }
    }
}
