package zel.wired.link

data class LinkSetup(
    val networkId: Int?,
    val hearsOwnRoom: Boolean,
) {
    companion object {
        val NONE = LinkSetup(null, false)
    }
}
