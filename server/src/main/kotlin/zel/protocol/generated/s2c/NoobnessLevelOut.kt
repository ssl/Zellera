package zel.protocol.generated

data class NoobnessLevelOut(
    val int1: Int,
)

object NoobnessLevelOutCodec : OutgoingCodec<NoobnessLevelOut> {
    override val name = "NoobnessLevelOut"

    override fun read(r: PacketReader): NoobnessLevelOut = readNoobnessLevelOut(r)

    override fun write(w: PacketWriter, m: NoobnessLevelOut) = writeNoobnessLevelOut(w, m)

    private fun readNoobnessLevelOut(r: PacketReader): NoobnessLevelOut {
        val vInt1 = r.int()
        return NoobnessLevelOut(vInt1)
    }

    private fun writeNoobnessLevelOut(w: PacketWriter, m: NoobnessLevelOut) {
        w.int(m.int1)
    }
}
