package zel.protocol.generated

data class WiredOpenContractOut(
    val int1: Int,
)

object WiredOpenContractOutCodec : OutgoingCodec<WiredOpenContractOut> {
    override val name = "WiredOpenContractOut"

    override fun read(r: PacketReader): WiredOpenContractOut = readWiredOpenContractOut(r)

    override fun write(w: PacketWriter, m: WiredOpenContractOut) = writeWiredOpenContractOut(w, m)

    private fun readWiredOpenContractOut(r: PacketReader): WiredOpenContractOut {
        val vInt1 = r.int()
        return WiredOpenContractOut(vInt1)
    }

    private fun writeWiredOpenContractOut(w: PacketWriter, m: WiredOpenContractOut) {
        w.int(m.int1)
    }
}
