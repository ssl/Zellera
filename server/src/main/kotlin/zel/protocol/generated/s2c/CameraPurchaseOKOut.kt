package zel.protocol.generated

data object CameraPurchaseOKOut

object CameraPurchaseOKOutCodec : OutgoingCodec<CameraPurchaseOKOut> {
    override val name = "CameraPurchaseOKOut"

    override fun read(r: PacketReader): CameraPurchaseOKOut = CameraPurchaseOKOut

    override fun write(w: PacketWriter, m: CameraPurchaseOKOut) = writeCameraPurchaseOKOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeCameraPurchaseOKOut(w: PacketWriter, m: CameraPurchaseOKOut) {
    }
}
