package zel.protocol.generated

data object RequestFurniInventoryIn

object RequestFurniInventoryInCodec : IncomingCodec<RequestFurniInventoryIn> {
    override val name = "RequestFurniInventoryIn"

    override fun read(r: PacketReader): RequestFurniInventoryIn = RequestFurniInventoryIn

    override fun write(w: PacketWriter, m: RequestFurniInventoryIn) = writeRequestFurniInventoryIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeRequestFurniInventoryIn(w: PacketWriter, m: RequestFurniInventoryIn) {
    }
}
