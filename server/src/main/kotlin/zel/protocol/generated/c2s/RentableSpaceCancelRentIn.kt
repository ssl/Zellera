package zel.protocol.generated

data class RentableSpaceCancelRentIn(
    val itemId: Int,
)

object RentableSpaceCancelRentInCodec : IncomingCodec<RentableSpaceCancelRentIn> {
    override val name = "RentableSpaceCancelRentIn"

    override fun read(r: PacketReader): RentableSpaceCancelRentIn = readRentableSpaceCancelRentIn(r)

    override fun write(w: PacketWriter, m: RentableSpaceCancelRentIn) = writeRentableSpaceCancelRentIn(w, m)

    private fun readRentableSpaceCancelRentIn(r: PacketReader): RentableSpaceCancelRentIn {
        val vItemId = r.int()
        return RentableSpaceCancelRentIn(vItemId)
    }

    private fun writeRentableSpaceCancelRentIn(w: PacketWriter, m: RentableSpaceCancelRentIn) {
        w.int(m.itemId)
    }
}
