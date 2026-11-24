package zel.game.games.snowball.sim

class GameObjectData(
    vars: IntArray,
    strings: List<String>,
) {
    private val values = vars.clone()

    val strings: List<String> = strings.toList()

    val type: Int get() = values[0]

    val id: Int get() = values[1]

    val vars: IntArray get() = values.clone()

    fun variable(index: Int): Int = values[index]

    override fun equals(other: Any?): Boolean =
        other is GameObjectData && values.contentEquals(other.values) && strings == other.strings

    override fun hashCode(): Int = 31 * values.contentHashCode() + strings.hashCode()

    override fun toString(): String = values.contentToString() + if (strings.isEmpty()) "" else strings.toString()
}
