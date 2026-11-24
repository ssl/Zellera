package zel.protocol.generated

data class CatalogPageExpirationOut(
    val int1: Int,
    val string1: String,
    val int2: Int,
    val string2: String,
)

object CatalogPageExpirationOutCodec : OutgoingCodec<CatalogPageExpirationOut> {
    override val name = "CatalogPageExpirationOut"

    override fun read(r: PacketReader): CatalogPageExpirationOut = readCatalogPageExpirationOut(r)

    override fun write(w: PacketWriter, m: CatalogPageExpirationOut) = writeCatalogPageExpirationOut(w, m)

    private fun readCatalogPageExpirationOut(r: PacketReader): CatalogPageExpirationOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vString2 = r.string()
        return CatalogPageExpirationOut(vInt1, vString1, vInt2, vString2)
    }

    private fun writeCatalogPageExpirationOut(w: PacketWriter, m: CatalogPageExpirationOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.string(m.string2)
    }
}
