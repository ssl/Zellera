package zel.protocol.generated

data class FurniRecolorIn(
    val furniId: Int,
    val scope: Int,
    val red: Int,
    val green: Int,
    val blue: Int,
)

object FurniRecolorInCodec : IncomingCodec<FurniRecolorIn> {
    override val name = "FurniRecolorIn"

    override fun read(r: PacketReader): FurniRecolorIn = readFurniRecolorIn(r)

    override fun write(w: PacketWriter, m: FurniRecolorIn) = writeFurniRecolorIn(w, m)

    private fun readFurniRecolorIn(r: PacketReader): FurniRecolorIn {
        val vFurniId = r.int()
        val vScope = r.int()
        val vRed = r.int()
        val vGreen = r.int()
        val vBlue = r.int()
        return FurniRecolorIn(vFurniId, vScope, vRed, vGreen, vBlue)
    }

    private fun writeFurniRecolorIn(w: PacketWriter, m: FurniRecolorIn) {
        w.int(m.furniId)
        w.int(m.scope)
        w.int(m.red)
        w.int(m.green)
        w.int(m.blue)
    }
}
