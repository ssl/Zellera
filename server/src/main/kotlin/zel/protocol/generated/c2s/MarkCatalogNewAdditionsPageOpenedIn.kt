package zel.protocol.generated

data object MarkCatalogNewAdditionsPageOpenedIn

object MarkCatalogNewAdditionsPageOpenedInCodec : IncomingCodec<MarkCatalogNewAdditionsPageOpenedIn> {
    override val name = "MarkCatalogNewAdditionsPageOpenedIn"

    override fun read(r: PacketReader): MarkCatalogNewAdditionsPageOpenedIn = MarkCatalogNewAdditionsPageOpenedIn

    override fun write(w: PacketWriter, m: MarkCatalogNewAdditionsPageOpenedIn) = writeMarkCatalogNewAdditionsPageOpenedIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeMarkCatalogNewAdditionsPageOpenedIn(w: PacketWriter, m: MarkCatalogNewAdditionsPageOpenedIn) {
    }
}
