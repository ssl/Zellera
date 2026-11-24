package zel.protocol.generated

data class RentableSpaceStatusIn(
    val itemId: Int,
)

object RentableSpaceStatusInCodec : IncomingCodec<RentableSpaceStatusIn> {
    override val name = "RentableSpaceStatusIn"

    override fun read(r: PacketReader): RentableSpaceStatusIn = readRentableSpaceStatusIn(r)

    override fun write(w: PacketWriter, m: RentableSpaceStatusIn) = writeRentableSpaceStatusIn(w, m)

    private fun readRentableSpaceStatusIn(r: PacketReader): RentableSpaceStatusIn {
        val vItemId = r.int()
        return RentableSpaceStatusIn(vItemId)
    }

    private fun writeRentableSpaceStatusIn(w: PacketWriter, m: RentableSpaceStatusIn) {
        w.int(m.itemId)
    }
}
