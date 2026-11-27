package zel.game.polls

import zel.db.Sql
import zel.db.intOrNone
import java.sql.ResultSet
import javax.sql.DataSource

internal class PollStore(
    dataSource: DataSource,
) {
    private val sql = Sql(dataSource)

    fun polls(): Map<Int, Poll> {
        val polls = ArrayList<Poll>()
        sql.select("SELECT $POLL_COLUMNS FROM polls").each { polls.add(poll(it)) }
        val choices = choices(ALL_CHOICES)
        val rows = ArrayList<Pair<Int, Question.Stored>>()
        sql.select(ALL_QUESTIONS).each { rows.add(it.getInt("poll_id") to question(it, choices)) }
        val byPoll = rows.groupBy({ it.first }, { it.second })
        return polls.associate { it.id to it.with(byPoll[it.id].orEmpty()) }
    }

    fun poll(pollId: Int): Poll? {
        val poll =
            sql
                .select("SELECT $POLL_COLUMNS FROM polls WHERE id = ? LIMIT 1", pollId)
                .first(::poll) ?: return null
        val choices = choices(POLL_CHOICES, pollId)
        val rows = ArrayList<Question.Stored>()
        sql.select(POLL_QUESTIONS, pollId).each { rows.add(question(it, choices)) }
        return poll.with(rows)
    }

    fun hasDeclined(
        playerId: Int,
        pollId: Int,
    ): Boolean =
        exists(
            "SELECT 1 FROM poll_answers WHERE poll_id = ? AND player_id = ? AND question_id = 0 LIMIT 1",
            pollId,
            playerId,
        )

    fun answeredQuestions(
        playerId: Int,
        pollId: Int,
    ): Set<Int> {
        val answered = HashSet<Int>()
        sql
            .select("SELECT question_id FROM poll_answers WHERE poll_id = ? AND player_id = ?", pollId, playerId)
            .each { answered.add(it.getInt(1)) }
        return answered
    }

    fun openPolls(playerId: Int): List<Int> = sql.select(OPEN_POLLS, playerId).rows { it.getInt(1) }

    private fun exists(
        query: String,
        vararg params: Any?,
    ): Boolean = sql.select(query, *params).first { true } != null

    private fun poll(row: ResultSet) =
        Poll(
            row.getInt("id"),
            row.getString("title"),
            row.getString("thanks_message"),
            row.getString("reward_badge"),
            row.getString("headline"),
            row.getString("summary"),
            row.getString("kind"),
        )

    private fun question(
        row: ResultSet,
        choices: Map<Int, List<PollChoice>>,
    ): Question.Stored {
        val id = row.getInt("id")
        val kind = row.getInt("kind")
        val question =
            Question(
                id = id,
                position = row.getInt("position"),
                kind = kind,
                text = row.getString("question").orEmpty(),
                category = row.getInt("category"),
                choices = if (kind == SINGLE_CHOICE || kind == MULTIPLE_CHOICE) choices[id].orEmpty() else emptyList(),
            )
        return Question.Stored(row.intOrNone("parent_id"), question)
    }

    private fun choices(
        query: String,
        vararg params: Any?,
    ): Map<Int, List<PollChoice>> {
        val choices = HashMap<Int, MutableList<PollChoice>>()
        sql.select(query, *params).each { row ->
            choices
                .getOrPut(row.getInt("question_id")) { ArrayList() }
                .add(PollChoice(row.getString("value"), row.getString("text"), row.getInt("category")))
        }
        return choices
    }

    private companion object {
        const val SINGLE_CHOICE = 1
        const val MULTIPLE_CHOICE = 2
        const val POLL_COLUMNS = "id, kind, title, headline, summary, thanks_message, reward_badge"
        const val QUESTION_COLUMNS = "id, poll_id, parent_id, position, category, question, kind"
        const val ALL_QUESTIONS = "SELECT $QUESTION_COLUMNS FROM poll_questions ORDER BY parent_id, position ASC"
        const val POLL_QUESTIONS =
            "SELECT $QUESTION_COLUMNS FROM poll_questions WHERE poll_id = ? ORDER BY parent_id, position ASC"
        const val ALL_CHOICES =
            "SELECT question_id, value, text, category FROM poll_question_options ORDER BY question_id, position"
        const val POLL_CHOICES =
            "SELECT o.question_id, o.value, o.text, o.category FROM poll_question_options o " +
                "JOIN poll_questions q ON q.id = o.question_id WHERE q.poll_id = ? ORDER BY o.question_id, o.position"
        const val OPEN_POLLS =
            "SELECT p.id FROM polls p WHERE NOT EXISTS " +
                "(SELECT 1 FROM poll_answers a WHERE a.poll_id = p.id AND a.player_id = ? AND a.question_id = 0) " +
                "ORDER BY p.id"
    }
}
