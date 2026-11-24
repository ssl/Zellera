package zel.game.bots

data class BotLook(
    val name: String,
    val motto: String = "",
    val figure: String = "",
    val gender: String = "M",
) {
    /** This look with the values a catalog preset (`name:...;motto:...;figure:...;gender:...`) names put over it. */
    fun withPreset(preset: String): BotLook {
        val values =
            preset
                .split(';')
                .mapNotNull { pair -> pair.split(':', limit = 2).takeIf { it.size == 2 } }
                .associate { (key, value) -> key.trim() to value.trim() }
        return BotLook(
            name = values["name"] ?: name,
            motto = values["motto"] ?: motto,
            figure = values["figure"] ?: figure,
            gender = values["gender"] ?: gender,
        )
    }
}
