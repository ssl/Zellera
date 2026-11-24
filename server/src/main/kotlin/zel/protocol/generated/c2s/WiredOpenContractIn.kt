package zel.protocol.generated

data class WiredOpenContractIn(
    val int1: Int,
)

object WiredOpenContractInCodec : IncomingCodec<WiredOpenContractIn> {
    override val name = "WiredOpenContractIn"

    override fun read(r: PacketReader): WiredOpenContractIn = readWiredOpenContractIn(r)

    override fun write(w: PacketWriter, m: WiredOpenContractIn) = writeWiredOpenContractIn(w, m)

    private fun readWiredOpenContractIn(r: PacketReader): WiredOpenContractIn {
        val vInt1 = r.int()
        return WiredOpenContractIn(vInt1)
    }

    private fun writeWiredOpenContractIn(w: PacketWriter, m: WiredOpenContractIn) {
        w.int(m.int1)
    }
}
