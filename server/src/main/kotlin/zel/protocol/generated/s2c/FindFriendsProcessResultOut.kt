package zel.protocol.generated

data class FindFriendsProcessResultOut(
    val boolean1: Boolean,
)

object FindFriendsProcessResultOutCodec : OutgoingCodec<FindFriendsProcessResultOut> {
    override val name = "FindFriendsProcessResultOut"

    override fun read(r: PacketReader): FindFriendsProcessResultOut = readFindFriendsProcessResultOut(r)

    override fun write(w: PacketWriter, m: FindFriendsProcessResultOut) = writeFindFriendsProcessResultOut(w, m)

    private fun readFindFriendsProcessResultOut(r: PacketReader): FindFriendsProcessResultOut {
        val vBoolean1 = r.boolean()
        return FindFriendsProcessResultOut(vBoolean1)
    }

    private fun writeFindFriendsProcessResultOut(w: PacketWriter, m: FindFriendsProcessResultOut) {
        w.boolean(m.boolean1)
    }
}
