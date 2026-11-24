package zel.protocol.generated

data class SetChestOptionsIn(
    val itemId: Int,
    val locked: Boolean,
    val autoLock: Boolean,
    val capacity: Int,
)

object SetChestOptionsInCodec : IncomingCodec<SetChestOptionsIn> {
    override val name = "SetChestOptionsIn"

    override fun read(r: PacketReader): SetChestOptionsIn = readSetChestOptionsIn(r)

    override fun write(w: PacketWriter, m: SetChestOptionsIn) = writeSetChestOptionsIn(w, m)

    private fun readSetChestOptionsIn(r: PacketReader): SetChestOptionsIn {
        val vItemId = r.int()
        val vLocked = r.boolean()
        val vAutoLock = r.boolean()
        val vCapacity = r.int()
        return SetChestOptionsIn(vItemId, vLocked, vAutoLock, vCapacity)
    }

    private fun writeSetChestOptionsIn(w: PacketWriter, m: SetChestOptionsIn) {
        w.int(m.itemId)
        w.boolean(m.locked)
        w.boolean(m.autoLock)
        w.int(m.capacity)
    }
}
