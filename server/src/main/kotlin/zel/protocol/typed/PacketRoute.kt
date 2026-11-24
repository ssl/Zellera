package zel.protocol.typed

import zel.ext.ExtensionBus
import zel.protocol.generated.AllowedField
import zel.protocol.generated.AllowedValueTable
import zel.protocol.generated.IncomingCodec
import zel.protocol.generated.PacketReader

class PacketRoute<T>(
    val codec: IncomingCodec<T>,
    val requiresPlayer: Boolean = true,
    val handler: PacketHandler<T>,
) {
    private val allowedRows: List<AllowedField<T>> = AllowedValueTable.of(codec.name)

    fun dispatch(
        session: PacketSession,
        body: ByteArray,
        allowed: AllowedValues = AllowedValues(),
    ): Int {
        val reader = PacketReader(body)
        val read = codec.read(reader)
        val verdict = allowed.check(session.client, allowedRows, read)
        val message =
            if (verdict.runHandler) {
                ExtensionBus.incoming(
                    session.client.player
                        ?.profile
                        ?.id,
                    codec,
                    read,
                )
            } else {
                null
            }
        if (message != null) handler.handle(session, message)
        verdict.after?.invoke()
        return reader.remaining
    }
}
