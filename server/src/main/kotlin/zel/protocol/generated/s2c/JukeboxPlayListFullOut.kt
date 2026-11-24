package zel.protocol.generated

data object JukeboxPlayListFullOut

object JukeboxPlayListFullOutCodec : OutgoingCodec<JukeboxPlayListFullOut> {
    override val name = "JukeboxPlayListFullOut"

    override fun read(r: PacketReader): JukeboxPlayListFullOut = JukeboxPlayListFullOut

    override fun write(w: PacketWriter, m: JukeboxPlayListFullOut) = writeJukeboxPlayListFullOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeJukeboxPlayListFullOut(w: PacketWriter, m: JukeboxPlayListFullOut) {
    }
}
