package zel.protocol.generated

data class RemoveAllRightsIn(
    val int1: Int,
)

object RemoveAllRightsInCodec : IncomingCodec<RemoveAllRightsIn> {
    override val name = "RemoveAllRightsIn"

    override fun read(r: PacketReader): RemoveAllRightsIn = readRemoveAllRightsIn(r)

    override fun write(w: PacketWriter, m: RemoveAllRightsIn) = writeRemoveAllRightsIn(w, m)

    private fun readRemoveAllRightsIn(r: PacketReader): RemoveAllRightsIn {
        val vInt1 = r.int()
        return RemoveAllRightsIn(vInt1)
    }

    private fun writeRemoveAllRightsIn(w: PacketWriter, m: RemoveAllRightsIn) {
        w.int(m.int1)
    }
}
