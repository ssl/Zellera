package zel.protocol.generated

data class RentableSpaceStatusOut(
    val boolean1: Boolean,
    val int1: Int,
    val int2: Int,
    val string1: String,
    val int3: Int,
    val int4: Int,
)

object RentableSpaceStatusOutCodec : OutgoingCodec<RentableSpaceStatusOut> {
    override val name = "RentableSpaceStatusOut"

    override fun read(r: PacketReader): RentableSpaceStatusOut = readRentableSpaceStatusOut(r)

    override fun write(w: PacketWriter, m: RentableSpaceStatusOut) = writeRentableSpaceStatusOut(w, m)

    private fun readRentableSpaceStatusOut(r: PacketReader): RentableSpaceStatusOut {
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vInt3 = r.int()
        val vInt4 = r.int()
        return RentableSpaceStatusOut(vBoolean1, vInt1, vInt2, vString1, vInt3, vInt4)
    }

    private fun writeRentableSpaceStatusOut(w: PacketWriter, m: RentableSpaceStatusOut) {
        w.boolean(m.boolean1)
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.int(m.int3)
        w.int(m.int4)
    }
}
