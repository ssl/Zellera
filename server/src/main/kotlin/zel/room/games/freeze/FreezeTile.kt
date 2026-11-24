package zel.room.games.freeze

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Usable
import zel.room.games.FieldFurni
import zel.room.games.GamesPart

class FreezeTile(
    private val part: GamesPart,
) : FieldFurni(),
    Usable {
    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        val unit = scope.room.units.ofPlayer(playerId) ?: return
        if (unit.tile == furni.anchor) part.of(scope.room).freeze?.throwBall(scope, playerId, furni)
    }
}
