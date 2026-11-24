package zel.protocol.generated

data class ResetUnseenItemsIn(
    val category: Int,
)

object ResetUnseenItemsInCodec : IncomingCodec<ResetUnseenItemsIn> {
    override val name = "ResetUnseenItemsIn"

    override fun read(r: PacketReader): ResetUnseenItemsIn = readResetUnseenItemsIn(r)

    override fun write(w: PacketWriter, m: ResetUnseenItemsIn) = writeResetUnseenItemsIn(w, m)

    private fun readResetUnseenItemsIn(r: PacketReader): ResetUnseenItemsIn {
        val vCategory = r.int()
        return ResetUnseenItemsIn(vCategory)
    }

    private fun writeResetUnseenItemsIn(w: PacketWriter, m: ResetUnseenItemsIn) {
        w.int(m.category)
    }
}
