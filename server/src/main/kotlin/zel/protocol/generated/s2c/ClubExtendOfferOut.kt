package zel.protocol.generated

data class ClubExtendOfferOut(
    val int1: Int,
    val string1: String,
    val boolean1: Boolean,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val boolean2: Boolean,
    val int5: Int,
    val int6: Int,
    val boolean3: Boolean,
    val int7: Int,
    val int8: Int,
    val int9: Int,
    val int10: Int,
    val int11: Int,
    val int12: Int,
    val int13: Int,
    val int14: Int,
)

object ClubExtendOfferOutCodec : OutgoingCodec<ClubExtendOfferOut> {
    override val name = "ClubExtendOfferOut"

    override fun read(r: PacketReader): ClubExtendOfferOut = readClubExtendOfferOut(r)

    override fun write(w: PacketWriter, m: ClubExtendOfferOut) = writeClubExtendOfferOut(w, m)

    private fun readClubExtendOfferOut(r: PacketReader): ClubExtendOfferOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vBoolean2 = r.boolean()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vBoolean3 = r.boolean()
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vInt9 = r.int()
        val vInt10 = r.int()
        val vInt11 = r.int()
        val vInt12 = r.int()
        val vInt13 = r.int()
        val vInt14 = r.int()
        return ClubExtendOfferOut(vInt1, vString1, vBoolean1, vInt2, vInt3, vInt4, vBoolean2, vInt5, vInt6, vBoolean3, vInt7, vInt8, vInt9, vInt10, vInt11, vInt12, vInt13, vInt14)
    }

    private fun writeClubExtendOfferOut(w: PacketWriter, m: ClubExtendOfferOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.boolean(m.boolean1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.boolean(m.boolean2)
        w.int(m.int5)
        w.int(m.int6)
        w.boolean(m.boolean3)
        w.int(m.int7)
        w.int(m.int8)
        w.int(m.int9)
        w.int(m.int10)
        w.int(m.int11)
        w.int(m.int12)
        w.int(m.int13)
        w.int(m.int14)
    }
}
