package zel.room.side

import zel.game.achievements.GameEvents
import zel.protocol.generated.CompetitionEntrySubmitResultOut
import zel.protocol.generated.CompetitionVotingInfoOut
import zel.protocol.generated.IsPlayerPartOfCompetitionOut
import zel.room.listing.RoomListings
import javax.sql.DataSource
import kotlin.random.Random

class RoomCompetitions internal constructor(
    private val store: CompetitionStore,
    listings: RoomListings,
    private val now: () -> Int,
    private val progress: (playerId: Int, event: String, amount: Int) -> Unit,
    private val random: Random,
) {
    constructor(
        dataSource: DataSource,
        listings: RoomListings,
        now: () -> Int,
        progress: (playerId: Int, event: String, amount: Int) -> Unit,
    ) : this(CompetitionStore(dataSource), listings, now, progress, Random.Default)

    val standing = CompetitionStanding(store, listings)

    fun current(): Competition? = store.running(now())

    fun phase(competition: Competition): Competition.Phase? = competition.phaseAt(now())

    fun partOf(
        code: String,
        playerId: Int,
    ): IsPlayerPartOfCompetitionOut {
        val entry = current()?.takeIf { it.code == code }?.let { store.entryOf(it.id, playerId) }
        return IsPlayerPartOfCompetitionOut(entry != null, entry?.roomId ?: 0)
    }

    fun isEntered(
        competition: Competition,
        roomId: Int,
    ): Boolean = store.isEntered(competition.id, roomId)

    /** Any room of the competition, each as likely; the player's own is one of them. */
    fun roomToJudge(
        competition: Competition,
        playerId: Int,
    ): Int? {
        val own = listOfNotNull(store.entryOf(competition.id, playerId)?.roomId)
        return (store.othersRooms(competition.id, playerId).map { it.roomId } + own).randomOrNull(random)
    }

    /** What the owner sees on coming into [roomId]: that it is entered, what keeps it out, or the rules. */
    fun firstLook(
        competition: Competition,
        roomId: Int,
        ownerId: Int,
    ): CompetitionEntrySubmitResultOut? =
        answer(competition, roomId, ownerId) { EntryAnswer.Rules }?.toWire(competition)

    /**
     * The owner's way into the competition with [roomId], one answer per [level] the window sends; null shows nothing.
     * A player who has an entry is told so for the room he entered and no other.
     */
    fun entryStep(
        competition: Competition,
        roomId: Int,
        ownerId: Int,
        level: Int,
    ): CompetitionEntrySubmitResultOut? =
        answer(competition, roomId, ownerId) { stepAnswer(competition, roomId, ownerId, level) }?.toWire(competition)

    private fun answer(
        competition: Competition,
        roomId: Int,
        ownerId: Int,
        whenFree: () -> EntryAnswer?,
    ): EntryAnswer? {
        val entry = store.entryOf(competition.id, ownerId)
        return when {
            entry != null -> EntryAnswer.Entered.takeIf { entry.roomId == roomId }
            store.isEntered(competition.id, roomId) -> null
            else -> obstacle(competition, roomId) ?: whenFree()
        }
    }

    private fun stepAnswer(
        competition: Competition,
        roomId: Int,
        ownerId: Int,
        level: Int,
    ): EntryAnswer? =
        when (level) {
            LEVEL_RECHECK, LEVEL_RULES_READ -> EntryAnswer.Submit
            LEVEL_SUBMIT -> EntryAnswer.Confirm
            LEVEL_CONFIRMED -> EntryAnswer.Entered.takeIf { store.enter(competition.id, ownerId, roomId, now()) }
            else -> null
        }

    /** What keeps the room out, the one its owner cannot change first. */
    private fun obstacle(
        competition: Competition,
        roomId: Int,
    ): EntryAnswer? {
        val room = store.roomFacts(roomId)
        if (room == null || !competition.takesRoomMadeAt(room.createdAt)) return EntryAnswer.TooOld
        if (!room.doorOpen) return EntryAnswer.DoorShut
        val missing = competition.requiredFurni.minus(store.furniCodesIn(roomId))
        return if (missing.isEmpty()) null else EntryAnswer.Missing(missing)
    }

    fun votingInfo(
        competition: Competition,
        playerId: Int,
        mayVote: Boolean,
    ): CompetitionVotingInfoOut {
        val left = (competition.votesPerDay - store.votesCastOn(competition.id, playerId, today())).coerceAtLeast(0)
        val result = if (mayVote) VOTE_OPEN else VOTE_NEEDS_PERK
        return CompetitionVotingInfoOut(competition.id, competition.code, result, left)
    }

    fun vote(
        competition: Competition,
        roomId: Int,
        playerId: Int,
    ) {
        if (!store.castVote(competition, roomId, playerId, today())) return
        progress(playerId, GameEvents.ROOM_COMPETITION_VOTE, 1)
    }

    private fun today(): Int = now() / DAY_SECONDS

    companion object {
        /** The window asks again after a furni change and stays on its submit step. */
        private const val LEVEL_RECHECK = 0
        private const val LEVEL_RULES_READ = 1
        private const val LEVEL_SUBMIT = 2
        private const val LEVEL_CONFIRMED = 3

        private const val VOTE_OPEN = 0
        private const val VOTE_NEEDS_PERK = 1
        private const val DAY_SECONDS = 86_400
    }
}
