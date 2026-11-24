package zel.game.moderation

/** A ladder step a player was given; times are unix seconds, 0 for none ([endsAt] of an alert, a probation ended). */
data class SanctionRecord(
    val id: Int,
    val playerId: Int,
    val step: Int,
    val kind: SanctionKind,
    val reason: String,
    val issuedAt: Int,
    val endsAt: Int,
    val probationEndsAt: Int,
) {
    fun onProbation(now: Int): Boolean = probationEndsAt > now
}
