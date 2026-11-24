package zel.protocol.generated

data class RemoveJukeboxDiskIn(
    val index: Int,
)

object RemoveJukeboxDiskInCodec : IncomingCodec<RemoveJukeboxDiskIn> {
    override val name = "RemoveJukeboxDiskIn"

    override fun read(r: PacketReader): RemoveJukeboxDiskIn = readRemoveJukeboxDiskIn(r)

    override fun write(w: PacketWriter, m: RemoveJukeboxDiskIn) = writeRemoveJukeboxDiskIn(w, m)

    private fun readRemoveJukeboxDiskIn(r: PacketReader): RemoveJukeboxDiskIn {
        val vIndex = r.int()
        return RemoveJukeboxDiskIn(vIndex)
    }

    private fun writeRemoveJukeboxDiskIn(w: PacketWriter, m: RemoveJukeboxDiskIn) {
        w.int(m.index)
    }
}
