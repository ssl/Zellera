package zel.room.side

import zel.db.Sql
import zel.room.DoorMode
import javax.sql.DataSource

internal class CompetitionStore(
    dataSource: DataSource,
) {
    private val sql = Sql(dataSource)

    /** [roomId] is null once the room is gone; the entry still holds its player's one place. */
    data class Entry(
        val roomId: Int?,
    )

    data class RoomFacts(
        val createdAt: Int,
        val doorOpen: Boolean,
    )

    data class Candidate(
        val roomId: Int,
        val votedFor: Boolean,
    )

    fun running(at: Int): Competition? {
        val found =
            sql.select(RUNNING, at, at).first {
                Competition(
                    it.getInt("id"),
                    it.getString("code"),
                    emptyList(),
                    it.getInt("entries_from"),
                    it.getInt("voting_from"),
                    it.getInt("ends_at"),
                    it.getInt("votes_per_day"),
                    it.getInt("rooms_newer_than"),
                )
            } ?: return null
        return found.copy(requiredFurni = sql.select(REQUIRED_FURNI, found.id).rows { it.getString(1) })
    }

    fun entryOf(
        competitionId: Int,
        playerId: Int,
    ): Entry? =
        sql.select(ENTRY_OF_PLAYER, competitionId, playerId).first { Entry(it.getInt(1).takeIf { id -> id > 0 }) }

    fun isEntered(
        competitionId: Int,
        roomId: Int,
    ): Boolean = sql.select(ROOM_IS_ENTERED, competitionId, roomId).first { true } ?: false

    fun roomFacts(roomId: Int): RoomFacts? =
        sql.select(ROOM_FACTS, roomId).first { RoomFacts(it.getInt(1), it.getString(2) == DoorMode.OPEN) }

    fun furniCodesIn(roomId: Int): Set<String> =
        sql.select(FURNI_CODES_IN_ROOM, roomId).rows { it.getString(1) }.toSet()

    /** False when the player or the room has an entry already. */
    fun enter(
        competitionId: Int,
        playerId: Int,
        roomId: Int,
        at: Int,
    ): Boolean = sql.write(ENTER, competitionId, playerId, roomId, at) == 1

    fun votesCastOn(
        competitionId: Int,
        voterId: Int,
        day: Int,
    ): Int = sql.select(VOTES_OF_DAY, competitionId, voterId, day).first { it.getInt(1) } ?: 0

    /**
     * One statement, so the day's allowance holds against two votes at once: counts only for another player's entry
     * the voter has not voted for, while fewer than [perDay] votes of the day are his.
     */
    fun castVote(
        competition: Competition,
        roomId: Int,
        voterId: Int,
        day: Int,
    ): Boolean =
        sql.write(CAST_VOTE) {
            int(voterId)
            int(day)
            int(competition.id)
            int(roomId)
            int(voterId)
            int(voterId)
            int(day)
            int(competition.votesPerDay)
        } == 1

    fun standingRooms(competitionId: Int): Int = sql.select(STANDING_ROOMS, competitionId).first { it.getInt(1) } ?: 0

    fun standing(
        competitionId: Int,
        skip: Int,
        take: Int,
    ): List<Int> = sql.select(STANDING, competitionId, skip, take).rows { it.getInt(1) }

    fun othersRooms(
        competitionId: Int,
        playerId: Int,
    ): List<Candidate> =
        sql.select(OTHERS_ROOMS, playerId, competitionId, playerId).rows { Candidate(it.getInt(1), it.getBoolean(2)) }

    private companion object {
        const val RUNNING =
            "SELECT id, code, entries_from, voting_from, ends_at, votes_per_day, rooms_newer_than " +
                "FROM room_competitions WHERE entries_from <= ? AND ends_at > ? " +
                "ORDER BY entries_from DESC, id DESC LIMIT 1"
        const val REQUIRED_FURNI =
            "SELECT furni_code FROM room_competition_furni WHERE competition_id = ? ORDER BY position"
        const val ENTRY_OF_PLAYER =
            "SELECT COALESCE(room_id, 0) FROM room_competition_entries WHERE competition_id = ? AND player_id = ?"
        const val ROOM_IS_ENTERED = "SELECT 1 FROM room_competition_entries WHERE competition_id = ? AND room_id = ?"
        const val ROOM_FACTS = "SELECT created_at, door_mode FROM rooms WHERE id = ?"
        const val FURNI_CODES_IN_ROOM =
            "SELECT DISTINCT b.code FROM furni i INNER JOIN furni_definitions b ON b.id = i.definition_id WHERE i.room_id = ?"
        const val ENTER =
            "INSERT IGNORE INTO room_competition_entries (competition_id, player_id, room_id, entered_at) VALUES (?, ?, ?, ?)"
        const val VOTES_OF_DAY =
            "SELECT COUNT(*) FROM room_competition_votes WHERE competition_id = ? AND voter_id = ? AND day_number = ?"
        const val CAST_VOTE =
            "INSERT IGNORE INTO room_competition_votes (competition_id, entrant_id, voter_id, day_number) " +
                "SELECT e.competition_id, e.player_id, ?, ? FROM room_competition_entries e " +
                "WHERE e.competition_id = ? AND e.room_id = ? AND e.player_id <> ? AND " +
                "(SELECT COUNT(*) FROM room_competition_votes v WHERE v.competition_id = e.competition_id " +
                "AND v.voter_id = ? AND v.day_number = ?) < ?"
        const val STANDING_ROOMS = "SELECT COUNT(room_id) FROM room_competition_entries WHERE competition_id = ?"
        const val STANDING =
            "SELECT e.room_id FROM room_competition_entries e LEFT JOIN room_competition_votes v " +
                "ON v.competition_id = e.competition_id AND v.entrant_id = e.player_id " +
                "WHERE e.competition_id = ? AND e.room_id IS NOT NULL GROUP BY e.player_id, e.room_id, e.entered_at " +
                "ORDER BY COUNT(v.voter_id) DESC, e.entered_at, e.player_id LIMIT ?, ?"
        const val OTHERS_ROOMS =
            "SELECT e.room_id, v.voter_id IS NOT NULL FROM room_competition_entries e " +
                "LEFT JOIN room_competition_votes v " +
                "ON v.competition_id = e.competition_id AND v.entrant_id = e.player_id AND v.voter_id = ? " +
                "WHERE e.competition_id = ? AND e.room_id IS NOT NULL AND e.player_id <> ?"
    }
}
