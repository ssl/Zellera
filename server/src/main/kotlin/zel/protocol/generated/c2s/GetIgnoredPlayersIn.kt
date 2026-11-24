package zel.protocol.generated

data object GetIgnoredPlayersIn

object GetIgnoredPlayersInCodec : IncomingCodec<GetIgnoredPlayersIn> {
    override val name = "GetIgnoredPlayersIn"

    override fun read(r: PacketReader): GetIgnoredPlayersIn = GetIgnoredPlayersIn

    override fun write(w: PacketWriter, m: GetIgnoredPlayersIn) = writeGetIgnoredPlayersIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetIgnoredPlayersIn(w: PacketWriter, m: GetIgnoredPlayersIn) {
    }
}
