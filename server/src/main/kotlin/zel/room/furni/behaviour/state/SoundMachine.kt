package zel.room.furni.behaviour.state

import zel.game.items.JukeboxTrack
import zel.protocol.generated.PlayListOut
import zel.protocol.generated.PlayListOutCodec
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.util.MILLIS_PER_SECOND

class SoundMachine(
    part: StatePart,
    private val store: SoundMachineStore,
    private val track: (Int) -> JukeboxTrack?,
) : Multistate(part) {
    override fun show(
        scope: RoomScope,
        furni: RoomFurni,
        state: String,
    ) {
        val playing = part.of(scope.room).playingSince
        if (state == ON) playing[furni.id] = scope.now() else playing.remove(furni.id)
        super.show(scope, furni, state)
    }

    /** The playlist of the machine playing in [scope]'s room; a room without one gets no answer. */
    fun answer(
        scope: RoomScope,
        playerId: Int,
    ) {
        val states = part.of(scope.room)
        val machine =
            scope.room.furni
                .all()
                .firstOrNull { it.behaviour === this && it.state == ON } ?: return
        val since = states.playingSince.getOrPut(machine.id) { states.firstTickAt ?: scope.now() }
        val songs =
            states.playlists.getOrPut(machine.id) { store.songs(machine.id) }.mapNotNull { id ->
                track(id)?.let {
                    PlayListOut.List1Item(
                        it.id,
                        (it.length * MILLIS_PER_SECOND).toInt(),
                        it.name.orEmpty(),
                        it.author.orEmpty(),
                    )
                }
            }
        val position = (scope.now() - since).toInt()
        scope.out.send(Lane.Now, Audience.Player(playerId), PlayListOutCodec, PlayListOut(position, songs))
    }

    private companion object {
        const val ON = "1"
    }
}
