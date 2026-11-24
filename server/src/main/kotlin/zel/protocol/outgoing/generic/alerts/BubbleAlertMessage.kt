package zel.protocol.outgoing.generic.alerts

import zel.protocol.generated.NotificationDialogOut
import zel.protocol.generated.NotificationDialogOutCodec
import zel.protocol.outgoing.OutgoingMessage

class BubbleAlertMessage(
    private val errorKey: String,
    private val keys: Map<String, String>,
) : OutgoingMessage<NotificationDialogOut>(NotificationDialogOutCodec) {
    constructor(errorKey: String, message: String) : this(errorKey, mapOf("message" to message))

    constructor(errorKey: String) : this(errorKey, emptyMap())

    override fun message() =
        NotificationDialogOut(
            errorKey,
            keys.map { (key, value) -> NotificationDialogOut.List1Item(key, value) },
        )
}
