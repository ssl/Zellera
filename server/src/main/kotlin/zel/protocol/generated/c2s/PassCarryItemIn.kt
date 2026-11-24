package zel.protocol.generated

data class PassCarryItemIn(
    val userId: Int,
)

object PassCarryItemInCodec : IncomingCodec<PassCarryItemIn> {
    override val name = "PassCarryItemIn"

    override fun read(r: PacketReader): PassCarryItemIn = readPassCarryItemIn(r)

    override fun write(w: PacketWriter, m: PassCarryItemIn) = writePassCarryItemIn(w, m)

    private fun readPassCarryItemIn(r: PacketReader): PassCarryItemIn {
        val vUserId = r.int()
        return PassCarryItemIn(vUserId)
    }

    private fun writePassCarryItemIn(w: PacketWriter, m: PassCarryItemIn) {
        w.int(m.userId)
    }
}
