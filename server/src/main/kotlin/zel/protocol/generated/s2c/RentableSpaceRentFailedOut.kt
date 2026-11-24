package zel.protocol.generated

data class RentableSpaceRentFailedOut(
    val int1: Int,
)

object RentableSpaceRentFailedOutCodec : OutgoingCodec<RentableSpaceRentFailedOut> {
    override val name = "RentableSpaceRentFailedOut"

    override fun read(r: PacketReader): RentableSpaceRentFailedOut = readRentableSpaceRentFailedOut(r)

    override fun write(w: PacketWriter, m: RentableSpaceRentFailedOut) = writeRentableSpaceRentFailedOut(w, m)

    private fun readRentableSpaceRentFailedOut(r: PacketReader): RentableSpaceRentFailedOut {
        val vInt1 = r.int()
        return RentableSpaceRentFailedOut(vInt1)
    }

    private fun writeRentableSpaceRentFailedOut(w: PacketWriter, m: RentableSpaceRentFailedOut) {
        w.int(m.int1)
    }
}
