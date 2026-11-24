package zel.protocol.generated

data class SetIgnoreRoomInvitesIn(
    val ignore: Boolean,
)

object SetIgnoreRoomInvitesInCodec : IncomingCodec<SetIgnoreRoomInvitesIn> {
    override val name = "SetIgnoreRoomInvitesIn"

    override fun read(r: PacketReader): SetIgnoreRoomInvitesIn = readSetIgnoreRoomInvitesIn(r)

    override fun write(w: PacketWriter, m: SetIgnoreRoomInvitesIn) = writeSetIgnoreRoomInvitesIn(w, m)

    private fun readSetIgnoreRoomInvitesIn(r: PacketReader): SetIgnoreRoomInvitesIn {
        val vIgnore = r.boolean()
        return SetIgnoreRoomInvitesIn(vIgnore)
    }

    private fun writeSetIgnoreRoomInvitesIn(w: PacketWriter, m: SetIgnoreRoomInvitesIn) {
        w.boolean(m.ignore)
    }
}
