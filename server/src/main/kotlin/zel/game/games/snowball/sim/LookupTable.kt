package zel.game.games.snowball.sim

internal class LookupTable(
    text: String,
) {
    private val values =
        text
            .trim()
            .split(Regex("\\s+"))
            .map(String::toInt)
            .toIntArray()

    operator fun get(index: Int): Int = values[index]

    fun toIntArray(): IntArray = values.clone()
}
