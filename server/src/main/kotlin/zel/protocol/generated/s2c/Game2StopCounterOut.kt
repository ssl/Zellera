package zel.protocol.generated

data object Game2StopCounterOut

object Game2StopCounterOutCodec : OutgoingCodec<Game2StopCounterOut> {
    override val name = "Game2StopCounterOut"

    override fun read(r: PacketReader): Game2StopCounterOut = Game2StopCounterOut

    override fun write(w: PacketWriter, m: Game2StopCounterOut) = writeGame2StopCounterOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGame2StopCounterOut(w: PacketWriter, m: Game2StopCounterOut) {
    }
}
