package zel.room.games.football

import zel.game.players.Gender
import zel.game.players.figure.Figure
import zel.protocol.generated.StuffData
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Steppable
import zel.room.furni.behaviour.special.SpecialStuff
import zel.room.furni.behaviour.state.StateShow
import zel.room.furni.place.StuffOnWire
import zel.room.games.GamesPart
import zel.room.games.GateLook
import zel.room.games.StepScope
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind

class FootballGate(
    private val part: GamesPart,
) : Steppable,
    StuffOnWire {
    override fun onStepOn(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return
        val held = StepScope(scope)
        val services = part.context.host
        val looks = part.of(scope.room).gateLooks
        val own = looks.remove(unit.index)
        if (own != null) {
            services.wear(held, playerId, own.look)
            return
        }
        val figure = services.figure(playerId) ?: return
        val outfit = if (figure.gender == FEMALE) female(furni.state) else male(furni.state)
        looks[unit.index] = GateLook(playerId, figure.look)
        services.wear(held, playerId, services.outfitOver(figure.look, outfit, figure.gender, playerId))
    }

    override fun onStepOff(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = Unit

    override fun stuff(furni: RoomFurni): StuffData =
        SpecialStuff.legacy(furni, "${male(furni.state)},${female(furni.state)}")

    fun save(
        scope: RoomScope,
        furni: RoomFurni,
        gender: String,
        look: String,
    ) {
        if (!acceptsOutfit(gender, look)) return
        val isFemale = Gender.ofCode(gender) == Gender.F
        val state = if (isFemale) "${male(furni.state)};$look" else "$look;${female(furni.state)}"
        StateShow.whole(scope, furni, state)
    }

    private fun male(state: String): String = state.substringBefore(';')

    private fun female(state: String): String = state.substringAfter(';', "")

    companion object {
        private const val FEMALE = "F"

        fun acceptsOutfit(
            gender: String,
            look: String,
        ): Boolean = Gender.ofCode(gender) != null && look.isNotEmpty() && Figure.isWellFormed(look)
    }
}
