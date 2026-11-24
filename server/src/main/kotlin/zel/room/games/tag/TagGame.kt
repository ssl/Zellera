package zel.room.games.tag

import zel.game.achievements.GameEvents
import zel.game.items.FurniParams
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.behaviour.state.StateShow
import zel.room.games.GameContext
import zel.room.games.RoomGame
import zel.room.games.RoomGames
import zel.room.games.TeamColor
import zel.room.games.TeamMember
import zel.room.map.Point
import zel.room.tick.RoomTiming
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import kotlin.math.abs

class TagGame(
    context: GameContext,
    games: RoomGames,
    val kind: TagKind,
) : RoomGame(context, games) {
    private val taggers = LinkedHashMap<Int, Int?>()

    val steppedOn = HashMap<Int, Long>()

    override val scoreName = kind.scoreName

    override fun member(
        unit: RoomUnit,
        playerId: Int,
        color: TeamColor,
    ): TeamMember = TeamMember(unit, playerId, color)

    fun isTagger(playerId: Int): Boolean = playerId in taggers

    private fun polesOnField(scope: RoomScope): List<RoomFurni> =
        if (kind.hasPoles) {
            scope.room.furni
                .all()
                .filter { (it.behaviour as? TagPole)?.kind == kind }
        } else {
            emptyList()
        }

    /** The field's furni name the looks (runner male, female, tagger male, female); the kind's own stand in. */
    private fun looks(scope: RoomScope): Pair<TagLook, TagLook> {
        val named =
            scope.room.furni
                .all()
                .firstOrNull { (it.behaviour as? TagField)?.kind == kind && it.definition.params.isNotBlank() }
                ?.let { FurniParams.values(it.definition.params).take(LOOK_VALUES).map(String::toIntOrNull) }
        if (named == null || named.size < LOOK_VALUES || named.any { it == null || it < 0 }) {
            return kind.runner to kind.tagger
        }
        val effects = named.map { it ?: 0 }
        return TagLook(effects[RUNNER], effects[RUNNER + 1]) to TagLook(effects[TAGGER], effects[TAGGER + 1])
    }

    private fun normalEffect(
        scope: RoomScope,
        unit: RoomUnit,
    ): Int = looks(scope).first.effectFor(context.hooks.male(unit))

    private fun taggerEffect(
        scope: RoomScope,
        unit: RoomUnit,
    ): Int = looks(scope).second.effectFor(context.hooks.male(unit))

    fun joinField(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        val member = join(scope, unit, TeamColor.RED) ?: return
        val free = polesOnField(scope).filter { pole -> pole.id !in taggers.values }
        val becomes = if (kind.hasPoles) free.isNotEmpty() else taggers.isEmpty()
        context.hooks.giveEffect(scope, unit, normalEffect(scope, unit), null)
        if (!becomes) return
        taggers[member.playerId] = free.firstOrNull()?.id
        context.jobs.after(scope.room, RoomTiming.TICK_MILLIS) { later ->
            if (later.room.units[unit.index] === unit && isTagger(member.playerId)) {
                context.hooks.giveEffect(later, unit, taggerEffect(later, unit), null)
            }
        }
    }

    override fun leave(
        scope: RoomScope,
        member: TeamMember,
    ) {
        super.leave(scope, member)
        taggers.remove(member.playerId)
        if (member.present(scope)) context.hooks.giveEffect(scope, member.unit, 0, null)
    }

    fun stepped(
        scope: RoomScope,
        member: TeamMember,
        to: Point,
    ) {
        val onField =
            scope.room.furni
                .at(to)
                .furni
                .any { (it.behaviour as? TagField)?.kind == kind }
        when {
            !onField -> leave(scope, member)
            taggers.isEmpty() -> tagged(scope, null, member)
        }
    }

    fun lookedAt(
        scope: RoomScope,
        looker: TeamMember,
        point: Point,
    ) {
        if (!isTagger(looker.playerId) || !near(looker.unit.tile, point)) return
        val target =
            scope.room.units
                .all()
                .filter { it.tile == point && it !== looker.unit }
                .firstNotNullOfOrNull { unit -> (unit.kind as? UnitKind.Player)?.let { memberOf(it.playerId) } }
        if (target != null) tagged(scope, looker, target)
    }

    private fun tagged(
        scope: RoomScope,
        tagger: TeamMember?,
        target: TeamMember,
    ) {
        if (isTagger(target.playerId)) return
        val poles = polesOnField(scope)
        val held = tagger?.let { taggers[it.playerId] }
        context.hooks.giveEffect(scope, target.unit, taggerEffect(scope, target.unit), null)
        if (poles.size > taggers.size) {
            taggers[target.playerId] = poles.firstOrNull { it.id !in taggers.values }?.id
        } else {
            if (tagger != null) {
                context.hooks.giveEffect(scope, tagger.unit, normalEffect(scope, tagger.unit), null)
                taggers.remove(tagger.playerId)
            }
            taggers[target.playerId] = held
        }
        flashPole(scope, held)
        val tagged = GameEvents.of(GameEvents.GAME_TAG_TAGGED, kind.detail)
        if (tagger != null) context.hooks.progress(target.playerId, tagged, 1)
    }

    private fun flashPole(
        scope: RoomScope,
        poleId: Int?,
    ) {
        val pole = poleId?.let { scope.room.furni[it] } ?: return
        StateShow.data(scope, pole, "1")
        context.jobs.after(scope.room, POLE_RESET_MS) { later ->
            later.room.furni[poleId]?.let { StateShow.data(later, it, "0") }
        }
    }

    override fun endRound(scope: RoomScope) = Unit

    private fun near(
        a: Point,
        b: Point,
    ): Boolean = abs(a.x - b.x) <= 1 && abs(a.y - b.y) <= 1

    private companion object {
        const val POLE_RESET_MS = 1000L
        const val LOOK_VALUES = 4
        const val RUNNER = 0
        const val TAGGER = 2
    }
}
