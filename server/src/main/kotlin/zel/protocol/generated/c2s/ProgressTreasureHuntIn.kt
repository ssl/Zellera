package zel.protocol.generated

data class ProgressTreasureHuntIn(
    val string1: String,
    val string2: String,
)

object ProgressTreasureHuntInCodec : IncomingCodec<ProgressTreasureHuntIn> {
    override val name = "ProgressTreasureHuntIn"

    override fun read(r: PacketReader): ProgressTreasureHuntIn = readProgressTreasureHuntIn(r)

    override fun write(w: PacketWriter, m: ProgressTreasureHuntIn) = writeProgressTreasureHuntIn(w, m)

    private fun readProgressTreasureHuntIn(r: PacketReader): ProgressTreasureHuntIn {
        val vString1 = r.string()
        val vString2 = r.string()
        return ProgressTreasureHuntIn(vString1, vString2)
    }

    private fun writeProgressTreasureHuntIn(w: PacketWriter, m: ProgressTreasureHuntIn) {
        w.string(m.string1)
        w.string(m.string2)
    }
}
