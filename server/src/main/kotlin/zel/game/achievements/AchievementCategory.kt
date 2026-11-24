package zel.game.achievements

enum class AchievementCategory(
    val code: String,
    /** An empty name keeps the achievement out of every tab. */
    val shownAs: String = code,
) {
    IDENTITY("identity"),
    EXPLORE("explore"),
    MUSIC("music"),
    SOCIAL("social"),
    GAMES("games"),
    ROOM_BUILDER("room_builder"),
    PETS("pets"),
    TOOLS("tools"),
    EVENTS("events"),
    OTHER("other"),
    TEST("test"),
    INVISIBLE("invisible", shownAs = ""),
    MISC("misc"),
    ARCHIVE("archive"),
    WIRED_GAMES("wired_games"),
    CRACKABLES("crackables"),
    CRAFTING("crafting"),
    ;

    companion object {
        private val byCode = entries.associateBy(AchievementCategory::code)

        fun ofCode(code: String?): AchievementCategory? = byCode[code]
    }
}
