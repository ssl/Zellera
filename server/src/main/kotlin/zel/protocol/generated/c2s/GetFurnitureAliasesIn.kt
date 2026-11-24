package zel.protocol.generated

data object GetFurnitureAliasesIn

object GetFurnitureAliasesInCodec : IncomingCodec<GetFurnitureAliasesIn> {
    override val name = "GetFurnitureAliasesIn"

    override fun read(r: PacketReader): GetFurnitureAliasesIn = GetFurnitureAliasesIn

    override fun write(w: PacketWriter, m: GetFurnitureAliasesIn) = writeGetFurnitureAliasesIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetFurnitureAliasesIn(w: PacketWriter, m: GetFurnitureAliasesIn) {
    }
}
