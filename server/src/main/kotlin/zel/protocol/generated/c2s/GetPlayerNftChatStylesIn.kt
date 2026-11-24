package zel.protocol.generated

data object GetPlayerNftChatStylesIn

object GetPlayerNftChatStylesInCodec : IncomingCodec<GetPlayerNftChatStylesIn> {
    override val name = "GetPlayerNftChatStylesIn"

    override fun read(r: PacketReader): GetPlayerNftChatStylesIn = GetPlayerNftChatStylesIn

    override fun write(w: PacketWriter, m: GetPlayerNftChatStylesIn) = writeGetPlayerNftChatStylesIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetPlayerNftChatStylesIn(w: PacketWriter, m: GetPlayerNftChatStylesIn) {
    }
}
