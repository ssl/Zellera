package zel.game.moderation

enum class SanctionKind {
    ALERT,
    MUTE,
    BAN,
    ;

    companion object {
        fun ofStored(value: String?): SanctionKind? = entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}
