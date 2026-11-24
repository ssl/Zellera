package zel.protocol.generated

data class CraftSecretIn(
    val altarId: Int,
    val count: Int,
    val itemIds: ByteArray,
)

object CraftSecretInCodec : IncomingCodec<CraftSecretIn> {
    override val name = "CraftSecretIn"

    override fun read(r: PacketReader): CraftSecretIn = readCraftSecretIn(r)

    override fun write(w: PacketWriter, m: CraftSecretIn) = writeCraftSecretIn(w, m)

    private fun readCraftSecretIn(r: PacketReader): CraftSecretIn {
        val vAltarId = r.int()
        val vCount = r.int()
        val vItemIds = r.rest()
        return CraftSecretIn(vAltarId, vCount, vItemIds)
    }

    private fun writeCraftSecretIn(w: PacketWriter, m: CraftSecretIn) {
        w.int(m.altarId)
        w.int(m.count)
        w.bytes(m.itemIds)
    }
}
