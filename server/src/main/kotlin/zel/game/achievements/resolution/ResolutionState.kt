package zel.game.achievements.resolution

/** [badge] is the badge of the promised level as the achievement names it today. */
sealed interface ResolutionState {
    data class Picking(
        val choices: List<ResolutionChoice>,
        val durationSeconds: Int,
    ) : ResolutionState

    data class Running(
        val resolution: Resolution,
        val badge: String,
        val progress: ResolutionProgress,
        val secondsLeft: Int,
    ) : ResolutionState

    data class Completed(
        val resolution: Resolution,
        val badge: String,
    ) : ResolutionState
}
