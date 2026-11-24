package zel.wired.protocol

import zel.protocol.generated.ObjectDataUpdateOut
import zel.protocol.generated.ObjectDataUpdateOutCodec
import zel.protocol.generated.OpenOut
import zel.protocol.generated.OpenOutCodec
import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.FurniBehaviour
import zel.room.furni.RoomFurni
import zel.room.furni.Usable
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Placeable
import zel.room.tick.Audience
import zel.room.tick.Lane

class BoxUse(
    private val levelOf: (Room, Int) -> Int?,
) : Usable,
    Placeable {
    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        if ((levelOf(scope.room, playerId) ?: 0) < RIGHTS) return
        furni.state = if (furni.state == "1") "0" else "1"
        furni.dirty = true
        scope.out.send(
            Lane.Now,
            Audience.Room,
            ObjectDataUpdateOutCodec,
            ObjectDataUpdateOut(furni.id.toString(), FurniWire.stuff(furni)),
        )
        scope.out.send(Lane.Now, Audience.Player(playerId), OpenOutCodec, OpenOut(furni.id))
    }

    override fun onPlaced(
        scope: RoomScope,
        furni: RoomFurni,
    ) = scope.out.send(Lane.Now, Audience.Player(furni.ownerId), OpenOutCodec, OpenOut(furni.id))

    fun entries(codes: Collection<String>): List<Pair<String, FurniBehaviour>> = codes.map { it to this }

    private companion object {
        const val RIGHTS = 1
    }
}
