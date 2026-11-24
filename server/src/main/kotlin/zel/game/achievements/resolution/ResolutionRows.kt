package zel.game.achievements.resolution

import zel.db.Sql
import java.sql.ResultSet
import javax.sql.DataSource

class ResolutionRows(
    dataSource: DataSource,
) : ResolutionStore {
    private val sql = Sql(dataSource)

    override fun byFurni(furniId: Int): Resolution? = sql.select(BY_FURNI, furniId).first { it.asResolution() }

    override fun openAchievementIds(playerId: Int): Set<Int> =
        buildSet { sql.select(OPEN_OF_PLAYER, playerId).each { add(it.getInt("achievement_id")) } }

    override fun add(resolution: Resolution): Boolean =
        sql.write(INSERT) { resolution.insertValues().forEach { value(it) } } == 1

    override fun markCompleted(
        furniId: Int,
        now: Int,
    ) {
        sql.write(MARK_COMPLETED, now, furniId)
    }

    override fun delete(furniId: Int) {
        sql.write(DELETE_BY_FURNI, furniId)
    }

    private companion object {
        const val TABLE = "achievement_resolutions"
        const val COLUMNS = "furni_id, player_id, achievement_id, target_level, ends_at"
        const val BY_FURNI = "SELECT $COLUMNS, completed_at FROM $TABLE WHERE furni_id = ?"
        const val OPEN_OF_PLAYER =
            "SELECT achievement_id FROM $TABLE WHERE player_id = ? AND completed_at IS NULL AND ends_at > UNIX_TIMESTAMP()"
        const val INSERT = "INSERT IGNORE INTO $TABLE ($COLUMNS) VALUES (?, ?, ?, ?, ?)"
        const val MARK_COMPLETED = "UPDATE $TABLE SET completed_at = ? WHERE furni_id = ? AND completed_at IS NULL"
        const val DELETE_BY_FURNI = "DELETE FROM $TABLE WHERE furni_id = ?"

        fun Resolution.insertValues(): List<Any> = listOf(furniId, playerId, achievement, level, endsAt)

        fun ResultSet.asResolution() =
            Resolution(
                furniId = getInt("furni_id"),
                playerId = getInt("player_id"),
                achievement = getInt("achievement_id"),
                level = getInt("target_level"),
                endsAt = getInt("ends_at"),
                completedAt = getInt("completed_at"),
            )
    }
}
