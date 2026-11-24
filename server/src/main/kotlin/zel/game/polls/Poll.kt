package zel.game.polls

class Poll(
    val id: Int,
    title: String?,
    thanksMessage: String?,
    badgeReward: String?,
    headline: String?,
    summary: String?,
    type: String? = "",
    val questions: List<Question> = emptyList(),
) {
    val title: String = title.orEmpty()

    val thanksMessage: String = thanksMessage.orEmpty()
    val badgeReward: String = badgeReward.orEmpty()

    val headline: String = headline.orEmpty()

    val summary: String = summary.orEmpty()

    /** Ignored by the client. */
    val type: String = type.orEmpty()

    fun offerHeadline(): String = headline.ifEmpty { title }

    fun asks(questionId: Int): Boolean =
        questions.any { top -> top.id == questionId || top.followUps.any { it.id == questionId } }

    /** True once every top question is in [answered]; follow-ups only show for some answers. */
    fun isComplete(answered: Set<Int>): Boolean = questions.isNotEmpty() && questions.all { it.id in answered }

    /** [rows] come top questions first, by position; a follow-up goes under its parent, an orphan nowhere. */
    fun with(rows: List<Question.Stored>): Poll {
        val followUps = rows.filter { it.parentId > 0 }.groupBy({ it.parentId }, { it.question })
        val tops =
            rows
                .filter { it.parentId <= 0 }
                .map { (_, top) -> top.copy(followUps = followUps[top.id].orEmpty().sortedBy(Question::position)) }
                .sortedBy(Question::position)
        return Poll(id, title, thanksMessage, badgeReward, headline, summary, type, tops)
    }
}
