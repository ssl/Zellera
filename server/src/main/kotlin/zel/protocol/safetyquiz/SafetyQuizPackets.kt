package zel.protocol.safetyquiz

import zel.game.achievements.AchievementProgress
import zel.game.achievements.GameEvents
import zel.game.achievements.count
import zel.game.safetyquiz.RulesQuiz
import zel.protocol.generated.GetQuizQuestionsInCodec
import zel.protocol.generated.PostQuizAnswersInCodec
import zel.protocol.generated.QuizDataOut
import zel.protocol.generated.QuizDataOutCodec
import zel.protocol.generated.QuizResultsOut
import zel.protocol.generated.QuizResultsOutCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute

class SafetyQuizPackets(
    quiz: RulesQuiz,
    achievements: () -> AchievementProgress,
) : PacketPackage {
    override val name = "safetyquiz"
    override val routes =
        listOf(
            PacketRoute(GetQuizQuestionsInCodec) { session, m ->
                val player = session.client.player ?: return@PacketRoute
                if (m.quizCode != RulesQuiz.CODE) return@PacketRoute
                session.send(QuizDataOutCodec, QuizDataOut(m.quizCode, quiz.ask(player.profile.id)))
            },
            PacketRoute(PostQuizAnswersInCodec) { session, m ->
                val player = session.client.player ?: return@PacketRoute
                if (m.quizCode != RulesQuiz.CODE || m.answers.size > MAX_ANSWERS) return@PacketRoute
                val wrong = quiz.answer(player.profile.id, m.answers) ?: return@PacketRoute
                session.send(QuizResultsOutCodec, QuizResultsOut(m.quizCode, wrong))
                if (wrong.isEmpty()) {
                    achievements().count(player, GameEvents.SAFETY_QUIZ_PASSED)
                }
            },
        )

    companion object {
        private const val MAX_ANSWERS = 64
    }
}
