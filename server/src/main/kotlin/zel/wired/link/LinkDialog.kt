package zel.wired.link

/** Each operation asks about the Link itself first (the room knows it), then what was typed, then the store. */
class LinkDialog(
    private val store: LinkStore,
    private val networksChanged: () -> Unit,
    private val maxNetworks: () -> Int = { DEFAULT_MAX_NETWORKS },
) {
    /** In a room, a furni that is no Link answers "no such Link here"; in no room nothing answers. */
    fun open(place: LinkPlace): List<LinkReply> =
        when {
            !place.isLink -> if (place.roomId != 0) listOf(LinkReply.Refused(OPEN, NOT_IN_ROOM)) else emptyList()
            !place.owned -> listOf(LinkReply.Refused(OPEN, NOT_OWNER))
            else -> listOf(configuration(place))
        }

    private fun configuration(place: LinkPlace) =
        LinkReply.Configuration(store.setup(place.furniId), rows(place, store.networks(place.playerId)))

    fun create(
        place: LinkPlace,
        name: String,
    ): List<LinkReply> =
        guarded(place, CREATE) {
            if (!valid(name)) return@guarded refused(CREATE, BAD_NAME)
            val full = store.count(place.playerId) >= maxNetworks()
            // a name the player already has is told as taken, at the limit too
            if (full && store.networks(place.playerId).none { it.name == name }) return@guarded refused(CREATE, LIMIT)
            val id = store.create(place.playerId, name) ?: return@guarded refused(CREATE, NAME_TAKEN)
            done(place, CREATE, changed = listOf(id))
        }

    fun rename(
        place: LinkPlace,
        networkText: String,
        name: String,
    ): List<LinkReply> =
        guarded(place, RENAME) {
            val id = parse(networkText) ?: return@guarded refused(RENAME, BAD_ID)
            if (!valid(name)) return@guarded refused(RENAME, BAD_NAME)
            if (owned(place, id) == null) return@guarded refused(RENAME, NO_NETWORK)
            if (!store.rename(place.playerId, id, name)) return@guarded refused(RENAME, NAME_TAKEN)
            done(place, RENAME, changed = listOf(id))
        }

    fun delete(
        place: LinkPlace,
        networkText: String,
    ): List<LinkReply> =
        guarded(place, DELETE) {
            val id = parse(networkText) ?: return@guarded refused(DELETE, BAD_ID)
            val network = owned(place, id) ?: return@guarded refused(DELETE, NO_NETWORK)
            store.delete(place.playerId, id)
            networksChanged()
            val result = LinkReply.Done(DELETE, store.setup(place.furniId), id, emptyList())
            listOf(result) + place.others(network).map { LinkReply.Invalidated(it, id) }
        }

    fun connections(
        place: LinkPlace,
        networkText: String,
    ): List<LinkReply> =
        guarded(place, CONNECTIONS) {
            val id = parse(networkText) ?: return@guarded refused(CONNECTIONS, BAD_ID)
            if (owned(place, id) == null) return@guarded refused(CONNECTIONS, NO_NETWORK)
            listOf(LinkReply.Connections(id, store.connections(id)))
        }

    fun save(
        place: LinkPlace,
        networkText: String,
        hearsOwnRoom: Boolean,
    ): List<LinkReply> =
        guarded(place, SAVE) {
            val id =
                if (networkText == NONE) {
                    null
                } else {
                    parse(networkText) ?: return@guarded refused(SAVE, BAD_ID)
                }
            if (id != null) {
                val network = owned(place, id) ?: return@guarded refused(SAVE, NO_NETWORK)
                if (place.others(network).isNotEmpty()) return@guarded refused(SAVE, TAKEN_IN_ROOM)
            }
            val before = store.setup(place.furniId).networkId
            store.save(place.furniId, LinkSetup(id, hearsOwnRoom))
            networksChanged()
            done(place, SAVE, changed = listOfNotNull(id, before).distinct())
        }

    fun pickedUp(linkId: Int) {
        store.disconnect(linkId)
        networksChanged()
    }

    private fun guarded(
        place: LinkPlace,
        operation: Int,
        block: () -> List<LinkReply>,
    ): List<LinkReply> =
        when {
            !place.isLink -> refused(operation, NOT_IN_ROOM)
            !place.owned -> refused(operation, NOT_OWNER)
            else -> block()
        }

    private fun refused(
        operation: Int,
        result: Int,
    ): List<LinkReply> = listOf(LinkReply.Refused(operation, result))

    private fun done(
        place: LinkPlace,
        operation: Int,
        changed: List<Int>,
    ): List<LinkReply> {
        val rows = changed.mapNotNull { id -> owned(place, id) }
        return listOf(LinkReply.Done(operation, store.setup(place.furniId), null, rows(place, rows)))
    }

    private fun owned(
        place: LinkPlace,
        networkId: Int,
    ): LinkNetwork? = store.network(place.playerId, networkId)

    private fun rows(
        place: LinkPlace,
        networks: List<LinkNetwork>,
    ): List<LinkRow> = networks.map { LinkRow(it.id, it.name, it.members.size, place.linkedHere(it)) }

    private fun valid(name: String): Boolean = name.length <= MAX_NAME && name.any { !it.isWhitespace() }

    /** An id counts only as the server writes it: a positive number in its plain decimal form. */
    private fun parse(text: String): Int? = text.toIntOrNull()?.takeIf { it > 0 && it.toString() == text }

    companion object {
        const val OPEN = 0
        const val SAVE = 1
        const val CREATE = 2
        const val RENAME = 3
        const val DELETE = 4
        const val CONNECTIONS = 5

        const val DEFAULT_MAX_NETWORKS = 10_000
        const val LIMIT = 8
        const val NOT_OWNER = 1
        const val NO_NETWORK = 2
        const val BAD_NAME = 3
        const val NAME_TAKEN = 4
        const val TAKEN_IN_ROOM = 5
        const val BAD_ID = 6
        const val NOT_IN_ROOM = 7

        const val NONE = "0"
        private const val MAX_NAME = 255
    }
}
