package zel.protocol.generated

data class SpinWheelOfFortuneIn(
    val itemId: Int,
)

object SpinWheelOfFortuneInCodec : IncomingCodec<SpinWheelOfFortuneIn> {
    override val name = "SpinWheelOfFortuneIn"

    override fun read(r: PacketReader): SpinWheelOfFortuneIn = readSpinWheelOfFortuneIn(r)

    override fun write(w: PacketWriter, m: SpinWheelOfFortuneIn) = writeSpinWheelOfFortuneIn(w, m)

    private fun readSpinWheelOfFortuneIn(r: PacketReader): SpinWheelOfFortuneIn {
        val vItemId = r.int()
        return SpinWheelOfFortuneIn(vItemId)
    }

    private fun writeSpinWheelOfFortuneIn(w: PacketWriter, m: SpinWheelOfFortuneIn) {
        w.int(m.itemId)
    }
}
