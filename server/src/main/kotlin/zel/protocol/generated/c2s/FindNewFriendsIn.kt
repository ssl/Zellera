package zel.protocol.generated

data object FindNewFriendsIn

object FindNewFriendsInCodec : IncomingCodec<FindNewFriendsIn> {
    override val name = "FindNewFriendsIn"

    override fun read(r: PacketReader): FindNewFriendsIn = FindNewFriendsIn

    override fun write(w: PacketWriter, m: FindNewFriendsIn) = writeFindNewFriendsIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeFindNewFriendsIn(w: PacketWriter, m: FindNewFriendsIn) {
    }
}
