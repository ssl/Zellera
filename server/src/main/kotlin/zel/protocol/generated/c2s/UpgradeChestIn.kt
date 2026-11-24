package zel.protocol.generated

data class UpgradeChestIn(
    val itemId: Int,
    val quantity: Int,
)

object UpgradeChestInCodec : IncomingCodec<UpgradeChestIn> {
    override val name = "UpgradeChestIn"

    override fun read(r: PacketReader): UpgradeChestIn = readUpgradeChestIn(r)

    override fun write(w: PacketWriter, m: UpgradeChestIn) = writeUpgradeChestIn(w, m)

    private fun readUpgradeChestIn(r: PacketReader): UpgradeChestIn {
        val vItemId = r.int()
        val vQuantity = r.int()
        return UpgradeChestIn(vItemId, vQuantity)
    }

    private fun writeUpgradeChestIn(w: PacketWriter, m: UpgradeChestIn) {
        w.int(m.itemId)
        w.int(m.quantity)
    }
}
