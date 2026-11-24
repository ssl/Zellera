package zel.game.permissions

import zel.db.Sql
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class PermissionsModule(
    dataSource: DataSource,
    now: () -> Int,
    competitionVoteMinRank: () -> Int = { Permissions.STANDARD_VOTE_MIN_RANK },
) {
    val permissions: Permissions

    init {
        val sql = Sql(dataSource)
        val tables = RankTables(sql)
        val stored = tables.ranks()
        if (stored == null) LOGGER.error("No ranks or permission keys are stored; every key is denied")
        val ranks = RankBook(stored.orEmpty())
        permissions = Permissions(ranks, RankLog(sql, now), tables.effectLimits(), competitionVoteMinRank)
        LOGGER.info("{} ranks loaded", ranks.all.size)
    }

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(PermissionsModule::class.java)
    }
}
