package zel.protocol.generated

data object GetConcurrentPlayersGoalProgressIn

object GetConcurrentPlayersGoalProgressInCodec : IncomingCodec<GetConcurrentPlayersGoalProgressIn> {
    override val name = "GetConcurrentPlayersGoalProgressIn"

    override fun read(r: PacketReader): GetConcurrentPlayersGoalProgressIn = GetConcurrentPlayersGoalProgressIn

    override fun write(w: PacketWriter, m: GetConcurrentPlayersGoalProgressIn) = writeGetConcurrentPlayersGoalProgressIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetConcurrentPlayersGoalProgressIn(w: PacketWriter, m: GetConcurrentPlayersGoalProgressIn) {
    }
}
