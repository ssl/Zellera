package zel.content

import zel.db.executeAll
import zel.db.first
import zel.db.rows
import zel.db.write
import zel.db.writeEach
import java.sql.Connection
import java.util.Locale

/** A pack player whose name can already log in is refused. */
internal class ContentIds(
    private val connection: Connection,
    private val staged: Map<String, List<String>>,
    private val stageName: (String) -> String,
) {
    data class Player(
        val id: Long,
        val name: String,
        val created: Boolean,
    )

    class Outcome(
        val players: List<Player>,
        val moved: Map<IdSpace, Int>,
        val roomIds: String,
    )

    fun assign(): Outcome {
        val moved = LinkedHashMap<IdSpace, Int>()
        val players = assignPlayers(moved)
        val contentRooms =
            connection
                .rows(
                    "SELECT id FROM rooms WHERE owner_id IN (${sqlList(players.map { it.id })}) " +
                        "OR (owner_id IS NULL AND id IN (SELECT room_id FROM ${ContentSections.ROOM_INDEX_TABLE}))",
                ) { it.getLong(1) }
                .toSet()
        val packRooms = stagedIds(IdSpace.ROOM)
        val heldRooms =
            if (packRooms.isEmpty()) {
                emptyList()
            } else {
                connection.rows("SELECT id FROM rooms WHERE id IN (${sqlList(packRooms)})") { it.getLong(1) }
            }
        val roomMap = nextFree(IdSpace.ROOM, heldRooms.filterNot { it in contentRooms })
        apply(IdSpace.ROOM, roomMap, moved)
        val roomIds = sqlList((contentRooms + packRooms.map { roomMap[it] ?: it }).sorted())
        for (space in listOf(IdSpace.GROUP, IdSpace.BOT, IdSpace.FURNI)) {
            val owner = ContentSections.OWNERS.getValue(space)
            if (owner.id !in staged) continue
            val scope = checkNotNull(owner.roomScope).format(Locale.ROOT, roomIds)
            val held =
                connection.rows(
                    "SELECT id FROM `${owner.table}` WHERE id IN (SELECT id FROM ${stageName(owner.id)}) " +
                        "AND NOT COALESCE(($scope), FALSE)",
                ) { it.getLong(1) }
            val map = nextFree(space, held)
            apply(space, map, moved)
            if (space == IdSpace.FURNI) renumberTexts(map)
        }
        return Outcome(players, moved, roomIds)
    }

    private fun assignPlayers(moved: MutableMap<IdSpace, Int>): List<Player> {
        val section = ContentSections.OWNERS.getValue(IdSpace.PLAYER)
        if (section.id !in staged) return emptyList()
        val rows =
            connection.rows("SELECT id, name FROM ${stageName(section.id)} ORDER BY id") {
                it.getLong(1) to
                    it.getString(2)
            }
        var next = maxOf(maxId("players"), rows.maxOfOrNull { it.first } ?: 0) + 1
        val given = HashSet<Long>()
        val map = LinkedHashMap<Long, Long>()
        val players =
            rows.map { (packId, name) ->
                val found =
                    connection.first(
                        "SELECT id, account_id IS NOT NULL FROM players WHERE name = ? LIMIT 1",
                        { string(name) },
                    ) { it.getLong(1) to it.getBoolean(2) }
                if (found?.second == true) {
                    throw ContentPackException("the pack's player $name is a player of this game who can log in")
                }
                val id = found?.first ?: packId.takeIf { it !in given && idFree(it) } ?: next++
                given += id
                if (id != packId) map[packId] = id
                Player(id, name, found == null)
            }
        apply(IdSpace.PLAYER, map, moved)
        return players
    }

    private fun idFree(id: Long) = connection.first("SELECT 1 FROM players WHERE id = ?", { long(id) }) { true } == null

    private fun stagedIds(space: IdSpace): List<Long> {
        val owner = ContentSections.OWNERS.getValue(space)
        if (owner.id !in staged) return emptyList()
        return connection.rows("SELECT id FROM ${stageName(owner.id)}") { it.getLong(1) }
    }

    private fun nextFree(
        space: IdSpace,
        held: List<Long>,
    ): Map<Long, Long> {
        if (held.isEmpty()) return emptyMap()
        val owner = ContentSections.OWNERS.getValue(space)
        val packMax = connection.first("SELECT MAX(id) FROM ${stageName(owner.id)}") { it.getLong(1) } ?: 0
        var next = maxOf(maxId(owner.table), packMax) + 1
        return held.sorted().associateWith { next++ }
    }

    private fun maxId(table: String) =
        connection.first("SELECT COALESCE(MAX(id), 0) FROM `$table`") { it.getLong(1) } ?: 0

    /** Goes via negated ids, so no row passes through an id still held. */
    private fun apply(
        space: IdSpace,
        map: Map<Long, Long>,
        moved: MutableMap<IdSpace, Int>,
    ) {
        if (map.isEmpty()) return
        moved[space] = map.size
        connection.executeAll(
            "DROP TEMPORARY TABLE IF EXISTS $MAP_TABLE",
            "CREATE TEMPORARY TABLE $MAP_TABLE (old BIGINT PRIMARY KEY, new BIGINT NOT NULL)",
        )
        connection.writeEach("INSERT INTO $MAP_TABLE (old, new) VALUES (?, ?)", map.entries) { (old, new) ->
            long(old)
            long(new)
        }
        for ((sectionId, columns) in staged) {
            val section = ContentSections.BY_ID.getValue(sectionId)
            val named =
                section.refs.filterValues { it == space }.keys + listOfNotNull(ID.takeIf { section.owns == space })
            for (column in named.filter { it in columns }) {
                val stage = stageName(sectionId)
                connection.executeAll(
                    "UPDATE $stage s JOIN $MAP_TABLE m ON s.`$column` = m.old SET s.`$column` = -m.new",
                    "UPDATE $stage SET `$column` = -`$column` WHERE `$column` < 0",
                )
            }
        }
        connection.executeAll("DROP TEMPORARY TABLE $MAP_TABLE")
    }

    /** Furni named in text columns follow [map] too; a shared wired variable names its box last. */
    private fun renumberTexts(map: Map<Long, Long>) {
        if (map.isEmpty()) return
        for ((sectionId, columns) in staged) {
            val section = ContentSections.BY_ID.getValue(sectionId)
            for (column in section.textRefs.filter { it in columns }) {
                val stage = stageName(sectionId)
                val texts =
                    connection.rows("SELECT DISTINCT `$column` FROM $stage WHERE `$column` LIKE '%shared:%'") {
                        it.getString(1)
                    }
                for (text in texts) {
                    val renumbered =
                        SHARED_VARIABLE.replace(text) { m ->
                            val id = m.groupValues[2].toLong()
                            "${m.groupValues[1]}${map[id] ?: id}\""
                        }
                    if (renumbered == text) continue
                    connection.write("UPDATE $stage SET `$column` = ? WHERE `$column` = ?") {
                        string(renumbered)
                        string(text)
                    }
                }
            }
        }
    }

    private companion object {
        const val ID = "id"
        const val MAP_TABLE = "content_id_map"
        val SHARED_VARIABLE = Regex("(\"shared:\\d+:\\d+:)(\\d+)\"")

        fun sqlList(ids: Collection<Long>) = ids.joinToString(",").ifEmpty { "NULL" }
    }
}
