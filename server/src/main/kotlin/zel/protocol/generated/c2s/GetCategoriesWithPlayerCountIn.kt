package zel.protocol.generated

data object GetCategoriesWithPlayerCountIn

object GetCategoriesWithPlayerCountInCodec : IncomingCodec<GetCategoriesWithPlayerCountIn> {
    override val name = "GetCategoriesWithPlayerCountIn"

    override fun read(r: PacketReader): GetCategoriesWithPlayerCountIn = GetCategoriesWithPlayerCountIn

    override fun write(w: PacketWriter, m: GetCategoriesWithPlayerCountIn) = writeGetCategoriesWithPlayerCountIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetCategoriesWithPlayerCountIn(w: PacketWriter, m: GetCategoriesWithPlayerCountIn) {
    }
}
