package zel.game.items

enum class ProductKind(
    val code: String,
) {
    FLOOR_FURNI("S"),
    WALL_FURNI("I"),
    AVATAR_EFFECT("E"),
    BADGE("B"),
    BOT("R"),
    CLUB_DAYS("H"),
    PET("P"),
    ;

    companion object {
        private val BY_CODE = entries.associateBy { it.code }

        /** A blank or unknown code reads as floor furni. */
        fun fromCode(code: String?): ProductKind = BY_CODE[code?.trim()?.uppercase()] ?: FLOOR_FURNI
    }
}
