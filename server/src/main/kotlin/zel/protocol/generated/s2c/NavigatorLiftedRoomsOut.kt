package zel.protocol.generated

data class NavigatorLiftedRoomsOut(
    val rooms: List<RoomsItem>,
) {
    data class RoomsItem(
        val flatId: Int,
        val areaId: Int,
        val image: String,
        val caption: String,
    )
}

object NavigatorLiftedRoomsOutCodec : OutgoingCodec<NavigatorLiftedRoomsOut> {
    override val name = "NavigatorLiftedRoomsOut"

    override fun read(r: PacketReader): NavigatorLiftedRoomsOut = readNavigatorLiftedRoomsOut(r)

    override fun write(w: PacketWriter, m: NavigatorLiftedRoomsOut) = writeNavigatorLiftedRoomsOut(w, m)

    private fun readNavigatorLiftedRoomsOutRoomsItem(r: PacketReader): NavigatorLiftedRoomsOut.RoomsItem {
        val vFlatId = r.int()
        val vAreaId = r.int()
        val vImage = r.string()
        val vCaption = r.string()
        return NavigatorLiftedRoomsOut.RoomsItem(vFlatId, vAreaId, vImage, vCaption)
    }

    private fun readNavigatorLiftedRoomsOut(r: PacketReader): NavigatorLiftedRoomsOut {
        val vRooms = List(r.countInt(12)) { readNavigatorLiftedRoomsOutRoomsItem(r) }
        return NavigatorLiftedRoomsOut(vRooms)
    }

    private fun writeNavigatorLiftedRoomsOutRoomsItem(w: PacketWriter, m: NavigatorLiftedRoomsOut.RoomsItem) {
        w.int(m.flatId)
        w.int(m.areaId)
        w.string(m.image)
        w.string(m.caption)
    }

    private fun writeNavigatorLiftedRoomsOut(w: PacketWriter, m: NavigatorLiftedRoomsOut) {
        w.countInt(m.rooms.size)
        m.rooms.forEach { writeNavigatorLiftedRoomsOutRoomsItem(w, it) }
    }
}
