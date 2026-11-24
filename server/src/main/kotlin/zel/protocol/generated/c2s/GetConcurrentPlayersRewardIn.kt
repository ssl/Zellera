package zel.protocol.generated

data object GetConcurrentPlayersRewardIn

object GetConcurrentPlayersRewardInCodec : IncomingCodec<GetConcurrentPlayersRewardIn> {
    override val name = "GetConcurrentPlayersRewardIn"

    override fun read(r: PacketReader): GetConcurrentPlayersRewardIn = GetConcurrentPlayersRewardIn

    override fun write(w: PacketWriter, m: GetConcurrentPlayersRewardIn) = writeGetConcurrentPlayersRewardIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetConcurrentPlayersRewardIn(w: PacketWriter, m: GetConcurrentPlayersRewardIn) {
    }
}
