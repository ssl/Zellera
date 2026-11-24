package zel.protocol.handshake

import zel.protocol.generated.IncomingCodec
import zel.protocol.generated.PacketReader
import zel.protocol.generated.SSOTicketIn
import zel.protocol.generated.SSOTicketInCodec

object SsoTicketReader : IncomingCodec<SSOTicketIn> by SSOTicketInCodec {
    private const val INT_SIZE = 4

    override fun read(r: PacketReader): SSOTicketIn {
        val ticket = r.string()
        return SSOTicketIn(ticket, if (r.remaining >= INT_SIZE) r.int() else 0)
    }
}
