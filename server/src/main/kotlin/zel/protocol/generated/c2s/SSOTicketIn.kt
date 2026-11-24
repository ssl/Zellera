package zel.protocol.generated

data class SSOTicketIn(
    val string1: String,
    val int1: Int,
)

object SSOTicketInCodec : IncomingCodec<SSOTicketIn> {
    override val name = "SSOTicketIn"

    override fun read(r: PacketReader): SSOTicketIn = readSSOTicketIn(r)

    override fun write(w: PacketWriter, m: SSOTicketIn) = writeSSOTicketIn(w, m)

    private fun readSSOTicketIn(r: PacketReader): SSOTicketIn {
        val vString1 = r.string()
        val vInt1 = r.int()
        return SSOTicketIn(vString1, vInt1)
    }

    private fun writeSSOTicketIn(w: PacketWriter, m: SSOTicketIn) {
        w.string(m.string1)
        w.int(m.int1)
    }
}
