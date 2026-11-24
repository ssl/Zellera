package zel.db.migration

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.api.exception.FlywayValidateException
import zel.db.Sql
import zel.db.migration.SchemaCommand.Step
import javax.sql.DataSource

/** The database must be empty or one this server created. */
class Migrations(
    dataSource: DataSource,
) {
    sealed interface Schema {
        data object Empty : Schema

        data class Managed(
            val applied: Int,
            val pending: Int,
        ) : Schema

        /** Tables without migration history: a dump taken without it, or another server's database. */
        data class Foreign(
            val tables: List<String>,
        ) : Schema {
            fun refusal(step: String) =
                MigrationFailure(
                    "$step refused: the database has ${tables.size} tables but no $HISTORY_TABLE; " +
                        "only an empty database or one this server created can be migrated",
                )
        }
    }

    private val tables = Sql(dataSource)

    // The seed is data: text in it that looks like a placeholder stays as written.
    internal val runner = Flyway(FluentConfiguration().dataSource(dataSource).placeholderReplacement(false))

    fun inspect(): Schema {
        val present = buildList { tables.select(TABLE_NAMES).each { add(it.getString(1)) } }
        return when {
            HISTORY_TABLE in present -> runner.info().let { Schema.Managed(it.applied().size, it.pending().size) }
            present.isEmpty() -> Schema.Empty
            else -> Schema.Foreign(present)
        }
    }

    fun status(): String =
        when (val schema = inspect()) {
            Schema.Empty -> "empty database, pending: ${runner.info().pending().size}"
            is Schema.Managed -> "applied: ${schema.applied}, pending: ${schema.pending}"
            is Schema.Foreign -> "not managed by this server: ${schema.tables.size} tables without $HISTORY_TABLE"
        }

    fun apply(): Int {
        admit("apply")
        return runner.migrate().migrationsExecuted
    }

    /** A database that still lacks packaged migrations is refused. */
    fun check() {
        admit("check")
        pendingRefusal(runner.info().pending().size)?.let { throw it }
        try {
            runner.validate()
        } catch (invalid: FlywayValidateException) {
            throw MigrationFailure("check failed: ${invalid.message}", invalid)
        }
    }

    fun repair() {
        admit("repair")
        runner.repair()
    }

    fun run(step: Step): String =
        when (step) {
            Step.APPLY -> "applied ${apply()} migrations; ${status()}"
            Step.CHECK -> check().let { "as this version expects; ${status()}" }
            Step.REPAIR -> repair().let { "repaired; ${status()}" }
        }

    private fun admit(step: String) {
        val schema = inspect()
        if (schema is Schema.Foreign) throw schema.refusal(step)
        if (schema is Schema.Managed) unknownRefusal(step, unknownApplied())?.let { throw it }
    }

    /** Applied migrations this jar does not package: a newer version ran on this database. */
    private fun unknownApplied(): List<String> =
        runner
            .info()
            .applied()
            .filter { it.version != null && !it.state.isResolved }
            .map { it.version.toString() }

    internal companion object {
        fun pendingRefusal(pending: Int): MigrationFailure? =
            if (pending == 0) {
                null
            } else {
                MigrationFailure(
                    "check refused: $pending migrations are not applied yet; run the server once with " +
                        "`${SchemaCommand.NAME} ${Step.APPLY.word}`",
                )
            }

        fun unknownRefusal(
            step: String,
            versions: List<String>,
        ): MigrationFailure? =
            if (versions.isEmpty()) {
                null
            } else {
                MigrationFailure(
                    "$step refused: the database holds migrations this version does not know " +
                        "(${versions.joinToString()}), so a newer version ran on it; restore the backup made " +
                        "before that upgrade, or install that version or a newer one",
                )
            }

        private const val HISTORY_TABLE = "flyway_schema_history"
        private const val TABLE_NAMES =
            "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()"
    }
}
