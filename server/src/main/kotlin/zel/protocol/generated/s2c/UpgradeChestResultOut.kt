package zel.protocol.generated

data class UpgradeChestResultOut(
    val int1: Int,
    val int2: Int,
)

object UpgradeChestResultOutCodec : OutgoingCodec<UpgradeChestResultOut> {
    override val name = "UpgradeChestResultOut"

    override fun read(r: PacketReader): UpgradeChestResultOut = readUpgradeChestResultOut(r)

    override fun write(w: PacketWriter, m: UpgradeChestResultOut) = writeUpgradeChestResultOut(w, m)

    private fun readUpgradeChestResultOut(r: PacketReader): UpgradeChestResultOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return UpgradeChestResultOut(vInt1, vInt2)
    }

    private fun writeUpgradeChestResultOut(w: PacketWriter, m: UpgradeChestResultOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
