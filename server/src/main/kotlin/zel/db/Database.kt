package zel.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.pool.HikariPool.PoolInitializationException
import zel.core.Configuration
import org.flywaydb.core.api.configuration.FluentConfiguration
import java.time.Duration
import javax.sql.DataSource

class Database(
    configuration: Configuration,
) : AutoCloseable {
    val dataSource: HikariDataSource =
        try {
            HikariDataSource(poolConfig(configuration))
        } catch (refusal: PoolInitializationException) {
            throw IllegalStateException("the database pool could not be opened", refusal)
        }

    val queries = Sql(dataSource)

    override fun close() = dataSource.close()

    companion object {
        const val DEFAULT_HOST = "localhost"
        const val DEFAULT_PORT = 3306
        const val DEFAULT_CONNECTIONS = 50
        const val DEFAULT_SPARE_CONNECTIONS = 10

        const val SILENT_KEY = "database.connection.silent.seconds"

        /** A connection that answers nothing for this long is given up, so a writer it held can try another. */
        val SILENT_CONNECTION: Duration = Duration.ofSeconds(60)

        /** A timeout left at 0 keeps the pool library's default. */
        private val TIMEOUTS: List<Pair<String, HikariConfig.(Long) -> Unit>> =
            listOf(
                "database.pool.wait.ms" to { connectionTimeout = it },
                "database.pool.idle.ms" to { idleTimeout = it },
                "database.pool.lifetime.ms" to { maxLifetime = it },
                "database.pool.check.ms" to { validationTimeout = it },
                "database.pool.leak_report.ms" to { leakDetectionThreshold = it },
            )

        fun poolConfig(configuration: Configuration): HikariConfig =
            HikariConfig().apply {
                jdbcUrl = url(configuration)
                username = configuration.getValue("database.user")
                password = configuration.getValue("database.password")
                maximumPoolSize = configuration.getInt("database.pool.max_connections", DEFAULT_CONNECTIONS)
                minimumIdle = configuration.getInt("database.pool.idle_connections", DEFAULT_SPARE_CONNECTIONS)
                TIMEOUTS.forEach { (key, apply) ->
                    configuration.getInt(key).takeIf { it > 0 }?.let { apply(it.toLong()) }
                }
                val silent =
                    configuration.getInt(SILENT_KEY).takeIf { it > 0 }?.let { Duration.ofSeconds(it.toLong()) }
                        ?: SILENT_CONNECTION
                addDataSourceProperty("socketTimeout", silent.toMillis().toString())
                addDataSourceProperty("rewriteBatchedStatements", "true")
            }

        /**
         * The schema step runs once and ends, so it has no pool: every connection asked for is opened then and closed
         * after use. None has a socket timeout: a schema statement cut off halfway is not undone.
         */
        fun schemaConnections(configuration: Configuration): DataSource =
            FluentConfiguration()
                .dataSource(
                    url(configuration),
                    configuration.getValue("database.user"),
                    configuration.getValue("database.password"),
                ).dataSource

        private fun url(configuration: Configuration): String {
            val host = configuration.getValue("database.host", DEFAULT_HOST)
            val port = configuration.getInt("database.port", DEFAULT_PORT)
            val name = configuration.getValue("database.name")
            return "jdbc:mariadb://$host:$port/$name${configuration.getValue("database.url_parameters")}"
        }
    }
}
