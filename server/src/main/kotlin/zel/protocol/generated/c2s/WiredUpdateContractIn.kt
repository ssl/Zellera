package zel.protocol.generated

data object WiredUpdateContractIn

object WiredUpdateContractInCodec : IncomingCodec<WiredUpdateContractIn> {
    override val name = "WiredUpdateContractIn"

    override fun read(r: PacketReader): WiredUpdateContractIn = WiredUpdateContractIn

    override fun write(w: PacketWriter, m: WiredUpdateContractIn) = writeWiredUpdateContractIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeWiredUpdateContractIn(w: PacketWriter, m: WiredUpdateContractIn) {
    }
}
