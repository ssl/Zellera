package zel.game.webcommands

/**
 * One row of `web_commands`; [createdBy] is the player who sent it: a staff member or the account itself, null when
 * the site's own pages wrote the row for the account.
 */
data class WebCommand(
    val id: Long,
    val userId: Int,
    val type: String?,
    val payload: String?,
    val createdBy: Int?,
)
