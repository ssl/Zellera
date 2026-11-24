@file:Suppress("MagicNumber") // group type wire codes

package zel.game.guilds

enum class JoinMode(
    val code: Int,
) {
    WALK_IN(0),

    ON_REQUEST(1),

    INVITE_ONLY(2),

    INVITE_ONLY_LARGE(3),

    WALK_IN_LARGE(4),
    ;

    val needsApproval: Boolean get() = this == ON_REQUEST

    val isInviteOnly: Boolean get() = this == INVITE_ONLY || this == INVITE_ONLY_LARGE

    companion object {
        fun fromCode(code: Int): JoinMode? = entries.firstOrNull { it.code == code }

        /** A stored value; a stray one lets nobody in by accident. */
        fun fromStored(code: Int): JoinMode = fromCode(code) ?: INVITE_ONLY
    }
}
