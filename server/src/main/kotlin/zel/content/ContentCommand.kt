package zel.content

import zel.core.Configuration
import zel.core.config.GameEnvironment
import zel.db.Database
import zel.db.rows
import java.io.IOException
import java.io.PrintStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.file.Path
import java.sql.Connection
import java.sql.SQLException
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

object ContentCommand {
    private const val USAGE =
        "usage: zellera content import [--no-client | --client-only] <pack.zip> | zellera content status"
    private const val FAILED = 1
    private const val NOT_IMPORTED = 3
    private const val FILES_MISSING = 4
    private const val FORMAT_NOT_READ = 5
    private const val NAME = "content"
    private const val NO_CLIENT = "--no-client"
    private const val CLIENT_ONLY = "--client-only"
    private const val CONNECT_TIMEOUT_MILLIS = 300

    fun handles(args: Array<String>): Boolean = args.firstOrNull() == NAME

    fun main(args: Array<String>) {
        val status = args.drop(1) == listOf("status")
        val code =
            try {
                if (status) status(System.out) else run(args, System.out).let { 0 }
            } catch (refusal: ContentPackException) {
                System.err.println(if (status) refusal.message else "import refused: ${refusal.message}")
                FAILED
            }
        exitProcess(code)
    }

    /** One line and the exit code `zel` decides by: 0 ready, 3 nothing imported, 4 files missing, 5 format. */
    internal fun status(
        out: PrintStream,
        files: ContentFiles = ContentFiles(ContentDirs.fromEnvironment()),
    ): Int {
        val owner = GameEnvironment.Account.SCHEMA_OWNER
        val imported = onDatabase(Configuration(usable(owner).startup(owner)), ImportedContent::read)
        val format = imported.formatProblem()
        val missing = files.missing()
        val (line, code) =
            when {
                !imported.present -> ImportedContent.NONE to NOT_IMPORTED
                format != null -> format to FORMAT_NOT_READ
                missing.isNotEmpty() -> filesMissing(imported, missing) to FILES_MISSING
                else -> "${imported.summary} is imported" to 0
            }
        out.println(line)
        return code
    }

    private fun filesMissing(
        imported: ImportedContent,
        missing: List<String>,
    ) = "${imported.summary} is imported, but its files are missing: ${missing.joinToString()}"

    private fun usable(account: GameEnvironment.Account): GameEnvironment {
        val environment = GameEnvironment(System.getenv())
        environment.problems(account).takeIf { it.isNotEmpty() }?.let {
            throw ContentPackException(
                "the configuration (config/zellera.env) is not usable: ${it.joinToString("; ")}",
            )
        }
        return environment
    }

    internal fun run(
        args: Array<String>,
        out: PrintStream,
    ) {
        val words = args.drop(1)
        val withClient = NO_CLIENT !in words
        val clientOnly = CLIENT_ONLY in words
        val operands = words.filter { it != NO_CLIENT && it != CLIENT_ONLY }
        if (operands.size != 2 || operands[0] != "import" || (clientOnly && !withClient)) {
            throw ContentPackException(USAGE)
        }
        if (clientOnly) return installClientOnly(Path.of(operands[1]), out)
        val startedAt = System.nanoTime()
        val owner = GameEnvironment.Account.SCHEMA_OWNER
        val environment = usable(owner)
        ContentPack.open(Path.of(operands[1])).use { pack ->
            val dirs = ContentDirs.fromEnvironment()
            val files = ContentFiles(dirs)
            val client = ClientInstall(dirs.client)
            files.check(pack)
            if (withClient) client.check(pack)
            val outcome =
                onDatabase(Configuration(environment.startup(owner))) { connection ->
                    refuseWhileRunning(connection)
                    ContentImporter(connection).importTables(pack).also { environment.writeSettings(connection) }
                }
            val written = files.write(pack)
            val installed = if (withClient) client.write(pack) else 0
            val tookMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
            out.println(
                "imported ${pack.name} ${pack.version}: ${pack.sections.size} sections, ${outcome.rows} rows, " +
                    "$written files, $tookMillis ms",
            )
            if (installed > 0) out.println("client: $installed files installed in ${dirs.client}")
            if (!withClient && client.names(pack).isNotEmpty()) out.println("client: not installed (--no-client)")
            val players = outcome.ids.players
            if (players.isNotEmpty()) {
                out.println(
                    players.joinToString(", ", "players: ") {
                        "${it.name} id ${it.id}" +
                            if (it.created) " (new)" else ""
                    },
                )
            }
            if (outcome.ids.moved.isNotEmpty()) {
                out.println(
                    outcome.ids.moved.entries.joinToString(", ", "ids this game holds already, given new ones: ") {
                        "${it.value} ${it.key.name.lowercase()}"
                    },
                )
            }
            if (!pack.assets.inPack) {
                out.println(
                    "assets are not in this pack: the game serves them from ZEL_ASSET_DIR or ZEL_ASSET_URL" +
                        whereAssetsAre(pack.assets),
                )
            }
        }
    }

    /** Only the pack's client: no database, no content files. */
    internal fun installClientOnly(
        path: Path,
        out: PrintStream,
        dirs: ContentDirs = ContentDirs.fromEnvironment(),
    ) {
        val dir = dirs.client
        ContentPack.open(path).use { pack ->
            val client = ClientInstall(dir)
            if (client.names(pack).isEmpty()) throw ContentPackException("${pack.name} ${pack.version} holds no client")
            client.check(pack)
            out.println("client: ${client.write(pack)} files installed in $dir")
        }
    }

    private fun whereAssetsAre(assets: PackAssets) =
        listOf(assets.dir, assets.url)
            .filter { it.isNotEmpty() }
            .joinToString(" / ")
            .let { if (it.isEmpty()) "" else " (the pack's: $it)" }

    private fun <T> onDatabase(
        config: Configuration,
        job: (Connection) -> T,
    ): T {
        val database =
            try {
                Database(config)
            } catch (e: IllegalStateException) {
                throw ContentPackException(
                    "cannot reach the database that ZEL_DB_HOST, ZEL_DB_PORT and ZEL_DB_NAME name",
                    e,
                )
            }
        return try {
            database.dataSource.connection.use(job)
        } catch (e: SQLException) {
            throw ContentPackException("database error: ${e.message}", e)
        } finally {
            database.close()
        }
    }

    /** The server keeps content in memory, so importing under a running one would leave the two apart. */
    private fun refuseWhileRunning(connection: Connection) {
        val listener = gameListener(connection) ?: return
        val running =
            try {
                Socket().use { it.connect(listener, CONNECT_TIMEOUT_MILLIS) }
                true
            } catch (_: IOException) {
                false
            }
        if (running) throw ContentPackException("a server is listening on $listener; stop it before importing")
    }

    private fun gameListener(connection: Connection): InetSocketAddress? {
        val settings =
            connection
                .rows(
                    "SELECT `name`, `value` FROM `settings`" +
                        " WHERE `name` IN ('network.listen.address', 'network.listen.port')",
                ) {
                    it.getString(1) to it.getString(2)
                }.toMap()
        val port = settings["network.listen.port"]?.toIntOrNull() ?: return null
        return InetSocketAddress(settings["network.listen.address"].orEmpty().ifEmpty { "127.0.0.1" }, port)
    }
}
