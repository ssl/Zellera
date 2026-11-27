package zel.ext

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.SQLException
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

internal class StoredPlayerValues(
    private val extension: String,
    private val dataSource: DataSource,
    private val inOrder: (key: Any, job: Runnable) -> Unit,
) : PlayerValues {
    private class Entry {
        @Suppress("DoubleMutabilityForCollection")
        var values: MutableMap<String, Long>? = null
        val waiting = mutableListOf<(MutableMap<String, Long>) -> Unit>()
    }

    private val players = ConcurrentHashMap<Int, Entry>()

    override fun get(
        playerId: Int,
        name: String,
    ): Long? {
        val entry = entry(playerId)
        return synchronized(entry) { entry.values?.let { it[name] ?: 0L } }
    }

    override fun set(
        playerId: Int,
        name: String,
        value: Long,
    ) {
        change(playerId) { it[name] = value }
        save(playerId, SET, name, value)
    }

    override fun add(
        playerId: Int,
        name: String,
        delta: Long,
    ): Long? {
        val now = change(playerId) { it[name] = (it[name] ?: 0L) + delta }?.get(name)
        save(playerId, ADD, name, delta)
        return now
    }

    override fun prefetch(playerId: Int) {
        entry(playerId)
    }

    private fun change(
        playerId: Int,
        edit: (MutableMap<String, Long>) -> Unit,
    ): Map<String, Long>? {
        val entry = entry(playerId)
        return synchronized(entry) {
            val values = entry.values
            if (values == null) {
                entry.waiting += edit
                null
            } else {
                edit(values)
                values
            }
        }
    }

    private fun entry(playerId: Int): Entry {
        var fresh = false
        val entry = players.computeIfAbsent(playerId) { Entry().also { fresh = true } }
        if (fresh) inOrder(queue(playerId), Runnable { load(playerId, entry) })
        return entry
    }

    private fun load(
        playerId: Int,
        entry: Entry,
    ) {
        val loaded = HashMap<String, Long>()
        try {
            read(playerId, loaded)
        } catch (e: SQLException) {
            log.error("extension {}: reading the numbers of player {} failed", extension, playerId, e)
            players.remove(playerId, entry)
            return
        }
        synchronized(entry) {
            entry.waiting.forEach { it(loaded) }
            entry.waiting.clear()
            entry.values = loaded
        }
    }

    private fun read(
        playerId: Int,
        into: MutableMap<String, Long>,
    ) = dataSource.connection.use { c ->
        c.prepareStatement(SELECT).use { s ->
            s.setString(1, extension)
            s.setInt(2, playerId)
            s.executeQuery().use { r -> while (r.next()) into[r.getString(1)] = r.getLong(2) }
        }
    }

    private fun save(
        playerId: Int,
        sql: String,
        name: String,
        value: Long,
    ) {
        inOrder(
            queue(playerId),
            Runnable {
                try {
                    dataSource.connection.use { c ->
                        c.prepareStatement(sql).use { s ->
                            s.setString(EXTENSION, extension)
                            s.setInt(PLAYER, playerId)
                            s.setString(NAME, name)
                            s.setLong(VALUE, value)
                            s.executeUpdate()
                        }
                    }
                } catch (e: SQLException) {
                    log.error("extension {}: saving {} of player {} failed", extension, name, playerId, e)
                }
            },
        )
    }

    private fun queue(playerId: Int): String = "extension:$extension:$playerId"

    private companion object {
        val log: Logger = LoggerFactory.getLogger(StoredPlayerValues::class.java)

        const val EXTENSION = 1
        const val PLAYER = 2
        const val NAME = 3
        const val VALUE = 4

        const val SELECT = "SELECT `name`, `value` FROM extension_player_values WHERE extension = ? AND player_id = ?"

        const val SET =
            "INSERT INTO extension_player_values (extension, player_id, `name`, `value`) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE `value` = VALUES(`value`)"

        const val ADD =
            "INSERT INTO extension_player_values (extension, player_id, `name`, `value`) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE `value` = `value` + VALUES(`value`)"
    }
}
