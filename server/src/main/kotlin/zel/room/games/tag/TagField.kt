package zel.room.games.tag

import zel.game.achievements.GameEvents
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Steppable
import zel.room.furni.place.Placeable
import zel.room.games.GamesPart
import zel.room.games.StepScope
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.walk.StepGate
import zel.util.MILLIS_PER_MINUTE

class TagField(
    private val part: GamesPart,
    val kind: TagKind,
) : Steppable,
    StepGate,
    Placeable {
    override fun lets(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ): Boolean = unit.kind is UnitKind.Player

    override fun refused(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = Unit

    override fun onStepOn(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return
        val games = part.of(scope.room)
        val game = games.tagOrNew(kind)
        if (games.playing[playerId] == null) game.joinField(StepScope(scope), unit)
        game.steppedOn[playerId] = scope.now()
    }

    override fun onStepOff(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return
        val game = part.of(scope.room).tagOrNew(kind)
        val since = game.steppedOn.remove(playerId) ?: return
        val minutes = ((scope.now() - since) / MILLIS_PER_MINUTE).toInt()
        if (minutes > 0) part.context.hooks.progress(playerId, event(GameEvents.GAME_TAG_MINUTES), minutes)
    }

    override fun onPlaced(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        if (part.of(scope.room).firstFieldPlacement(furni.id)) {
            part.context.hooks.progress(furni.ownerId, event(GameEvents.GAME_TAG_PLACED), 1)
        }
    }

    private fun event(name: String): String = GameEvents.of(name, kind.detail)
}
