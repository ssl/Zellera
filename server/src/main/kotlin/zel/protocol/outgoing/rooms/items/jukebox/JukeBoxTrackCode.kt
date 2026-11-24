package zel.protocol.outgoing.rooms.items.jukebox

import zel.game.items.JukeboxTrack
import zel.protocol.generated.OfficialSongIdOut
import zel.protocol.generated.OfficialSongIdOutCodec
import zel.protocol.outgoing.OutgoingMessage

class JukeBoxTrackCode(
    private val track: JukeboxTrack,
) : OutgoingMessage<OfficialSongIdOut>(OfficialSongIdOutCodec) {
    override fun message() = OfficialSongIdOut(track.code.orEmpty(), track.id)
}
