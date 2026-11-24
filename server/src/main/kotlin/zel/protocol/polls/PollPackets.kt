package zel.protocol.polls

import zel.db.SqlFailure
import zel.game.players.Player
import zel.game.polls.Poll
import zel.game.polls.PollOffers
import zel.game.polls.Polls
import zel.protocol.generated.PollAnswerInCodec
import zel.protocol.generated.PollContentsOutCodec
import zel.protocol.generated.PollRejectInCodec
import zel.protocol.generated.PollStartInCodec
import zel.protocol.outgoing.handshake.ErrorReport
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.port.RoomPort
import zel.server.GameServices
import org.slf4j.LoggerFactory

/** Only opening is answered; poll id 0 likes or dislikes the staff question. */
class PollPackets(
    private val services: GameServices,
    private val polls: Polls,
    private val offers: PollOffers,
    rooms: () -> RoomPort,
) : PacketPackage {
    override val name = "polls"

    override val routes =
        listOf(
            PacketRoute(PollStartInCodec) { session, m ->
                val (player, poll) = offeredPoll(session, m.pollId) ?: return@PacketRoute
                open(session, player, poll)
            },
            PacketRoute(PollAnswerInCodec) { session, m ->
                if (m.pollId == ROOM_QUESTION) {
                    val player = session.client.player ?: return@PacketRoute
                    rooms().answerRoomQuestion(player.profile.id, m.questionId, m.answers.firstOrNull().orEmpty())
                    return@PacketRoute
                }
                val (player, poll) = offeredPoll(session, m.pollId) ?: return@PacketRoute
                answer(player, poll, m.questionId, m.answers)
            },
            PacketRoute(PollRejectInCodec) { session, m ->
                val (player, poll) = offeredPoll(session, m.pollId) ?: return@PacketRoute
                record(player, poll, DECLINED_QUESTION, "")
            },
        )

    private fun offeredPoll(
        session: PacketSession,
        pollId: Int,
    ): Pair<Player, Poll>? {
        val player = session.client.player ?: return null
        if (!offers.wasOffered(player, pollId)) return null
        val poll = polls.getPoll(pollId) ?: return null
        return player to poll
    }

    private fun open(
        session: PacketSession,
        player: Player,
        poll: Poll,
    ) {
        when {
            polls.declinedPoll(player, poll.id) -> session.client.send(pollNotAvailable())
            polls.donePoll(player, poll.id) -> Unit
            else -> session.send(PollContentsOutCodec, pollContents(poll))
        }
    }

    /** Not after the poll is turned down; any answer that completes the poll awards the badge (once). */
    private fun answer(
        player: Player,
        poll: Poll,
        questionId: Int,
        answers: List<String>,
    ) {
        if (!poll.asks(questionId)) return
        services.threading.run {
            if (polls.declinedPoll(player, poll.id)) return@run
            store(player, poll, questionId, answers.joinToString(":").take(ANSWER_LENGTH))
            if (poll.badgeReward.isNotEmpty() && polls.completedPoll(player, poll)) {
                player.inventory.badges.award(poll.badgeReward)
            }
        }
    }

    private fun record(
        player: Player,
        poll: Poll,
        questionId: Int,
        answer: String,
    ) {
        services.threading.run { store(player, poll, questionId, answer) }
    }

    private fun store(
        player: Player,
        poll: Poll,
        questionId: Int,
        answer: String,
    ) {
        try {
            services.database.queries.write(STORE_ANSWER, poll.id, player.profile.id, questionId, answer)
        } catch (e: SqlFailure) {
            log.error("Poll {} answer of player {} not stored", poll.id, player.profile.id, e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(PollPackets::class.java)
        private const val DECLINED_QUESTION = 0
        private const val ROOM_QUESTION = 0
        private const val ANSWER_LENGTH = 255
        private const val POLL_START_REQUEST = 234
        private const val POLL_NOT_AVAILABLE = 4004
        private const val STORE_ANSWER =
            "INSERT IGNORE INTO poll_answers (poll_id, player_id, question_id, answer) VALUES (?, ?, ?, ?)"

        fun pollNotAvailable(): ErrorReport = ErrorReport(POLL_START_REQUEST, POLL_NOT_AVAILABLE)
    }
}
