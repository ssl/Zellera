package zel.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.core.config.ConfigRegistry
import java.util.concurrent.CopyOnWriteArrayList
import javax.sql.DataSource

/**
 * Settings in layers: the startup keys over the `settings` table. A value its key cannot be read from is never
 * served: at the start the key's default stands in, and at a reload the value the server was running on stays, so a
 * typing mistake in the table cannot throw a running game back to a default.
 */
class Configuration(
    startup: Map<String, String>,
    dataSource: () -> DataSource? = { null },
) {
    private class Layers(
        val startup: Map<String, String>,
        val table: Map<String, String>,
    ) {
        operator fun get(key: String): String? = startup[key] ?: table[key]
    }

    private val table = NameValueTable("settings", dataSource)
    private val reloadListeners = CopyOnWriteArrayList<() -> Unit>()
    private val startup = usable(startup, "startup key", emptyMap())

    @Volatile
    private var layers = Layers(this.startup, emptyMap())

    @Volatile
    private var tableLoaded = false

    fun onReload(listener: () -> Unit) {
        reloadListeners += listener
    }

    /** A startup key keeps its startup value. */
    fun loadTable() {
        tableLoaded = true
        layers = Layers(startup, usable(readTable().orEmpty(), "setting", emptyMap()))
    }

    /** The first read may come before the pool is open, over a connection source of its own. */
    fun loadTable(source: DataSource) {
        tableLoaded = true
        val read = readTable(NameValueTable("settings") { source }).orEmpty()
        layers = Layers(startup, usable(read, "setting", emptyMap()))
    }

    fun reload() {
        if (tableLoaded) {
            val read = readTable()
            if (read == null) {
                log.warn("settings could not be read, the loaded settings stay")
            } else {
                layers = Layers(startup, usable(read, "setting", layers.table))
            }
        }
        reloadListeners.forEach { it() }
    }

    fun valueOrNull(key: String): String? = layers[key]

    fun startupValueOrNull(key: String): String? = startup[key]

    fun getValue(key: String): String = getValue(key, "")

    fun getValue(
        key: String,
        default: String,
    ): String = valueOrNull(key) ?: default

    /** `true`/`1` and `false`/`0` in any case and spacing; anything else is the default. */
    fun getBoolean(
        key: String,
        default: Boolean = false,
    ): Boolean =
        when (valueOrNull(key)?.trim()?.lowercase()) {
            in TRUE_WORDS -> true
            in FALSE_WORDS -> false
            else -> default
        }

    fun getInt(
        key: String,
        default: Int = 0,
    ): Int = valueOrNull(key)?.trim()?.toIntOrNull() ?: default

    fun getDouble(
        key: String,
        default: Double = 0.0,
    ): Double = valueOrNull(key)?.trim()?.toDoubleOrNull() ?: default

    private fun readTable(from: NameValueTable = table): Map<String, String>? {
        val rows = mutableMapOf<String, String>()
        return if (from.each { name, value -> rows[name] = value }) rows else null
    }

    /** [values] with each unusable one replaced by what [running] holds for its key, or left out. */
    private fun usable(
        values: Map<String, String>,
        what: String,
        running: Map<String, String>,
    ): Map<String, String> {
        val refused = ConfigRegistry.shipped.unusableIn(values)
        if (refused.isEmpty()) return values
        val kept = values.toMutableMap()
        refused.forEach { (key, value, problem) ->
            val before = running[key]
            if (before == null) kept.remove(key) else kept[key] = before
            log.warn("{} {}={} is not usable ({}): {}", what, key, value, problem, standIn(before))
        }
        return kept
    }

    private fun standIn(before: String?) = if (before == null) "the default applies" else "it stays $before"

    private companion object {
        val log: Logger = LoggerFactory.getLogger(Configuration::class.java)
        val TRUE_WORDS = setOf("true", "1")
        val FALSE_WORDS = setOf("false", "0")
    }
}
