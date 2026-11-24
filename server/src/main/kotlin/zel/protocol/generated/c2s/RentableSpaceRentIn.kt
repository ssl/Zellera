package zel.protocol.generated

data class RentableSpaceRentIn(
    val itemId: Int,
)

object RentableSpaceRentInCodec : IncomingCodec<RentableSpaceRentIn> {
    override val name = "RentableSpaceRentIn"

    override fun read(r: PacketReader): RentableSpaceRentIn = readRentableSpaceRentIn(r)

    override fun write(w: PacketWriter, m: RentableSpaceRentIn) = writeRentableSpaceRentIn(w, m)

    private fun readRentableSpaceRentIn(r: PacketReader): RentableSpaceRentIn {
        val vItemId = r.int()
        return RentableSpaceRentIn(vItemId)
    }

    private fun writeRentableSpaceRentIn(w: PacketWriter, m: RentableSpaceRentIn) {
        w.int(m.itemId)
    }
}
