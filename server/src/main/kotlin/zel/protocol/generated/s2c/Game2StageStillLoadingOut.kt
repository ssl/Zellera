package zel.protocol.generated

data class Game2StageStillLoadingOut(
    val int1: Int,
    val list1: List<Int>,
)

object Game2StageStillLoadingOutCodec : OutgoingCodec<Game2StageStillLoadingOut> {
    override val name = "Game2StageStillLoadingOut"

    override fun read(r: PacketReader): Game2StageStillLoadingOut = readGame2StageStillLoadingOut(r)

    override fun write(w: PacketWriter, m: Game2StageStillLoadingOut) = writeGame2StageStillLoadingOut(w, m)

    private fun readGame2StageStillLoadingOut(r: PacketReader): Game2StageStillLoadingOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(4)) { r.int() }
        return Game2StageStillLoadingOut(vInt1, vList1)
    }

    private fun writeGame2StageStillLoadingOut(w: PacketWriter, m: Game2StageStillLoadingOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
    }
}
