package zel.game.polls

data class Question(
    val id: Int,
    val position: Int,
    val kind: Int,
    val text: String,
    val category: Int,
    val choices: List<PollChoice>,
    val followUps: List<Question> = emptyList(),
) {
    data class Stored(
        val parentId: Int,
        val question: Question,
    )
}
