package zel.protocol.generated

data object GetCatalogPageWithEarliestExpiryIn

object GetCatalogPageWithEarliestExpiryInCodec : IncomingCodec<GetCatalogPageWithEarliestExpiryIn> {
    override val name = "GetCatalogPageWithEarliestExpiryIn"

    override fun read(r: PacketReader): GetCatalogPageWithEarliestExpiryIn = GetCatalogPageWithEarliestExpiryIn

    override fun write(w: PacketWriter, m: GetCatalogPageWithEarliestExpiryIn) = writeGetCatalogPageWithEarliestExpiryIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetCatalogPageWithEarliestExpiryIn(w: PacketWriter, m: GetCatalogPageWithEarliestExpiryIn) {
    }
}
