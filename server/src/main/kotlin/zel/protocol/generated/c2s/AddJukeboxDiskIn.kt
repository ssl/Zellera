package zel.protocol.generated

data class AddJukeboxDiskIn(
    val itemId: Int,
    val int1: Int,
)

object AddJukeboxDiskInCodec : IncomingCodec<AddJukeboxDiskIn> {
    override val name = "AddJukeboxDiskIn"

    override fun read(r: PacketReader): AddJukeboxDiskIn = readAddJukeboxDiskIn(r)

    override fun write(w: PacketWriter, m: AddJukeboxDiskIn) = writeAddJukeboxDiskIn(w, m)

    private fun readAddJukeboxDiskIn(r: PacketReader): AddJukeboxDiskIn {
        val vItemId = r.int()
        val vInt1 = r.int()
        return AddJukeboxDiskIn(vItemId, vInt1)
    }

    private fun writeAddJukeboxDiskIn(w: PacketWriter, m: AddJukeboxDiskIn) {
        w.int(m.itemId)
        w.int(m.int1)
    }
}
