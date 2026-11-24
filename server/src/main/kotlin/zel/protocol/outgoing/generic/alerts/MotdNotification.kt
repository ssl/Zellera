package zel.protocol.outgoing.generic.alerts

import zel.protocol.generated.MOTDNotificationOut
import zel.protocol.generated.MOTDNotificationOutCodec
import zel.protocol.outgoing.OutgoingMessage

class MotdNotification(
    private val messages: List<String>,
) : OutgoingMessage<MOTDNotificationOut>(MOTDNotificationOutCodec) {
    constructor(messages: Array<String>) : this(messages.toList())

    override fun message() = MOTDNotificationOut(messages)
}
