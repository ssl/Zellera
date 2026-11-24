package zel.protocol.generated

data object FurniListInvalidateOut

object FurniListInvalidateOutCodec : OutgoingCodec<FurniListInvalidateOut> {
    override val name = "FurniListInvalidateOut"

    override fun read(r: PacketReader): FurniListInvalidateOut = FurniListInvalidateOut

    override fun write(w: PacketWriter, m: FurniListInvalidateOut) = writeFurniListInvalidateOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeFurniListInvalidateOut(w: PacketWriter, m: FurniListInvalidateOut) {
    }
}
