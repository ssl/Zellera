package zel.protocol.generated

data object GetPlayerSongDisksIn

object GetPlayerSongDisksInCodec : IncomingCodec<GetPlayerSongDisksIn> {
    override val name = "GetPlayerSongDisksIn"

    override fun read(r: PacketReader): GetPlayerSongDisksIn = GetPlayerSongDisksIn

    override fun write(w: PacketWriter, m: GetPlayerSongDisksIn) = writeGetPlayerSongDisksIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetPlayerSongDisksIn(w: PacketWriter, m: GetPlayerSongDisksIn) {
    }
}
