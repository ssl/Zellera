package zel.game.moderation

import zel.protocol.generated.CfhSanctionOut

data class SanctionProposal(
    val name: String,
    val hours: Int,
    val probationDays: Int,
    val avatarOnly: Boolean,
) {
    fun message(
        issueId: Int,
        accountId: Int,
    ) = CfhSanctionOut(issueId, accountId, name, hours, probationDays, avatarOnly, "", "")
}
