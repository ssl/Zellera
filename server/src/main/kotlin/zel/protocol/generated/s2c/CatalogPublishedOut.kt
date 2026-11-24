package zel.protocol.generated

data class CatalogPublishedOut(
    val boolean1: Boolean,
    val trailing1: String?,
)

object CatalogPublishedOutCodec : OutgoingCodec<CatalogPublishedOut> {
    override val name = "CatalogPublishedOut"

    override fun read(r: PacketReader): CatalogPublishedOut = readCatalogPublishedOut(r)

    override fun write(w: PacketWriter, m: CatalogPublishedOut) = writeCatalogPublishedOut(w, m)

    private fun readCatalogPublishedOut(r: PacketReader): CatalogPublishedOut {
        val vBoolean1 = r.boolean()
        val vTrailing1 = if (r.remaining > 0) r.string() else null
        return CatalogPublishedOut(vBoolean1, vTrailing1)
    }

    private fun writeCatalogPublishedOut(w: PacketWriter, m: CatalogPublishedOut) {
        w.boolean(m.boolean1)
        m.trailing1?.let { w.string(it) }
    }
}
