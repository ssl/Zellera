package zel.wired.signal

import zel.db.rows
import zel.room.furni.buildpass.BuildPassRowIds
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

/**
 * A known network answers from memory and refreshes in [background] once a second old, so a repeater sending every
 * tick does not query under its room's lock; only the first send of a link waits for the database.
 */
class JdbcLinkNetworks(
    private val dataSource: DataSource,
    private val background: (Runnable) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) : LinkNetworks {
    private val cache = ConcurrentHashMap<Int, Cached>()
    private val refreshing = ConcurrentHashMap.newKeySet<Int>()

    override fun members(linkId: Int): List<LinkMember> {
        val now = clock()
        val known = cache[linkId] ?: return query(linkId).also { cache[linkId] = Cached(now, it) }
        if (now - known.at >= CACHE_MILLIS && refreshing.add(linkId)) {
            background {
                try {
                    cache[linkId] = Cached(clock(), query(linkId))
                } finally {
                    refreshing.remove(linkId)
                }
            }
        }
        return known.members
    }

    /** After a network edit the next send reads it again. */
    fun forget() = cache.clear()

    /** The Link itself is always among them, network or not, so one that hears its own room keeps hearing it. */
    private fun query(linkId: Int): List<LinkMember> =
        dataSource.connection.use { c ->
            c.rows(SQL, { int(BuildPassRowIds.rowOf(c, linkId)) }) {
                LinkMember(it.getInt("link_id"), it.getInt("room_id"), it.getBoolean("hears_own_room"))
            }
        }

    private class Cached(
        val at: Long,
        val members: List<LinkMember>,
    )

    private companion object {
        const val CACHE_MILLIS = 1_000L
        const val SQL =
            "SELECT ${BuildPassRowIds.CLIENT_FACING} AS link_id, i.room_id, f.hears_own_room " +
                "FROM wired_link_members s " +
                "JOIN wired_link_members f ON (f.network_id = s.network_id OR f.furni_id = s.furni_id) " +
                "JOIN furni i ON i.id = f.furni_id " +
                "${BuildPassRowIds.JOIN} " +
                "WHERE s.furni_id = ? AND i.room_id IS NOT NULL"
    }
}
