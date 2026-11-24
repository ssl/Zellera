package zel

import zel.content.ClientStrings
import zel.content.ImportedContent
import zel.core.CommandLog
import zel.core.Configuration
import zel.core.CoreModule
import zel.core.Housekeeping
import zel.core.ServerState
import zel.core.Texts
import zel.core.config.GameEnvironment
import zel.core.load.LoadWatch
import zel.core.threading.GameThreads
import zel.db.Database
import zel.db.DatabaseWriter
import zel.db.WriterSize
import zel.db.migration.Migrations
import zel.db.migration.SchemaCommand
import zel.db.migration.VersionRecord
import zel.game.Game
import zel.game.GameCore
import zel.game.ServerInfo
import zel.game.achievements.AchievementTally
import zel.game.guilds.BadgeImager
import zel.game.icons.IconService
import zel.net.NetModule
import zel.net.crypto.RsaKey
import zel.net.server.GamePort
import zel.protocol.typed.PacketDispatcher
import zel.protocol.typed.gameAllowedValues
import zel.server.ServerWiring
import zel.server.gamePackages
import zel.util.GameZone
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.time.Duration
import java.util.concurrent.Executor

internal class GameBootstrap(
    private val lifecycle: Lifecycle,
    private val state: ServerState,
    private val build: BuildStamp,
    private val settings: Configuration,
    private val environment: GameEnvironment,
    private val settingsLoaded: () -> Unit,
) {
    @Volatile
    var database: Database? = null
        private set

    @Volatile
    var threading: GameThreads? = null
        private set

    @Volatile
    var game: Game? = null
        private set

    @Volatile
    var server: GamePort? = null
        private set

    @Volatile
    private var badgeArt: BadgeImager? = null

    private val core = CoreModule(state) { database?.dataSource }

    /** False when the server stops after the schema step. */
    fun start(schema: SchemaCommand): Boolean {
        val database = openDatabase(schema) ?: return false
        requireContent(database)
        environment.dataFolders().forEach { Files.createDirectories(it) }
        settingsReady()
        ClientStrings.load(database.dataSource)
        val texts = core.texts()
        val commandLog = lifecycle.start("command log", { it.write() }) { core.commandLog(settings) }
        val writer = writerPart()
        val threading =
            lifecycle
                .start("scheduler", ::stopScheduler) { core.threading(schedulerThreads(), writer) }
                .also { threading = it }
        val game = gamePart(database, texts, threading, commandLog)
        val loadWatch =
            core.loadWatch(settings, database, writer, threading) {
                server?.packetOccupancy()
            }
        val port = networkPart(database, texts, threading, game, loadWatch)
        badgeArt = game.guilds?.let { BadgeImager(settings, database, it) }
        lifecycle.start("housekeeping", Housekeeping::stop) {
            core.housekeeping(settings, commandLog, threading) { this.game }.also(Housekeeping::start)
        }
        lifecycle.start("network", GamePort::stop) { port.also(GamePort::start).also { server = it } }
        // First thing at stop: saves made while the parts close run on the closing thread instead of being cancelled.
        lifecycle.atStop("scheduler accepting") { threading.stopAccepting() }
        return true
    }

    /**
     * Null when only a schema step is asked. The operator's settings rows are written either way, and a database a
     * newer version ran on is refused before anything is changed.
     */
    private fun openDatabase(schema: SchemaCommand): Database? {
        val step = schema.step
        val version = VersionRecord(build.version)
        val connections = Database.schemaConnections(settings)
        connections.connection.use(version::admit)
        val done = Migrations(connections).run(step)
        log.info("schema {}: {}", step.word, done)
        val rows =
            connections.connection.use {
                version.write(it)
                environment.writeSettings(it)
            }
        if (rows > 0) log.info("settings: {} rows from the configuration", rows)
        if (!schema.serves) return null
        // Before the pool opens: its size and timeouts are settings rows too.
        settings.loadTable(connections)
        return lifecycle.start("database", Database::close) { Database(settings) }.also { database = it }
    }

    private fun requireContent(database: Database) {
        val imported = database.dataSource.connection.use(ImportedContent::read)
        check(imported.present) { ImportedContent.NONE }
        imported.formatProblem()?.let { error(it) }
    }

    private fun settingsReady() {
        GameZone.follow(settings)
        settingsLoaded()
        core.configurationReload(settings).activate()
    }

    private fun stopScheduler(threading: GameThreads) {
        val stopped = threading.stop()
        if (!stopped.clean) {
            log.warn(
                "scheduler stopped with work still running; {} waiting tasks dropped",
                stopped.letGo,
            )
        }
    }

    private fun schedulerThreads(): Int =
        settings.getInt("server.scheduler.threads", 0).takeIf { it > 0 }
            ?: schedulerThreadsFor(Runtime.getRuntime().availableProcessors())

    private fun writerPart(): DatabaseWriter {
        val size = WriterSize.configured(settings)
        log.info("database writer: {} threads, queue {}", size.threads, size.capacity)
        val drain =
            settings.getInt("server.stop.write_drain.seconds", DEFAULT_DRAIN_SECONDS).coerceIn(0, MAX_DRAIN_SECONDS)
        return lifecycle.start("database writer", { it.close(Duration.ofSeconds(drain.toLong())) }) {
            DatabaseWriter(size.threads, size.capacity)
        }
    }

    private fun gamePart(
        database: Database,
        texts: Texts,
        threading: GameThreads,
        commandLog: CommandLog,
    ): Game {
        val info = ServerInfo(build.version, { build.hash }, { state.uptimeSeconds })
        val gameCore = GameCore(database, texts, commandLog, state, info, { server?.connections }, { badgeArt })
        val icons =
            IconService(database.dataSource) { player, name, amount ->
                AchievementTally.running().add(player, name, amount)
            }
        return lifecycle.start("game", Game::dispose) {
            Game(Executor(threading::write), icons, threading, settings, gameCore)
                .also { game = it }
                .also(Game::load)
        }
    }

    /** Not listening yet; the rooms stop right after the network does. */
    private fun networkPart(
        database: Database,
        texts: Texts,
        threading: GameThreads,
        game: Game,
        loadWatch: LoadWatch,
    ): GamePort {
        val rsaKey = RsaKey.fromSettings(settings)
        log.info("key exchange: {}", if (rsaKey == null) "off" else "on")
        val network =
            NetModule(settings, rsaKey) { clients ->
                val wiring =
                    ServerWiring(
                        settings,
                        rsaKey,
                        database,
                        threading,
                        texts,
                        game,
                        clients,
                        loadWatch,
                        state,
                    )
                lifecycle.atStop("rooms") {
                    wiring.roomEngine.shutdown()
                    wiring.extensions.stop()
                }
                PacketDispatcher(
                    gamePackages(wiring),
                    gameAllowedValues(game, settings, database.dataSource, threading),
                ) { state.isShuttingDown }
            }
        return network.gamePort()
    }

    internal companion object {
        private val log = LoggerFactory.getLogger(GameBootstrap::class.java)
        private const val FEWEST_SCHEDULER_THREADS = 4

        /**
         * Timed tasks compute, so more threads than processors gain nothing; some read the database on the way, and
         * four keep the timers going while a few of them wait for it.
         */
        fun schedulerThreadsFor(processors: Int): Int = processors.coerceAtLeast(FEWEST_SCHEDULER_THREADS)

        const val DEFAULT_DRAIN_SECONDS = 90
        const val MAX_DRAIN_SECONDS = 150
    }
}
