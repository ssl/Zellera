package zel.protocol.generated

data class UniqueMachineIDOut(
    val string1: String,
)

object UniqueMachineIDOutCodec : OutgoingCodec<UniqueMachineIDOut> {
    override val name = "UniqueMachineIDOut"

    override fun read(r: PacketReader): UniqueMachineIDOut = readUniqueMachineIDOut(r)

    override fun write(w: PacketWriter, m: UniqueMachineIDOut) = writeUniqueMachineIDOut(w, m)

    private fun readUniqueMachineIDOut(r: PacketReader): UniqueMachineIDOut {
        val vString1 = r.string()
        return UniqueMachineIDOut(vString1)
    }

    private fun writeUniqueMachineIDOut(w: PacketWriter, m: UniqueMachineIDOut) {
        w.string(m.string1)
    }
}
