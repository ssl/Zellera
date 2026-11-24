package zel.protocol.generated

data class RoomInviteErrorOut(
    val int1: Int,
    val branch1: Branch1,
) {
    sealed interface Branch1 {
        data class Then(
            val list1: List<Int>,
        ) : Branch1
        data object Else : Branch1
    }
}

object RoomInviteErrorOutCodec : OutgoingCodec<RoomInviteErrorOut> {
    override val name = "RoomInviteErrorOut"

    override fun read(r: PacketReader): RoomInviteErrorOut = readRoomInviteErrorOut(r)

    override fun write(w: PacketWriter, m: RoomInviteErrorOut) = writeRoomInviteErrorOut(w, m)

    private fun readRoomInviteErrorOutBranch1Then(r: PacketReader): RoomInviteErrorOut.Branch1.Then {
        val vList1 = List(r.countInt(4)) { r.int() }
        return RoomInviteErrorOut.Branch1.Then(vList1)
    }

    private fun readRoomInviteErrorOut(r: PacketReader): RoomInviteErrorOut {
        val vInt1 = r.int()
        val vBranch1 = if (vInt1 == 1) readRoomInviteErrorOutBranch1Then(r) else RoomInviteErrorOut.Branch1.Else
        return RoomInviteErrorOut(vInt1, vBranch1)
    }

    private fun writeRoomInviteErrorOutBranch1Then(w: PacketWriter, m: RoomInviteErrorOut.Branch1.Then) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
    }

    private fun writeRoomInviteErrorOut(w: PacketWriter, m: RoomInviteErrorOut) {
        w.int(m.int1)
        when (val b = m.branch1) {
            is RoomInviteErrorOut.Branch1.Then -> writeRoomInviteErrorOutBranch1Then(w, b)
            is RoomInviteErrorOut.Branch1.Else -> {}
        }
    }
}
