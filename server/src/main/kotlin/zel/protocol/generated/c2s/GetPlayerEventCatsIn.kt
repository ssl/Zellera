package zel.protocol.generated

data object GetPlayerEventCatsIn

object GetPlayerEventCatsInCodec : IncomingCodec<GetPlayerEventCatsIn> {
    override val name = "GetPlayerEventCatsIn"

    override fun read(r: PacketReader): GetPlayerEventCatsIn = GetPlayerEventCatsIn

    override fun write(w: PacketWriter, m: GetPlayerEventCatsIn) = writeGetPlayerEventCatsIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetPlayerEventCatsIn(w: PacketWriter, m: GetPlayerEventCatsIn) {
    }
}
