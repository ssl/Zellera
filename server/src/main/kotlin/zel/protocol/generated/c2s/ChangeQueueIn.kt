package zel.protocol.generated

data class ChangeQueueIn(
    val target: Int,
)

object ChangeQueueInCodec : IncomingCodec<ChangeQueueIn> {
    override val name = "ChangeQueueIn"

    override fun read(r: PacketReader): ChangeQueueIn = readChangeQueueIn(r)

    override fun write(w: PacketWriter, m: ChangeQueueIn) = writeChangeQueueIn(w, m)

    private fun readChangeQueueIn(r: PacketReader): ChangeQueueIn {
        val vTarget = r.int()
        return ChangeQueueIn(vTarget)
    }

    private fun writeChangeQueueIn(w: PacketWriter, m: ChangeQueueIn) {
        w.int(m.target)
    }
}
