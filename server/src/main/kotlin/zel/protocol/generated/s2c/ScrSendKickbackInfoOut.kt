package zel.protocol.generated

data class ScrSendKickbackInfoOut(
    val int1: Int,
    val string1: String,
    val double1: Double,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val int6: Int,
    val int7: Int,
)

object ScrSendKickbackInfoOutCodec : OutgoingCodec<ScrSendKickbackInfoOut> {
    override val name = "ScrSendKickbackInfoOut"

    override fun read(r: PacketReader): ScrSendKickbackInfoOut = readScrSendKickbackInfoOut(r)

    override fun write(w: PacketWriter, m: ScrSendKickbackInfoOut) = writeScrSendKickbackInfoOut(w, m)

    private fun readScrSendKickbackInfoOut(r: PacketReader): ScrSendKickbackInfoOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vDouble1 = r.double()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        return ScrSendKickbackInfoOut(vInt1, vString1, vDouble1, vInt2, vInt3, vInt4, vInt5, vInt6, vInt7)
    }

    private fun writeScrSendKickbackInfoOut(w: PacketWriter, m: ScrSendKickbackInfoOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.double(m.double1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
    }
}
