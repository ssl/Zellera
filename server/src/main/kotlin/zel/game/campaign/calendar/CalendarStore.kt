package zel.game.campaign.calendar

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Database
import zel.db.orFallback
import java.sql.ResultSet
import java.time.LocalDate

class CalendarStore(
    database: Database,
) {
    private val sql = database.queries

    /** Empty when the tables fail. */
    fun campaigns(): List<CalendarCampaign> =
        log.orFallback("calendar campaigns", emptyList()) {
            val doorPrizes =
                sql
                    .select(DOOR_PRIZES)
                    .rows { it.getInt("campaign_id") to it.prize() }
                    .groupBy({ it.first }, { it.second })
            sql.select(CAMPAIGNS).rows { it.campaign(doorPrizes[it.getInt("id")].orEmpty()) }
        }

    fun openedDays(
        playerId: Int,
        calendarId: Int,
    ): List<Int> =
        log.orFallback("calendar: opened doors of player $playerId", emptyList()) {
            sql.select(OPENED_DOORS, playerId, calendarId).rows { it.getInt("day") }
        }

    /** False when the door was open already: the row's key opens a door once per player. */
    fun claim(
        playerId: Int,
        calendarId: Int,
        day: Int,
        won: Int,
        now: Int,
    ): Boolean =
        log.orFallback("calendar: door opened by player $playerId", false) {
            sql.write(OPEN_DOOR, calendarId, playerId, day, won, now) > 0
        }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(CalendarStore::class.java)

        const val CAMPAIGNS =
            "SELECT id, name, image, first_day, last_day, catch_up_days FROM calendar_campaigns ORDER BY first_day, id"
        const val DOOR_PRIZES =
            "SELECT id, campaign_id, day, door_text, image, currency, amount, badge_code, definition_id, " +
                "membership, membership_days FROM calendar_prizes ORDER BY campaign_id, id"
        const val OPENED_DOORS =
            "SELECT day FROM calendar_opened_doors WHERE player_id = ? AND campaign_id = ? ORDER BY day"
        const val OPEN_DOOR =
            "INSERT IGNORE INTO calendar_opened_doors (campaign_id, player_id, day, prize_id, opened_at) " +
                "VALUES (?, ?, ?, ?, ?)"

        fun ResultSet.campaign(doorPrizes: List<CalendarPrize>) =
            CalendarCampaign(
                getInt("id"),
                getString("name"),
                getString("image"),
                getObject("first_day", LocalDate::class.java),
                getObject("last_day", LocalDate::class.java),
                getInt("catch_up_days").takeUnless { wasNull() },
                doorPrizes,
            )

        fun ResultSet.prize() =
            CalendarPrize(
                id = getInt("id"),
                text = getString("door_text"),
                image = getString("image"),
                currency = getInt("currency").takeUnless { wasNull() },
                amount = getInt("amount"),
                badge = getString("badge_code").orEmpty(),
                furniId = getInt("definition_id").takeIf { it > 0 },
                clubKind = getString("membership").orEmpty(),
                clubDays = getInt("membership_days"),
                day = getInt("day").takeUnless { wasNull() },
            )
    }
}
