package zel.protocol.generated

data class RentableSpaceRentOkOut(
    val int1: Int,
)

object RentableSpaceRentOkOutCodec : OutgoingCodec<RentableSpaceRentOkOut> {
    override val name = "RentableSpaceRentOkOut"

    override fun read(r: PacketReader): RentableSpaceRentOkOut = readRentableSpaceRentOkOut(r)

    override fun write(w: PacketWriter, m: RentableSpaceRentOkOut) = writeRentableSpaceRentOkOut(w, m)

    private fun readRentableSpaceRentOkOut(r: PacketReader): RentableSpaceRentOkOut {
        val vInt1 = r.int()
        return RentableSpaceRentOkOut(vInt1)
    }

    private fun writeRentableSpaceRentOkOut(w: PacketWriter, m: RentableSpaceRentOkOut) {
        w.int(m.int1)
    }
}
