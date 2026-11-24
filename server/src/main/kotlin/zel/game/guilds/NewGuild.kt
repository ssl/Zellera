package zel.game.guilds

data class NewGuild(
    val name: String,
    val about: String,
    val home: Home,
    val crest: Crest,
) {
    data class Home(
        val id: Int,
        val title: String,
    )

    data class Crest(
        val code: String,
        val primary: Int,
        val secondary: Int,
    )
}
