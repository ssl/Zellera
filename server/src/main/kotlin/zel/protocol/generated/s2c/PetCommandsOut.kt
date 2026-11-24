package zel.protocol.generated

data class PetCommandsOut(
    val int1: Int,
    val list1: List<Int>,
    val list2: List<Int>,
)

object PetCommandsOutCodec : OutgoingCodec<PetCommandsOut> {
    override val name = "PetCommandsOut"

    override fun read(r: PacketReader): PetCommandsOut = readPetCommandsOut(r)

    override fun write(w: PacketWriter, m: PetCommandsOut) = writePetCommandsOut(w, m)

    private fun readPetCommandsOut(r: PacketReader): PetCommandsOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(4)) { r.int() }
        val vList2 = List(r.countInt(4)) { r.int() }
        return PetCommandsOut(vInt1, vList1, vList2)
    }

    private fun writePetCommandsOut(w: PacketWriter, m: PetCommandsOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.countInt(m.list2.size)
        m.list2.forEach { w.int(it) }
    }
}
