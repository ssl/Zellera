package zel.protocol.generated

data class GetCatalogIndexIn(
    val mODE: String,
)

object GetCatalogIndexInCodec : IncomingCodec<GetCatalogIndexIn> {
    override val name = "GetCatalogIndexIn"

    override fun read(r: PacketReader): GetCatalogIndexIn = readGetCatalogIndexIn(r)

    override fun write(w: PacketWriter, m: GetCatalogIndexIn) = writeGetCatalogIndexIn(w, m)

    private fun readGetCatalogIndexIn(r: PacketReader): GetCatalogIndexIn {
        val vMODE = r.string()
        return GetCatalogIndexIn(vMODE)
    }

    private fun writeGetCatalogIndexIn(w: PacketWriter, m: GetCatalogIndexIn) {
        w.string(m.mODE)
    }
}
