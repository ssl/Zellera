package zel.wired.link

interface LinkStore {
    /** Oldest first. */
    fun networks(ownerId: Int): List<LinkNetwork>

    fun network(
        ownerId: Int,
        networkId: Int,
    ): LinkNetwork?

    fun count(ownerId: Int): Int

    /** The new id, or null when the owner has a network of that name. */
    fun create(
        ownerId: Int,
        name: String,
    ): Int?

    fun rename(
        ownerId: Int,
        networkId: Int,
        name: String,
    ): Boolean

    fun delete(
        ownerId: Int,
        networkId: Int,
    )

    fun connections(networkId: Int): List<LinkConnection>

    fun setup(linkId: Int): LinkSetup

    fun save(
        linkId: Int,
        setup: LinkSetup,
    )

    fun disconnect(linkId: Int)
}
