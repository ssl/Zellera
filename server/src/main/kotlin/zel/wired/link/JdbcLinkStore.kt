package zel.wired.link

import zel.db.Sql
import zel.db.SqlFailure
import zel.db.insertForId
import zel.room.furni.buildpass.BuildPassRowIds
import java.sql.ResultSet
import java.sql.SQLIntegrityConstraintViolationException
import javax.sql.DataSource

class JdbcLinkStore(
    private val dataSource: DataSource,
) : LinkStore {
    private val sql = Sql(dataSource)

    override fun networks(ownerId: Int): List<LinkNetwork> = grouped(sql.select(ALL_OWNED, ownerId).rows(::memberRow))

    override fun network(
        ownerId: Int,
        networkId: Int,
    ): LinkNetwork? = grouped(sql.select(ONE_OWNED, networkId, ownerId).rows(::memberRow)).firstOrNull()

    override fun count(ownerId: Int): Int = sql.select(COUNT_OWNED, ownerId).first { it.getInt(1) } ?: 0

    override fun create(
        ownerId: Int,
        name: String,
    ): Int? =
        nameFree {
            dataSource.connection.use { connection ->
                connection.insertForId(INSERT_NETWORK) {
                    int(ownerId)
                    string(name)
                }
            }
        }

    private fun memberRow(row: ResultSet) = MemberRow(row.getInt("id"), row.getString("name"), row.intOrNull("link_id"))

    private fun grouped(rows: List<MemberRow>): List<LinkNetwork> =
        rows
            .groupBy(MemberRow::networkId)
            .map { (id, members) -> LinkNetwork(id, members.first().name, members.mapNotNull(MemberRow::linkId)) }

    override fun rename(
        ownerId: Int,
        networkId: Int,
        name: String,
    ): Boolean = nameFree { sql.write(RENAME_OWNED, name, networkId, ownerId) == 1 } == true

    override fun delete(
        ownerId: Int,
        networkId: Int,
    ) {
        sql.write(DELETE_OWNED, networkId, ownerId)
    }

    override fun connections(networkId: Int): List<LinkConnection> =
        sql.select(MEMBERS_BY_ROOM, networkId).rows { row ->
            LinkConnection(
                roomId = row.getInt("room_id"),
                roomName = row.getString("name"),
                linkId = row.getInt("item_id"),
                hearsOwnRoom = row.getBoolean("hears_own_room"),
            )
        }

    override fun setup(linkId: Int): LinkSetup =
        sql.select(MEMBER, linkId, linkId).first { row ->
            LinkSetup(row.intOrNull("network_id"), row.getBoolean("hears_own_room"))
        } ?: LinkSetup.NONE

    override fun save(
        linkId: Int,
        setup: LinkSetup,
    ) {
        sql.write(UPSERT_MEMBER, setup.networkId, setup.hearsOwnRoom, linkId, linkId)
    }

    override fun disconnect(linkId: Int) {
        sql.write(DELETE_MEMBER, linkId, linkId)
    }

    private data class MemberRow(
        val networkId: Int,
        val name: String,
        val linkId: Int?,
    )

    private fun ResultSet.intOrNull(column: String): Int? = getInt(column).takeUnless { wasNull() }

    private fun <T> nameFree(write: () -> T): T? =
        try {
            write()
        } catch (_: SQLIntegrityConstraintViolationException) {
            null
        } catch (failure: SqlFailure) {
            if (failure.cause is SQLIntegrityConstraintViolationException) null else throw failure
        }

    private companion object {
        const val LINK_ROW = "COALESCE((SELECT furni_id FROM build_pass_furni WHERE client_id = ?), ?)"

        const val NETWORKS =
            "SELECT n.id, n.name, IF(i.id IS NULL, NULL, ${BuildPassRowIds.CLIENT_FACING}) AS link_id " +
                "FROM wired_link_networks n " +
                "LEFT JOIN wired_link_members f ON f.network_id = n.id " +
                "LEFT JOIN furni i ON i.id = f.furni_id AND i.room_id IS NOT NULL " +
                "${BuildPassRowIds.JOIN}"
        const val ALL_OWNED = "$NETWORKS WHERE n.player_id = ? ORDER BY n.id, i.id"
        const val ONE_OWNED = "$NETWORKS WHERE n.id = ? AND n.player_id = ? ORDER BY i.id"
        const val COUNT_OWNED = "SELECT COUNT(*) FROM wired_link_networks WHERE player_id = ?"
        const val INSERT_NETWORK = "INSERT INTO wired_link_networks (player_id, name) VALUES (?, ?)"
        const val RENAME_OWNED = "UPDATE wired_link_networks SET name = ? WHERE id = ? AND player_id = ?"
        const val DELETE_OWNED = "DELETE FROM wired_link_networks WHERE id = ? AND player_id = ?"
        const val MEMBERS_BY_ROOM =
            "SELECT i.room_id, r.name, ${BuildPassRowIds.CLIENT_FACING} AS item_id, f.hears_own_room " +
                "FROM wired_link_members f " +
                "JOIN furni i ON i.id = f.furni_id JOIN rooms r ON r.id = i.room_id " +
                "${BuildPassRowIds.JOIN} " +
                "WHERE f.network_id = ? ORDER BY i.room_id, f.furni_id"
        const val MEMBER = "SELECT network_id, hears_own_room FROM wired_link_members WHERE furni_id = $LINK_ROW"
        const val UPSERT_MEMBER =
            "INSERT INTO wired_link_members (network_id, hears_own_room, furni_id) SELECT ?, ?, $LINK_ROW " +
                "ON DUPLICATE KEY UPDATE network_id = VALUES(network_id), hears_own_room = VALUES(hears_own_room)"
        const val DELETE_MEMBER = "DELETE FROM wired_link_members WHERE furni_id = $LINK_ROW"
    }
}
