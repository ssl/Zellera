package zel.wired.link

sealed interface LinkReply {
    data class Configuration(
        val setup: LinkSetup,
        val rows: List<LinkRow>,
    ) : LinkReply

    data class Done(
        val operation: Int,
        val setup: LinkSetup,
        val removed: Int?,
        val rows: List<LinkRow>,
    ) : LinkReply

    data class Refused(
        val operation: Int,
        val result: Int,
    ) : LinkReply

    data class Connections(
        val networkId: Int,
        val rows: List<LinkConnection>,
    ) : LinkReply

    data class Invalidated(
        val linkId: Int,
        val networkId: Int,
    ) : LinkReply
}
