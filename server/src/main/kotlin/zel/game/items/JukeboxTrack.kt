package zel.game.items

data class JukeboxTrack(
    val id: Int,
    val code: String?,
    val name: String?,
    val author: String?,
    val data: String?,
    val length: Int,
)
