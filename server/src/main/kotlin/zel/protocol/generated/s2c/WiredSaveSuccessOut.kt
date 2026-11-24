package zel.protocol.generated

data object WiredSaveSuccessOut

object WiredSaveSuccessOutCodec : OutgoingCodec<WiredSaveSuccessOut> {
    override val name = "WiredSaveSuccessOut"

    override fun read(r: PacketReader): WiredSaveSuccessOut = WiredSaveSuccessOut

    override fun write(w: PacketWriter, m: WiredSaveSuccessOut) = writeWiredSaveSuccessOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeWiredSaveSuccessOut(w: PacketWriter, m: WiredSaveSuccessOut) {
    }
}
