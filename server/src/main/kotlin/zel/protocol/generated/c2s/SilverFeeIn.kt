package zel.protocol.generated

data class SilverFeeIn(
    val boolean1: Boolean,
)

object SilverFeeInCodec : IncomingCodec<SilverFeeIn> {
    override val name = "SilverFeeIn"

    override fun read(r: PacketReader): SilverFeeIn = readSilverFeeIn(r)

    override fun write(w: PacketWriter, m: SilverFeeIn) = writeSilverFeeIn(w, m)

    private fun readSilverFeeIn(r: PacketReader): SilverFeeIn {
        val vBoolean1 = r.boolean()
        return SilverFeeIn(vBoolean1)
    }

    private fun writeSilverFeeIn(w: PacketWriter, m: SilverFeeIn) {
        w.boolean(m.boolean1)
    }
}
