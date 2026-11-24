package zel.protocol.generated

data class DirectSMSClubBuyAvailableOut(
    val string1: String,
    val string2: String,
    val int1: Int,
)

object DirectSMSClubBuyAvailableOutCodec : OutgoingCodec<DirectSMSClubBuyAvailableOut> {
    override val name = "DirectSMSClubBuyAvailableOut"

    override fun read(r: PacketReader): DirectSMSClubBuyAvailableOut = readDirectSMSClubBuyAvailableOut(r)

    override fun write(w: PacketWriter, m: DirectSMSClubBuyAvailableOut) = writeDirectSMSClubBuyAvailableOut(w, m)

    private fun readDirectSMSClubBuyAvailableOut(r: PacketReader): DirectSMSClubBuyAvailableOut {
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt1 = r.int()
        return DirectSMSClubBuyAvailableOut(vString1, vString2, vInt1)
    }

    private fun writeDirectSMSClubBuyAvailableOut(w: PacketWriter, m: DirectSMSClubBuyAvailableOut) {
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int1)
    }
}
