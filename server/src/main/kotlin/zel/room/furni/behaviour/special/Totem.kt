package zel.room.furni.behaviour.special

import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.Toggleable
import zel.room.furni.Usable
import zel.room.furni.behaviour.state.Multistate
import zel.room.furni.behaviour.state.StateHooks
import zel.room.furni.behaviour.state.StateShow
import kotlin.random.Random

/** One piece of a totem: legs at the bottom of a tile's stack, a head on them, a planet on top. */
class Totem(
    private val piece: Part,
    private val hooks: StateHooks,
    /** Runs on the persistence executor. */
    private val wear: (playerId: Int, effect: Int) -> Unit = { _, _ -> },
) : Usable,
    Toggleable {
    enum class Part { Legs, Head, Planet }

    /** What legs or a head show, both counted from 0; a head that stands on no legs has no tint. */
    data class Look(
        val figure: Int,
        val tint: Int?,
    ) {
        fun headState(figures: Int): Int = if (tint == null) figure else figures + figure * TINTS + tint

        companion object {
            fun ofLegs(state: Int): Look = Look(state / TINTS, state % TINTS)

            fun ofHead(
                state: Int,
                figures: Int,
            ): Look = if (state < figures) Look(state, null) else ofLegs(state - figures)
        }
    }

    /** A totem that gives an effect: the states its three pieces show. */
    data class Recipe(
        val legs: Int,
        val head: Int,
        val planet: Int,
        val effect: Int,
    ) {
        companion object {
            private const val LAST_FIELD = 3
            private val FORMAT = Regex("""\((\d+),(\d+),(\d+)=(\d+)\)""")

            fun listOf(params: String): List<Recipe> =
                FORMAT
                    .findAll(params.filterNot(Char::isWhitespace))
                    .map { found -> found.groupValues.drop(1).map(String::toInt) }
                    .map { Recipe(legs = it[0], head = it[1], planet = it[2], effect = it[LAST_FIELD]) }
                    .toList()
        }
    }

    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        val unit = scope.room.units.ofPlayer(playerId) ?: return
        val turns = hooks.hasRights(scope, playerId)
        when (piece) {
            Part.Legs -> {
                if (!turns) return
                val next = Multistate.stepped(furni, backward = false) ?: return
                StateShow.data(scope, furni, next)
                scope.raise(RoomEvent.StateChanged(furni.id, unit.index))
                pieces(scope, furni, Part.Head).filter { it.z > furni.z }.forEach { tint(scope, it, shown(it).figure) }
            }

            Part.Head -> {
                if (turns) tint(scope, furni, (shown(furni).figure + 1) % figures(furni))
            }

            Part.Planet -> {
                val effect = if (furni.ownerId == playerId) effect(scope, furni) else null
                if (effect != null) {
                    wear(playerId, effect)
                } else if (turns) {
                    Multistate.stepped(furni, backward = false)?.let { StateShow.data(scope, furni, it) }
                }
            }
        }
    }

    override fun toggle(
        scope: RoomScope,
        furni: RoomFurni,
        backward: Boolean,
    ): Boolean = false

    override fun toggleRandom(
        scope: RoomScope,
        furni: RoomFurni,
        random: Random,
    ): Boolean = false

    /** Shows [figure] on a head in the tint of the legs it stands on. */
    private fun tint(
        scope: RoomScope,
        head: RoomFurni,
        figure: Int,
    ) {
        val legs = pieces(scope, head, Part.Legs).lastOrNull { it.z < head.z }
        val look = Look(figure, legs?.let { Look.ofLegs(state(it)).tint })
        StateShow.data(scope, head, look.headState(figures(head)).toString())
    }

    private fun effect(
        scope: RoomScope,
        planet: RoomFurni,
    ): Int? {
        val legs = pieces(scope, planet, Part.Legs).lastOrNull { it.z < planet.z } ?: return null
        val head = pieces(scope, planet, Part.Head).lastOrNull { it.z > legs.z && it.z < planet.z } ?: return null
        val shown = Triple(state(legs), state(head), state(planet))
        return Recipe
            .listOf(planet.definition.params)
            .firstOrNull { Triple(it.legs, it.head, it.planet) == shown }
            ?.effect
    }

    private fun pieces(
        scope: RoomScope,
        furni: RoomFurni,
        part: Part,
    ): List<RoomFurni> {
        val tile = furni.anchor ?: return emptyList()
        return scope.room.furni
            .at(tile)
            .furni
            .filter { (it.behaviour as? Totem)?.piece == part }
            .sortedBy { it.z }
    }

    private fun shown(head: RoomFurni): Look = Look.ofHead(state(head), figures(head))

    /** A head has one bare picture of every figure and one in each tint. */
    private fun figures(head: RoomFurni): Int = maxOf(1, head.definition.stateCount / (TINTS + 1))

    private fun state(furni: RoomFurni): Int = furni.state.trim().toIntOrNull() ?: 0

    private companion object {
        const val TINTS = 4
    }
}
