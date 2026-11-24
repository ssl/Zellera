package zel.protocol.generated

data class ResetUnseenItemIdsIn(
    val category: Int,
    val itemIds: List<Int>,
)

object ResetUnseenItemIdsInCodec : IncomingCodec<ResetUnseenItemIdsIn> {
    override val name = "ResetUnseenItemIdsIn"

    override fun read(r: PacketReader): ResetUnseenItemIdsIn = readResetUnseenItemIdsIn(r)

    override fun write(w: PacketWriter, m: ResetUnseenItemIdsIn) = writeResetUnseenItemIdsIn(w, m)

    private fun readResetUnseenItemIdsIn(r: PacketReader): ResetUnseenItemIdsIn {
        val vCategory = r.int()
        val vItemIds = List(r.countInt(4)) { r.int() }
        return ResetUnseenItemIdsIn(vCategory, vItemIds)
    }

    private fun writeResetUnseenItemIdsIn(w: PacketWriter, m: ResetUnseenItemIdsIn) {
        w.int(m.category)
        w.countInt(m.itemIds.size)
        m.itemIds.forEach { w.int(it) }
    }
}
