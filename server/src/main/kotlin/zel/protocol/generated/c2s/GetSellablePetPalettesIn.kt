package zel.protocol.generated

data class GetSellablePetPalettesIn(
    val petName: String,
)

object GetSellablePetPalettesInCodec : IncomingCodec<GetSellablePetPalettesIn> {
    override val name = "GetSellablePetPalettesIn"

    override fun read(r: PacketReader): GetSellablePetPalettesIn = readGetSellablePetPalettesIn(r)

    override fun write(w: PacketWriter, m: GetSellablePetPalettesIn) = writeGetSellablePetPalettesIn(w, m)

    private fun readGetSellablePetPalettesIn(r: PacketReader): GetSellablePetPalettesIn {
        val vPetName = r.string()
        return GetSellablePetPalettesIn(vPetName)
    }

    private fun writeGetSellablePetPalettesIn(w: PacketWriter, m: GetSellablePetPalettesIn) {
        w.string(m.petName)
    }
}
