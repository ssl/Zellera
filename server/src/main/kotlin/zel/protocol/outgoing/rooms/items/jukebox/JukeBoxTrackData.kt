package zel.protocol.outgoing.rooms.items.jukebox

import zel.game.items.JukeboxTrack
import zel.protocol.generated.TraxSongInfoOut
import zel.protocol.generated.TraxSongInfoOutCodec
import zel.protocol.outgoing.OutgoingMessage
import zel.util.MILLIS_PER_SECOND

class JukeBoxTrackData(
    private val tracks: List<JukeboxTrack>,
) : OutgoingMessage<TraxSongInfoOut>(TraxSongInfoOutCodec) {
    override fun message() =
        TraxSongInfoOut(
            tracks.map {
                TraxSongInfoOut.List1Item(
                    it.id,
                    it.code.orEmpty(),
                    it.name.orEmpty(),
                    it.data.orEmpty(),
                    (it.length * MILLIS_PER_SECOND).toInt(),
                    it.author.orEmpty(),
                )
            },
        )
}
