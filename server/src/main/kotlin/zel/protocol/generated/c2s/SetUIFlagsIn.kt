package zel.protocol.generated

data class SetUIFlagsIn(
    val flags: Int,
)

object SetUIFlagsInCodec : IncomingCodec<SetUIFlagsIn> {
    override val name = "SetUIFlagsIn"

    override fun read(r: PacketReader): SetUIFlagsIn = readSetUIFlagsIn(r)

    override fun write(w: PacketWriter, m: SetUIFlagsIn) = writeSetUIFlagsIn(w, m)

    private fun readSetUIFlagsIn(r: PacketReader): SetUIFlagsIn {
        val vFlags = r.int()
        return SetUIFlagsIn(vFlags)
    }

    private fun writeSetUIFlagsIn(w: PacketWriter, m: SetUIFlagsIn) {
        w.int(m.flags)
    }
}
