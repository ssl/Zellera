package zel.game.moderation

import zel.db.Sql
import zel.db.orFallback
import zel.room.port.RoomPort
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import javax.sql.DataSource

/** The mod tool's user classification (`:uc`): nearby accounts, each classed by age and shared machine. */
class PlayerClassifier(
    dataSource: DataSource,
    private val live: () -> RoomPort,
    private val now: () -> Int,
) {
    private val queries = Sql(dataSource)

    data class Classified(
        val id: Int,
        val name: String,
        val kind: String,
    )

    enum class Kind(
        val label: String,
    ) {
        NEW("new account"),
        SHARED_MACHINE("shared machine"),
        REGULAR("regular"),
    }

    private class Account(
        val id: Int,
        val name: String,
        val createdAt: Int,
        val machineId: String,
    )

    /** The named player and every account on its machine or address, oldest first; nobody for an unknown name. */
    fun peersOf(name: String): List<Classified> =
        log.orFallback("classifying the peers of $name", emptyList()) {
            classify(queries.select(PEERS, name, name, name).rows(::account))
        }

    fun inRoom(roomId: Int): List<Classified> {
        val ids = live().playersIn(roomId).take(MAX_LISTED)
        if (ids.isEmpty()) return emptyList()
        return log.orFallback("classifying room $roomId", emptyList()) {
            classify(queries.select(IN_ROOM + marks(ids.size)) { ids.forEach { value(it) } }.rows(::account))
        }
    }

    private fun classify(accounts: List<Account>): List<Classified> {
        val machines = accounts.map { it.machineId }.filter { it.isNotEmpty() }.distinct()
        val shared =
            if (machines.isEmpty()) {
                emptySet()
            } else {
                queries
                    .select(SHARED_MACHINES + marks(machines.size) + SHARED_MACHINES_TAIL) {
                        machines.forEach { value(it) }
                    }.rows { it.getString(1) }
                    .toSet()
            }
        val at = now()
        return accounts.map { Classified(it.id, it.name, kindOf(it.createdAt, at, it.machineId in shared).label) }
    }

    private fun account(row: ResultSet) =
        Account(
            row.getInt("id"),
            row.getString("name"),
            row.getInt("created_at"),
            row.getString("machine_id").orEmpty(),
        )

    companion object {
        private val log = LoggerFactory.getLogger(PlayerClassifier::class.java)
        private const val MAX_LISTED = 100
        private const val NEW_ACCOUNT_SECONDS = 24 * 60 * 60

        private const val COLUMNS =
            "SELECT p.id, p.name, p.created_at, a.machine_id " +
                "FROM players p JOIN player_accounts a ON a.id = p.account_id "
        private const val PEERS =
            COLUMNS + "WHERE p.name = ? OR (a.machine_id <> '' AND a.machine_id = (SELECT a2.machine_id " +
                "FROM player_accounts a2 JOIN players p2 ON p2.account_id = a2.id WHERE p2.name = ?)) " +
                "OR (a.last_ip <> '' AND a.last_ip = (SELECT a3.last_ip FROM player_accounts a3 " +
                "JOIN players p3 ON p3.account_id = a3.id WHERE p3.name = ?)) ORDER BY p.id LIMIT $MAX_LISTED"
        private const val IN_ROOM = COLUMNS + "WHERE p.id IN "
        private const val SHARED_MACHINES = "SELECT machine_id FROM player_accounts WHERE machine_id IN "
        private const val SHARED_MACHINES_TAIL = " GROUP BY machine_id HAVING COUNT(*) > 1"

        private fun marks(count: Int): String = (1..count).joinToString(",", "(", ")") { "?" }

        fun kindOf(
            createdAt: Int,
            now: Int,
            machineShared: Boolean,
        ): Kind =
            when {
                now - createdAt < NEW_ACCOUNT_SECONDS -> Kind.NEW
                machineShared -> Kind.SHARED_MACHINE
                else -> Kind.REGULAR
            }
    }
}
