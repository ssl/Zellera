package zel.protocol.generated

data class ApplySnapshotIn(
    val itemId: Int,
)

object ApplySnapshotInCodec : IncomingCodec<ApplySnapshotIn> {
    override val name = "ApplySnapshotIn"

    override fun read(r: PacketReader): ApplySnapshotIn = readApplySnapshotIn(r)

    override fun write(w: PacketWriter, m: ApplySnapshotIn) = writeApplySnapshotIn(w, m)

    private fun readApplySnapshotIn(r: PacketReader): ApplySnapshotIn {
        val vItemId = r.int()
        return ApplySnapshotIn(vItemId)
    }

    private fun writeApplySnapshotIn(w: PacketWriter, m: ApplySnapshotIn) {
        w.int(m.itemId)
    }
}
