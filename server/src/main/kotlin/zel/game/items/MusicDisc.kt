package zel.game.items

/** A music disc's state is its song's id and nothing else. */
object MusicDisc {
    fun state(songId: Int): String = songId.toString()

    fun songId(state: String): Int = state.trim().toIntOrNull() ?: 0
}
