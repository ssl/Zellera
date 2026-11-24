package zel.protocol.friends

/** The least milliseconds between two accepted friend list messages of one kind from one connection. */
internal object FriendPacing {
    /** Messages that change a friend list or a request; each is a click. */
    const val LIST_CHANGE = 300L

    /** Messages a click answers from the database. */
    const val LOOKUP = 300L

    const val SEARCH = 1000L

    const val LIST_REFRESH = 5000L

    /** The client lets a player invite once a minute; the rest is room for a late first message. */
    const val INVITE = 55_000L
}
