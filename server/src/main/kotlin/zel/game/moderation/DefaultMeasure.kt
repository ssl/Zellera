package zel.game.moderation

/**
 * What the default sanction for a report would do: [kind] for [hours] (0 for an alert). [step] is the ladder step
 * it counts as, null when the ladder is off.
 */
data class DefaultMeasure(
    val kind: SanctionKind,
    val hours: Int,
    val step: LadderStep? = null,
) {
    val probationDays: Int get() = step?.probationDays ?: 0

    fun proposal(): SanctionProposal = SanctionProposal(kind.name, hours, probationDays, false)
}
