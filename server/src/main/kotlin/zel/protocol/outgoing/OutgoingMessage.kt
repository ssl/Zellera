package zel.protocol.outgoing

import zel.protocol.Outbound
import zel.protocol.generated.OutgoingCodec

/** Builds a server message from game state; [message] answers null when there is nothing to send. */
abstract class OutgoingMessage<T>(
    private val codec: OutgoingCodec<T>,
) {
    private var composed: Outbound<T>? = null

    protected abstract fun message(): T?

    /** The message is built once, here. */
    fun compose(): Outbound<T>? =
        composed ?: message()
            ?.let { Outbound(codec, it) }
            ?.also { composed = it }
}
