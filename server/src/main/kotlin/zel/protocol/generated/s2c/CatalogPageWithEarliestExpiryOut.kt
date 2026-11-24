package zel.protocol.generated

data class CatalogPageWithEarliestExpiryOut(
    val pageName: String,
    val secondsToExpiry: Int,
    val image: String,
)

object CatalogPageWithEarliestExpiryOutCodec : OutgoingCodec<CatalogPageWithEarliestExpiryOut> {
    override val name = "CatalogPageWithEarliestExpiryOut"

    override fun read(r: PacketReader): CatalogPageWithEarliestExpiryOut = readCatalogPageWithEarliestExpiryOut(r)

    override fun write(w: PacketWriter, m: CatalogPageWithEarliestExpiryOut) = writeCatalogPageWithEarliestExpiryOut(w, m)

    private fun readCatalogPageWithEarliestExpiryOut(r: PacketReader): CatalogPageWithEarliestExpiryOut {
        val vPageName = r.string()
        val vSecondsToExpiry = r.int()
        val vImage = r.string()
        return CatalogPageWithEarliestExpiryOut(vPageName, vSecondsToExpiry, vImage)
    }

    private fun writeCatalogPageWithEarliestExpiryOut(w: PacketWriter, m: CatalogPageWithEarliestExpiryOut) {
        w.string(m.pageName)
        w.int(m.secondsToExpiry)
        w.string(m.image)
    }
}
