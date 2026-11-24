package zel.game.safetyquiz

import java.util.Random
import java.util.concurrent.ConcurrentHashMap

/** Five of ten questions per attempt, answered by chosen text index in the order asked; retries have no lockout. */
class RulesQuiz(
    private val random: Random = Random(),
) {
    private val openAttempts = ConcurrentHashMap<Int, List<Int>>()

    fun ask(playerId: Int): List<Int> =
        ANSWER_KEY.indices
            .shuffled(random)
            .take(QUESTIONS_PER_ATTEMPT)
            .also { openAttempts[playerId] = it }

    fun answer(
        playerId: Int,
        answers: List<Int>,
    ): List<Int>? = openAttempts.remove(playerId)?.let { wrongAnswers(it, answers) }

    companion object {
        const val CODE = "RulesQuiz1"
        const val QUESTIONS_PER_ATTEMPT = 5

        private val ANSWER_KEY = listOf(2, 1, 2, 0, 1, 3, 1, 3, 0, 0)

        fun wrongAnswers(
            questions: List<Int>,
            answers: List<Int>,
        ): List<Int> =
            questions.filterIndexed { place, question ->
                val right = ANSWER_KEY.getOrNull(question)
                right == null || right != answers.getOrNull(place)
            }
    }
}
