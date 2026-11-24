package zel.wired.signal

fun interface LinkNetworks {
    fun members(linkId: Int): List<LinkMember>

    companion object {
        val None: LinkNetworks = LinkNetworks { emptyList() }
    }
}
