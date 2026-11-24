package zel.protocol.generated

data class CfhChatlogOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val byte1: Byte,
    val rest: ByteArray,
)

object CfhChatlogOutCodec : OutgoingCodec<CfhChatlogOut> {
    override val name = "CfhChatlogOut"

    override fun read(r: PacketReader): CfhChatlogOut = readCfhChatlogOut(r)

    override fun write(w: PacketWriter, m: CfhChatlogOut) = writeCfhChatlogOut(w, m)

    private fun readCfhChatlogOut(r: PacketReader): CfhChatlogOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vByte1 = r.byte()
        val vRest = r.rest()
        return CfhChatlogOut(vInt1, vInt2, vInt3, vInt4, vByte1, vRest)
    }

    private fun writeCfhChatlogOut(w: PacketWriter, m: CfhChatlogOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.byte(m.byte1)
        w.bytes(m.rest)
    }
}
