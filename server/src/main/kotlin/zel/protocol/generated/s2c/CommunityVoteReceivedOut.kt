package zel.protocol.generated

data class CommunityVoteReceivedOut(
    val accepted: Boolean,
)

object CommunityVoteReceivedOutCodec : OutgoingCodec<CommunityVoteReceivedOut> {
    override val name = "CommunityVoteReceivedOut"

    override fun read(r: PacketReader): CommunityVoteReceivedOut = readCommunityVoteReceivedOut(r)

    override fun write(w: PacketWriter, m: CommunityVoteReceivedOut) = writeCommunityVoteReceivedOut(w, m)

    private fun readCommunityVoteReceivedOut(r: PacketReader): CommunityVoteReceivedOut {
        val vAccepted = r.boolean()
        return CommunityVoteReceivedOut(vAccepted)
    }

    private fun writeCommunityVoteReceivedOut(w: PacketWriter, m: CommunityVoteReceivedOut) {
        w.boolean(m.accepted)
    }
}
