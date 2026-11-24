@file:Suppress("MagicNumber") // member list wire codes

package zel.game.guilds

enum class MemberLevel(
    val code: Int,
    val standing: GuildStanding,
    val isAdmin: Boolean = false,
) {
    FOUNDER(0, GuildStanding.MEMBER, isAdmin = true),
    OFFICER(1, GuildStanding.MEMBER, isAdmin = true),
    REGULAR(2, GuildStanding.MEMBER),
    APPLICANT(3, GuildStanding.PENDING),
    BARRED(4, GuildStanding.NONE),
    ;

    /** True for everyone who is in the guild, applicants and barred players excluded. */
    val isMember: Boolean get() = standing == GuildStanding.MEMBER

    companion object {
        /** A stray level grants nothing. */
        fun fromCode(code: Int): MemberLevel = entries.firstOrNull { it.code == code } ?: BARRED
    }
}
