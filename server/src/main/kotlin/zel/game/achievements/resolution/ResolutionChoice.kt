package zel.game.achievements.resolution

data class ResolutionChoice(
    val achievementId: Int,
    val level: Int,
    val badgeCode: String,
    val requiredLevel: Int,
    val state: State,
) {
    @Suppress("MagicNumber") // the state numbers the client reads
    enum class State(
        val code: Int,
    ) {
        OPEN(0),
        FINISHED(1),
        TAKEN(2),
    }
}
