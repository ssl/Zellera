package zel.protocol.friends

object InstantMessageRefusal {
    const val RECEIVER_MUTED = 3
    const val SENDER_MUTED = 4
    const val NOT_A_FRIEND = 6

    const val RECIPIENT_UNAVAILABLE = 14

    /** Request id of the error report for a message to someone neither a friend nor online. */
    const val SEND_REQUEST = 33

    const val UNDELIVERED = 10
}
