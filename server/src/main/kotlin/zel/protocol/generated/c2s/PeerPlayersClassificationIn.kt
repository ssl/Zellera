package zel.protocol.generated

data class PeerPlayersClassificationIn(
    val string1: String,
)

object PeerPlayersClassificationInCodec : IncomingCodec<PeerPlayersClassificationIn> {
    override val name = "PeerPlayersClassificationIn"

    override fun read(r: PacketReader): PeerPlayersClassificationIn = readPeerPlayersClassificationIn(r)

    override fun write(w: PacketWriter, m: PeerPlayersClassificationIn) = writePeerPlayersClassificationIn(w, m)

    private fun readPeerPlayersClassificationIn(r: PacketReader): PeerPlayersClassificationIn {
        val vString1 = r.string()
        return PeerPlayersClassificationIn(vString1)
    }

    private fun writePeerPlayersClassificationIn(w: PacketWriter, m: PeerPlayersClassificationIn) {
        w.string(m.string1)
    }
}
