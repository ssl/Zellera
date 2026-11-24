package zel.protocol.generated

data class GetCatalogPageExpirationIn(
    val string1: String,
)

object GetCatalogPageExpirationInCodec : IncomingCodec<GetCatalogPageExpirationIn> {
    override val name = "GetCatalogPageExpirationIn"

    override fun read(r: PacketReader): GetCatalogPageExpirationIn = readGetCatalogPageExpirationIn(r)

    override fun write(w: PacketWriter, m: GetCatalogPageExpirationIn) = writeGetCatalogPageExpirationIn(w, m)

    private fun readGetCatalogPageExpirationIn(r: PacketReader): GetCatalogPageExpirationIn {
        val vString1 = r.string()
        return GetCatalogPageExpirationIn(vString1)
    }

    private fun writeGetCatalogPageExpirationIn(w: PacketWriter, m: GetCatalogPageExpirationIn) {
        w.string(m.string1)
    }
}
