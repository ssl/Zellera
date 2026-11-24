package zel.protocol.generated

data object GetPetInventoryIn

object GetPetInventoryInCodec : IncomingCodec<GetPetInventoryIn> {
    override val name = "GetPetInventoryIn"

    override fun read(r: PacketReader): GetPetInventoryIn = GetPetInventoryIn

    override fun write(w: PacketWriter, m: GetPetInventoryIn) = writeGetPetInventoryIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetPetInventoryIn(w: PacketWriter, m: GetPetInventoryIn) {
    }
}
