package zel.protocol.outgoing.catalog

import zel.protocol.generated.OutgoingCodec
import zel.protocol.generated.PacketReader
import zel.protocol.generated.PacketWriter

internal fun bytesOf(build: (PacketWriter) -> Unit): ByteArray = PacketWriter().also(build).toByteArray()

/** The layout must use every byte. */
internal fun <T> OutgoingCodec<T>.decodeBody(body: ByteArray): T {
    val reader = PacketReader(body)
    val message = read(reader)
    check(reader.remaining == 0) { "$name: ${reader.remaining} bytes after the layout" }
    return message
}
