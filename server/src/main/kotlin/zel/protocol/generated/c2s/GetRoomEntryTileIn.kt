package zel.protocol.generated

data object GetRoomEntryTileIn

object GetRoomEntryTileInCodec : IncomingCodec<GetRoomEntryTileIn> {
    override val name = "GetRoomEntryTileIn"

    override fun read(r: PacketReader): GetRoomEntryTileIn = GetRoomEntryTileIn

    override fun write(w: PacketWriter, m: GetRoomEntryTileIn) = writeGetRoomEntryTileIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetRoomEntryTileIn(w: PacketWriter, m: GetRoomEntryTileIn) {
    }
}
