package zel.protocol.generated

data object GetOccupiedTilesIn

object GetOccupiedTilesInCodec : IncomingCodec<GetOccupiedTilesIn> {
    override val name = "GetOccupiedTilesIn"

    override fun read(r: PacketReader): GetOccupiedTilesIn = GetOccupiedTilesIn

    override fun write(w: PacketWriter, m: GetOccupiedTilesIn) = writeGetOccupiedTilesIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetOccupiedTilesIn(w: PacketWriter, m: GetOccupiedTilesIn) {
    }
}
