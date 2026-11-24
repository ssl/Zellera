package zel.protocol.generated

data class MysteryBoxWaitingCanceledIn(
    val int1: Int,
)

object MysteryBoxWaitingCanceledInCodec : IncomingCodec<MysteryBoxWaitingCanceledIn> {
    override val name = "MysteryBoxWaitingCanceledIn"

    override fun read(r: PacketReader): MysteryBoxWaitingCanceledIn = readMysteryBoxWaitingCanceledIn(r)

    override fun write(w: PacketWriter, m: MysteryBoxWaitingCanceledIn) = writeMysteryBoxWaitingCanceledIn(w, m)

    private fun readMysteryBoxWaitingCanceledIn(r: PacketReader): MysteryBoxWaitingCanceledIn {
        val vInt1 = r.int()
        return MysteryBoxWaitingCanceledIn(vInt1)
    }

    private fun writeMysteryBoxWaitingCanceledIn(w: PacketWriter, m: MysteryBoxWaitingCanceledIn) {
        w.int(m.int1)
    }
}
