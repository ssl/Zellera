package zel.protocol.generated

data object FriendRequestQuestCompleteIn

object FriendRequestQuestCompleteInCodec : IncomingCodec<FriendRequestQuestCompleteIn> {
    override val name = "FriendRequestQuestCompleteIn"

    override fun read(r: PacketReader): FriendRequestQuestCompleteIn = FriendRequestQuestCompleteIn

    override fun write(w: PacketWriter, m: FriendRequestQuestCompleteIn) = writeFriendRequestQuestCompleteIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeFriendRequestQuestCompleteIn(w: PacketWriter, m: FriendRequestQuestCompleteIn) {
    }
}
