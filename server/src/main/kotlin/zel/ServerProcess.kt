package zel

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import org.slf4j.LoggerFactory
import zel.core.BuildInfo
import zel.core.Configuration
import zel.core.ServerState
import zel.core.config.GameEnvironment
import zel.core.console.ConsoleMenu
import zel.core.log.ConsoleColour
import zel.core.rules.ActiveRules
import zel.db.migration.SchemaCommand
import zel.util.GameZone
import java.time.Duration
import kotlin.system.exitProcess

class ServerProcess(
    private val build: BuildStamp = BuildStamp.ofRunningCode(),
    private val lifecycle: Lifecycle = Lifecycle(),
    private val nanoClock: () -> Long = System::nanoTime,
) : ServerState {
    private enum class Stage { BOOTING, SERVING, CLOSING, CLOSED }

    private enum class Ending(
        val code: Int,
    ) {
        SCHEMA_STEP_DONE(0),
        START_FAILED(1),
        BAD_ARGUMENTS(2),
        BAD_CONFIGURATION(CONFIGURATION_REFUSED),
    }

    @Volatile
    private var stage = Stage.BOOTING
    private val launchedAt = nanoClock()

    @Volatile
    private var parts: GameBootstrap? = null

    override val isReady: Boolean get() = stage == Stage.SERVING
    override val isShuttingDown: Boolean get() = stage >= Stage.CLOSING
    override val uptimeSeconds: Int get() = uptime().seconds.toInt()
    val stopped: Boolean get() = stage == Stage.CLOSED

    fun run(commandLine: Array<String>) {
        val schemaStep =
            try {
                SchemaCommand.fromCommandLine(commandLine)
            } catch (refused: IllegalArgumentException) {
                System.err.println(refused.message)
                exitProcess(Ending.BAD_ARGUMENTS.code)
            }
        boot(schemaStep)?.let { exitProcess(it.code) }
    }

    /** Logging stops last, so what the closing parts report reaches the log. */
    @Synchronized
    fun stop() {
        if (isShuttingDown) return
        stage = Stage.CLOSING
        log.info("shutting down {}", build.version)
        lifecycle.stop()
        stage = Stage.CLOSED
        log.info("stopped after {} s", uptimeSeconds)
        (LoggerFactory.getILoggerFactory() as? LoggerContext)?.stop()
    }

    /**
     * Null while serving; the shutdown hook comes first, so a stop during the start still closes what is up. A schema
     * step alone works as the schema owner, the server as its data-only account.
     */
    @Suppress("TooGenericExceptionCaught") // whatever fails while starting, what is up closes and the process ends
    private fun boot(schemaStep: SchemaCommand): Ending? {
        val environment = GameEnvironment(System.getenv())
        val account = if (schemaStep.serves) GameEnvironment.Account.APP else GameEnvironment.Account.SCHEMA_OWNER
        val problems = environment.problems(account)
        if (problems.isNotEmpty()) {
            System.err.println(
                problems.joinToString("\n", "the configuration (config/zellera.env) is not usable:\n") { "  $it" },
            )
            return Ending.BAD_CONFIGURATION
        }
        return try {
            Runtime.getRuntime().addShutdownHook(Thread(::stop, "shutdown"))
            if (schemaStep.serves) ActiveRules.get()
            val settings = Configuration(environment.startup(account)) { parts?.database?.dataSource }
            greet(settings)
            val assembled =
                GameBootstrap(lifecycle, this, build, settings, environment) { settingsLoaded(settings) }
                    .also { parts = it }
            if (assembled.start(schemaStep)) {
                serve(assembled, settings)
                null
            } else {
                Ending.SCHEMA_STEP_DONE
            }
        } catch (trouble: Exception) {
            log.error("{} did not start", BuildInfo.PRODUCT, trouble)
            stop()
            Ending.START_FAILED
        }
    }

    /** Only the startup keys are known here; the settings table follows in [settingsLoaded]. */
    private fun greet(settings: Configuration) {
        ConsoleColour.follow(settings.getValue(ConsoleColour.SETTING))
        print(Greeting.banner(build, "Java ${Runtime.version()}", ConsoleColour.on))
    }

    /** The table may set the console colour, and it holds the game's time zone the build line is shown in. */
    private fun settingsLoaded(settings: Configuration) {
        ConsoleColour.follow(settings.getValue(ConsoleColour.SETTING))
        log.info(Greeting.buildLine(build, GameZone.current))
    }

    private fun serve(
        assembled: GameBootstrap,
        settings: Configuration,
    ) {
        followLogLevel(settings)
        settings.onReload { followLogLevel(settings) }
        synchronized(this) { if (stage == Stage.BOOTING) stage = Stage.SERVING }
        log.info("{} {} ready in {} ms", BuildInfo.PRODUCT, build.version, uptime().toMillis())
        if (!settings.getBoolean("server.console.reads_commands", false)) return
        val menu =
            ConsoleMenu.standard(
                this,
                build.version,
                build.hash,
                assembled::threading,
                assembled::server,
                assembled::game,
            )
        ServerConsole(menu) { !isShuttingDown }.start()
    }

    /** The setting is followed both ways at every reload, so debug lines can be had from a running game and ended. */
    private fun followLogLevel(settings: Configuration) {
        val wanted = if (settings.getBoolean("server.log.is_debug", false)) Level.DEBUG else Level.INFO
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        if (root.level == wanted) return
        root.level = wanted
        log.info("log level {}", wanted.levelStr.lowercase())
    }

    private fun uptime(): Duration = Duration.ofNanos(nanoClock() - launchedAt)

    private companion object {
        val log: org.slf4j.Logger = LoggerFactory.getLogger(ServerProcess::class.java)

        /** `zel` tells a refused configuration from a failed start by this code. */
        const val CONFIGURATION_REFUSED = 3
    }
}
