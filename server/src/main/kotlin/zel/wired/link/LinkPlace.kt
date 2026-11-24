package zel.wired.link

data class LinkPlace(
    val playerId: Int,
    val roomId: Int,
    val furniId: Int,
    val isLink: Boolean,
    val owned: Boolean,
    val roomLinks: Set<Int>,
) {
    fun linkedHere(network: LinkNetwork): Boolean = network.members.any { it in roomLinks }

    fun others(network: LinkNetwork): List<Int> = network.members.filter { it in roomLinks && it != furniId }
}
