package zel.protocol.generated

data class GenericErrorOut(
    val int1: Int,
)

object GenericErrorOutCodec : OutgoingCodec<GenericErrorOut> {
    override val name = "GenericErrorOut"

    override fun read(r: PacketReader): GenericErrorOut = readGenericErrorOut(r)

    override fun write(w: PacketWriter, m: GenericErrorOut) = writeGenericErrorOut(w, m)

    private fun readGenericErrorOut(r: PacketReader): GenericErrorOut {
        val vInt1 = r.int()
        return GenericErrorOut(vInt1)
    }

    private fun writeGenericErrorOut(w: PacketWriter, m: GenericErrorOut) {
        w.int(m.int1)
    }
}
