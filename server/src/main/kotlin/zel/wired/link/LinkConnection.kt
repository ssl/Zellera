package zel.wired.link

data class LinkConnection(
    val roomId: Int,
    val roomName: String,
    val linkId: Int,
    val hearsOwnRoom: Boolean,
)
