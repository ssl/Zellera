package zel.core.config

import zel.ext.GameExtensions
import zel.room.RoomCheckpoints
import zel.security.ClientCheck
import zel.util.GameZone
import java.net.URI
import java.nio.file.Path
import java.sql.Connection
import java.time.DateTimeException
import java.time.ZoneId

/**
 * The operator's `ZEL_*` variables (`config/zellera.env` and its local file, which `zel` loads): the startup keys
 * of one server command and the `settings` rows the server and the web read. A command with [problems] does not run.
 * Without `ZEL_SITE_URL` there is no game around the command and the rows stay as they are.
 */
class GameEnvironment(
    private val variables: Map<String, String>,
) {
    /** The database account a command works as: the server's data-only one, or the schema owner. */
    enum class Account(
        internal val user: String,
        internal val password: String,
    ) {
        APP("ZEL_DB_APP_USER", "ZEL_DB_APP_PASS"),
        SCHEMA_OWNER("ZEL_DB_MIGRATE_USER", "ZEL_DB_MIGRATE_PASS"),
    }

    private val found = mutableListOf<String>()

    private val site = origin("ZEL_SITE_URL", value("ZEL_SITE_URL"))
    private val perAddress = wholeNumber("ZEL_MAX_CONNECTIONS_PER_IP", "20", "(0: no cap)")
    private val roomSave =
        wholeNumber(
            "ZEL_ROOM_SAVE_MINUTES",
            RoomCheckpoints.DEFAULT_INTERVAL_MINUTES.toString(),
            "of minutes (0: at unload only)",
        )
    private val dbPort = port("ZEL_DB_PORT")
    private val keyExchange = flag("ZEL_KEY_EXCHANGE", "1")
    private val timeZone = zone("ZEL_TIME_ZONE")
    private val listenHost = value("ZEL_LISTEN_HOST") ?: "127.0.0.1"
    private val gameExtensions = gameExtensions()
    private val rows: List<Pair<String, String>> = if (site == null) emptyList() else settingsRows(site)

    private val key: Result<KeyFile.Key?> by lazy {
        runCatching { if (keyExchange == "0") null else KeyFile.path(variables)?.let(KeyFile::read) }
    }

    private val bridgeSecret: Result<String>? by lazy {
        value("ZEL_SECRETS_DIR")?.let { dir -> runCatching { BridgeSecret.of(Path.of(dir)) } }
    }

    /** Every value out of range, and what [account] lacks; empty when the command may run. */
    fun problems(account: Account): List<String> =
        buildList {
            addAll(found)
            if (value("ZEL_DB_NAME") == null) add("ZEL_DB_NAME is not set")
            if (value(account.user) == null) add("${account.user} is not set")
            if (account == Account.APP) {
                key.exceptionOrNull()?.let { add(it.message.orEmpty()) }
                bridgeSecret?.exceptionOrNull()?.let { add(it.message.orEmpty()) }
            }
        }

    fun startup(account: Account): Map<String, String> =
        buildMap {
            value("ZEL_DB_HOST")?.let { put("database.host", it) }
            dbPort?.let { put("database.port", it) }
            value("ZEL_DB_NAME")?.let { put("database.name", it) }
            value(account.user)?.let { put("database.user", it) }
            put("database.password", variables[account.password].orEmpty())
            val key = if (account == Account.APP) key.getOrNull() else null
            // Always set, so a row left in the table never turns the key exchange on.
            put("login.key_exchange.is_on", (key != null).toString())
            if (key != null) {
                put("login.key_exchange.modulus", key.modulus)
                put("login.key_exchange.private_exponent", key.exponent)
            }
            put("network.connections.per_address", perAddress)
            put("room.checkpoint.interval.minutes", roomSave)
            gameExtensions?.let { put(GameExtensions.LIST_KEY, it) }
            value("ZEL_ROOT")?.let { root ->
                put(GameExtensions.FOLDER_KEY, Path.of(root, "custom", "game-extensions").toString())
            }
            (value("ZEL_CLIENT_DIR") ?: value("ZEL_ROOT")?.let { Path.of(it, "client").toString() })?.let {
                put(ClientCheck.FOLDER_KEY, it)
            }
            // Over the settings row: the zone is the operator's, and an empty variable leaves the row to say.
            timeZone?.let { put(GameZone.SETTING, it) }
            if (account == Account.APP) bridgeSecret?.getOrNull()?.let { put("wired.variables.bridge.secret", it) }
        }

    /** The rows over what a content pack brings; the RSA key and its switch are never a row. */
    fun writeSettings(connection: Connection): Int {
        if (rows.isEmpty()) return 0
        connection.prepareStatement("DELETE FROM settings WHERE `name` IN ($KEY_ROWS)").use {
            it.executeUpdate()
        }
        connection
            .prepareStatement(
                "INSERT INTO settings (`name`, `value`, `description`) VALUES (?, ?, '') " +
                    "ON DUPLICATE KEY UPDATE `value` = VALUES(`value`)",
            ).use { insert ->
                rows.forEach { (name, value) ->
                    insert.setString(1, name)
                    insert.setString(2, value)
                    insert.addBatch()
                }
                insert.executeBatch()
            }
        return rows.size
    }

    internal fun settingsRows(): List<Pair<String, String>> = rows

    /** The folders under `ZEL_DATA_DIR` the game writes into. */
    fun dataFolders(): List<Path> =
        value("ZEL_DATA_DIR")
            ?.let(Path::of)
            ?.let { data -> DATA_FOLDERS.map(data::resolve) }
            .orEmpty()

    private fun settingsRows(site: URI): List<Pair<String, String>> {
        val wsPort = required("ZEL_WS_PORT", port("ZEL_WS_PORT"))
        val webPort = port("ZEL_WEB_PORT") ?: DEFAULT_WEB_PORT
        val data = required("ZEL_DATA_DIR", value("ZEL_DATA_DIR"))
        val debug = debugLogin()
        val email = choice("ZEL_WEB_EMAIL", "off", EMAIL_MODES)
        val origins = listOf(site) + extraOrigins()
        return buildList {
            add("network.listen.address" to listenHost)
            add("network.listen.port" to wsPort)
            add("network.origin.hosts" to origins.map { it.host }.distinct().joinToString(","))
            add("network.client_address.header" to clientIpHeader())
            add("network.client_address.trusted_proxies" to trustedProxies())
            add("login.client_releases" to "any")
            add("login.ticket.is_reusable" to debug)
            add("login.ticket.expires" to "1")
            add("login.device.is_checked" to flag("ZEL_DEVICE_CHECK", "1"))
            add("groups.badges.are_drawn" to "1")
            add("groups.badges.parts.folder" to "$data/badgeparts/")
            add("groups.badges.output.folder" to "$data/usercontent/badges/")
            add("camera.storage.folder" to "$data/usercontent/")
            add("camera.samples.folder" to value("ZEL_CAMERA_SAMPLES_DIR").orEmpty())
            val webHost = if (listenHost in EVERY_ADDRESS) "127.0.0.1" else listenHost
            add("furni.data.assets.folder" to "$data/furniture/")
            add("snowball.arenas.folder" to "$data/snowball/arenas/")
            add("site.url" to site.toString())
            add(LOCAL_SITE_ROW to "http://$webHost:$webPort")
            SWITCHES.forEach { (variable, row, default) -> add(row to flag(variable, default)) }
            add("site.help_requests.mode" to choice("ZEL_WEB_HELP_TICKETS", "account", HELP_MODES))
            add("site.email.mode" to email)
            add("site.avatars.per_account" to avatarsPerAccount(email))
            add("site.extensions" to extensions())
            add("treasure_hunt.is_on" to "0")
            value("ZEL_GAME_NAME")?.let { add("game.name" to it) }
        }
    }

    /** Reusable tickets are a development tool, and the site refuses a production start that finds them on. */
    private fun debugLogin(): String {
        val debug = flag("ZEL_DEBUG_LOGIN", "0")
        if (debug == "1" && value("ZEL_ENV") != "dev") {
            found += "ZEL_DEBUG_LOGIN=1 needs development mode (ZEL_ENV=dev, which only ZEL_PROXY=local takes)"
        }
        return debug
    }

    private fun avatarsPerAccount(email: String): String {
        val text = value("ZEL_WEB_AVATARS_PER_ACCOUNT") ?: return "1"
        val count = text.takeIf { AVATAR_COUNT.matches(it) }?.toInt()
        if (count == null || count > MAX_AVATARS) {
            found += "ZEL_WEB_AVATARS_PER_ACCOUNT must be a whole number from 1 to $MAX_AVATARS, not '$text'"
        } else if (count > 1 && email != "required") {
            found += "ZEL_WEB_AVATARS_PER_ACCOUNT=$count logs in by e-mail, so it needs ZEL_WEB_EMAIL=required " +
                "(not '$email')"
        }
        return text
    }

    private fun extraOrigins(): List<URI> =
        value("ZEL_EXTRA_ORIGINS")
            .orEmpty()
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .mapNotNull { origin("ZEL_EXTRA_ORIGINS", it) }

    private fun clientIpHeader(): String {
        val name = value("ZEL_CLIENT_IP_HEADER") ?: return "X-Forwarded-For"
        if (!HEADER_NAME.matches(name)) found += "ZEL_CLIENT_IP_HEADER must be a header name, not '$name'"
        return name
    }

    private fun trustedProxies(): String {
        val list = value("ZEL_TRUSTED_PROXIES") ?: return "127.0.0.1"
        if (!PROXY_LIST.matches(list)) {
            found += "ZEL_TRUSTED_PROXIES lists addresses or CIDR blocks, comma separated, not '$list'"
        }
        return list.replace(" ", "")
    }

    /** An origin is scheme, host and an optional port, nothing after it. */
    private fun origin(
        name: String,
        text: String?,
    ): URI? {
        if (text == null) return null
        val uri = runCatching { URI(text) }.getOrNull()
        val plain =
            uri != null && uri.scheme in ORIGIN_SCHEMES && uri.host != null && uri.rawPath.isNullOrEmpty() &&
                uri.rawQuery == null && uri.rawFragment == null && uri.rawUserInfo == null
        if (!plain) found += "$name must be an origin like https://game.example.org (no path), not '$text'"
        return uri.takeIf { plain }
    }

    /** Names of folders under custom/game-extensions, or absolute folders; startup only, never a settings row. */
    private fun gameExtensions(): String? {
        val text = value("ZEL_GAME_EXTENSIONS") ?: return null
        text.split(',', ' ', '\t', '\n').filter(String::isNotEmpty).forEach { entry ->
            if (!EXTENSION_NAME.matches(entry) && !EXTENSION_FOLDER.matches(entry)) {
                found += "ZEL_GAME_EXTENSIONS lists extension names (a-z, 0-9) or absolute folders, comma separated, " +
                    "not '$entry'"
            }
        }
        return text.replace(" ", "")
    }

    /** Unset is the default list; set to nothing is none. */
    private fun extensions(): String {
        val text = variables["ZEL_WEB_EXTENSIONS"] ?: return "guestbook"
        text.split(',', ' ', '\t', '\n').filter(String::isNotEmpty).forEach { entry ->
            if (!EXTENSION_NAME.matches(entry) && !EXTENSION_FOLDER.matches(entry)) {
                found += "ZEL_WEB_EXTENSIONS lists extension names (a-z, 0-9) or absolute folders, comma separated, " +
                    "not '$entry'"
            }
        }
        return text.replace(" ", "")
    }

    private fun value(name: String): String? = variables[name]?.takeIf(String::isNotEmpty)

    private fun wholeNumber(
        name: String,
        default: String,
        meaning: String,
    ): String {
        val text = value(name) ?: return default
        if (!WHOLE_NUMBER.matches(text)) found += "$name must be a whole number $meaning, not '$text'"
        return text
    }

    /** A zone name such as Europe/Amsterdam, as the game reads it; unset or empty is none. */
    private fun zone(name: String): String? {
        val text = value(name)?.trim()?.takeIf(String::isNotEmpty) ?: return null
        if (runCatching { ZoneId.of(text) }.exceptionOrNull() is DateTimeException) {
            found += "$name must be a time zone name like Europe/Amsterdam, not '$text'"
        }
        return text
    }

    private fun port(name: String): String? {
        val text = value(name) ?: return null
        if (!WHOLE_NUMBER.matches(text) || text.toInt() !in 1..MAX_PORT) {
            found += "$name must be a port number (1 to $MAX_PORT), not '$text'"
        }
        return text
    }

    private fun flag(
        name: String,
        default: String,
    ): String {
        val text = value(name) ?: return default
        if (text != "0" && text != "1") found += "$name must be 0 or 1, not '$text'"
        return text
    }

    private fun choice(
        name: String,
        default: String,
        modes: List<String>,
    ): String {
        val text = value(name) ?: return default
        if (text !in modes) {
            found += "$name must be ${modes.dropLast(1).joinToString()} or ${modes.last()}, not '$text'"
        }
        return text
    }

    private fun required(
        name: String,
        text: String?,
    ): String {
        if (text == null && found.none { it.startsWith("$name ") }) found += "$name is not set"
        return text.orEmpty()
    }

    companion object {
        /** The site as this machine reaches it, past any proxy and its certificate. */
        const val LOCAL_SITE_ROW = "site.local_url"
        const val DEFAULT_LOCAL_SITE = "http://127.0.0.1:8760"
        private const val MAX_PORT = 65535
        private const val MAX_AVATARS = 50
        private const val DEFAULT_WEB_PORT = "8760"
        private const val KEY_ROWS =
            "'login.key_exchange.is_on', 'login.key_exchange.modulus', 'login.key_exchange.private_exponent'"
        private val WHOLE_NUMBER = Regex("[0-9]{1,9}")
        private val DATA_FOLDERS = listOf("usercontent/badges", "furniture", "badgeparts")
        private val ORIGIN_SCHEMES = setOf("https", "http")
        private val EVERY_ADDRESS = setOf("0.0.0.0", "::", "[::]")
        private val HEADER_NAME = Regex("[A-Za-z0-9-]{1,64}")
        private val PROXY_LIST = Regex("[0-9A-Fa-f.:/, ]{1,512}")
        private val AVATAR_COUNT = Regex("[1-9][0-9]?")
        private val EXTENSION_NAME = Regex("[a-z][a-z0-9]{1,31}")
        private val EXTENSION_FOLDER = Regex("/[^,\\s]+")
        private val EMAIL_MODES = listOf("off", "optional", "required")
        private val HELP_MODES = listOf("account", "anyone", "off")

        /** Variable, row and default of the site's on/off switches. */
        private val SWITCHES =
            listOf(
                Triple("ZEL_WEB_PUBLIC_PHOTOS", "site.photos.are_public", "0"),
                Triple("ZEL_WEB_PUBLIC_PROFILES", "site.players.are_public", "0"),
                Triple("ZEL_WEB_PUBLIC_API", "site.api.is_public", "0"),
                Triple("ZEL_WEB_PUBLIC_COMMUNITY", "site.community.is_public", "1"),
                Triple("ZEL_WEB_STAFF_PAGE", "site.staff_page.is_shown", "1"),
                Triple("ZEL_WEB_AVATAR_IMAGES", "site.avatar_images.are_shown", "1"),
                Triple("ZEL_WEB_GUEST_ACCOUNTS", "site.guest_accounts.are_allowed", "0"),
            )
    }
}
