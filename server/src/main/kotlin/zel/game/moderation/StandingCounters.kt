package zel.game.moderation

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.SqlFailure
import javax.sql.DataSource

class StandingCounters(
    dataSource: DataSource,
) {
    private val sql = Sql(dataSource)

    fun count(
        playerId: Int,
        counter: StandingCounter,
    ) {
        val column = counter.column
        try {
            sql.write("UPDATE player_standing SET $column = $column + 1 WHERE player_id = ?", playerId)
        } catch (failure: SqlFailure) {
            log.error("standing {} of player {} not raised", column, playerId, failure)
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(StandingCounters::class.java)
    }
}
