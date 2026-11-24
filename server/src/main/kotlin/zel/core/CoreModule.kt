package zel.core

import zel.core.config.ConfigurationReload
import zel.core.load.LoadWatch
import zel.core.load.ServerGauges
import zel.core.threading.GameThreads
import zel.db.Database
import zel.db.DatabaseWriter
import zel.game.Game
import javax.sql.DataSource

class CoreModule(
    val state: ServerState,
    private val dataSource: () -> DataSource?,
) {
    fun commandLog(configuration: Configuration): CommandLog =
        CommandLog(
            configuration.getInt(CommandLog.LIMIT_KEY).takeIf { it > 0 } ?: CommandLog.DEFAULT_LIMIT,
            dataSource,
        )

    fun threading(
        threads: Int,
        writer: DatabaseWriter,
    ): GameThreads = GameThreads(threads, writer)

    fun loadWatch(
        configuration: Configuration,
        database: Database,
        writer: DatabaseWriter,
        threading: GameThreads,
        packetLoad: () -> ServerGauges.Occupancy?,
    ): LoadWatch {
        val settings = LoadWatch.Settings.from(configuration)
        val gauges =
            ServerGauges.forServer(database, writer, threading, settings.schedulerLateMillis, packetLoad)
        return LoadWatch(settings, gauges::sample).also { it.start(threading) }
    }

    fun configurationReload(configuration: Configuration): ConfigurationReload = ConfigurationReload(configuration)

    fun texts(): Texts = Texts.load(dataSource())

    fun housekeeping(
        configuration: Configuration,
        commandLog: CommandLog,
        threading: GameThreads,
        game: () -> Game?,
    ): Housekeeping = Housekeeping(configuration, dataSource(), commandLog, threading, state, game)
}
