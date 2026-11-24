package zel.game.items

data class YoutubePlaylist(
    val id: String,
    val name: String,
    val description: String,
    val clips: List<Clip>,
) {
    data class Clip(
        val id: String,
        val seconds: Int,
    )
}
