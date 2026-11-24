package zel.game.moderation

/** One rung of the sanction ladder: what it does, for how many [hours], and how long it is remembered. */
data class LadderStep(
    val step: Int,
    val kind: SanctionKind,
    val hours: Int,
    val probationDays: Int,
)
