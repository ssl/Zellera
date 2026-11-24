package zel.protocol.generated

data class ModerateRoomIn(
    val roomId: Int,
    val lockDoor: Int,
    val changeTitle: Int,
    val kickUsers: Int,
)

object ModerateRoomInCodec : IncomingCodec<ModerateRoomIn> {
    override val name = "ModerateRoomIn"

    override fun read(r: PacketReader): ModerateRoomIn = readModerateRoomIn(r)

    override fun write(w: PacketWriter, m: ModerateRoomIn) = writeModerateRoomIn(w, m)

    private fun readModerateRoomIn(r: PacketReader): ModerateRoomIn {
        val vRoomId = r.int()
        val vLockDoor = r.int()
        val vChangeTitle = r.int()
        val vKickUsers = r.int()
        return ModerateRoomIn(vRoomId, vLockDoor, vChangeTitle, vKickUsers)
    }

    private fun writeModerateRoomIn(w: PacketWriter, m: ModerateRoomIn) {
        w.int(m.roomId)
        w.int(m.lockDoor)
        w.int(m.changeTitle)
        w.int(m.kickUsers)
    }
}
