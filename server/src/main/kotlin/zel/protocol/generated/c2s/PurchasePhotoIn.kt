package zel.protocol.generated

data object PurchasePhotoIn

object PurchasePhotoInCodec : IncomingCodec<PurchasePhotoIn> {
    override val name = "PurchasePhotoIn"

    override fun read(r: PacketReader): PurchasePhotoIn = PurchasePhotoIn

    override fun write(w: PacketWriter, m: PurchasePhotoIn) = writePurchasePhotoIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writePurchasePhotoIn(w: PacketWriter, m: PurchasePhotoIn) {
    }
}
