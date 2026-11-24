package zel.game.permissions

import zel.db.Sql
import java.sql.ResultSet

internal class RankTables(
    private val sql: Sql,
) {
    /** Every stored rank by id, or null when no rank or no permission key is stored at all. */
    fun ranks(): Map<Int, Rank>? {
        if (!anyKeyStored()) return null
        val stored = buildMap { sql.select("SELECT * FROM ranks").each { put(it.getInt("id"), bareRank(it)) } }
        if (stored.isEmpty()) return null
        val held = heldByRank()
        return stored.mapValues { (id, rank) -> rank.copy(holds = held[id].orEmpty()) }
    }

    fun effectLimits(): Map<Int, Int> =
        buildMap {
            sql.select("SELECT effect_id, min_rank FROM effect_rank_limits").each {
                put(it.getInt("effect_id"), it.getInt("min_rank"))
            }
        }

    private fun anyKeyStored(): Boolean =
        sql.select("SELECT code FROM permissions").first { it.getString("code") } != null

    private fun bareRank(row: ResultSet): Rank =
        Rank(
            row.getInt("id"),
            row.getString("name"),
            row.getString("badge_code"),
            row.getBoolean("log_commands"),
            row.getInt("max_rooms"),
            row.getInt("max_friends"),
            row.getInt("max_favourite_rooms"),
        )

    private fun heldByRank(): Map<Int, Set<String>> =
        sql
            .select("SELECT rank_id, permission_code FROM rank_permissions")
            .rows { it.getInt("rank_id") to it.getString("permission_code") }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, codes) -> codes.toSet() }
}
