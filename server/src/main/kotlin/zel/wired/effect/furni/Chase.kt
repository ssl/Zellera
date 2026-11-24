package zel.wired.effect.furni

import zel.room.Room
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets
import kotlin.random.Random

class Chase(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val moves: FurniMoves,
    private val random: Random,
    private val standsOn: (RoomScope, RoomUnit) -> Point,
) : EffectBox,
    TriesMoves {
    override val negative = false
    override val needsUser = false

    private val lastStep = HashMap<Int, Int>()

    override fun tryMoves(
        firing: Firing,
        trial: MoveTrial,
    ) {
        val room = firing.scope.room
        Targets.furni(firing, this).filterNot { it.isWall }.forEach { furni ->
            val anchor = furni.anchor ?: return@forEach
            val seen = LineSight.from(room, anchor) { standsOn(firing.scope, it) }
            when {
                seen == null -> {
                    wander(room, furni, anchor)?.let { trial.move(furni, anchor.step(it)) } ?: trial.refuse()
                }

                seen.distance == 1 -> {
                    trial.refuse()
                }

                else -> {
                    trial.move(furni, anchor.step(seen.direction))
                }
            }
        }
    }

    override fun run(firing: Firing) {
        Targets.furni(firing, this).filterNot { it.isWall }.forEach { pursue(firing, it) }
    }

    /** Caught is a collision and no step; a way that is shut sends the furni wandering, as seeing nobody does. */
    private fun pursue(
        firing: Firing,
        furni: RoomFurni,
    ) {
        val anchor = furni.anchor ?: return
        val room = firing.scope.room
        val seen = LineSight.from(room, anchor) { standsOn(firing.scope, it) }
        if (seen != null && seen.distance == 1) {
            firing.scope.raise(RoomEvent.Collision(seen.unit.index, furni.id))
            return
        }
        val direction =
            seen?.direction?.takeIf { free(room, furni, anchor, it) } ?: wander(room, furni, anchor) ?: return
        if (moves.move(firing, furni, anchor.step(direction))) lastStep[furni.id] = direction
    }

    private fun wander(
        room: Room,
        furni: RoomFurni,
        anchor: Point,
    ): Int? {
        val open = LineSight.STRAIGHT.filter { free(room, furni, anchor, it) }
        if (open.size == 1) return open.single()
        val back = lastStep[furni.id]?.let { LineSight.opposite(it) }
        return open.filterNot { it == back }.randomOrNull(random)
    }

    private fun free(
        room: Room,
        furni: RoomFurni,
        anchor: Point,
        direction: Int,
    ): Boolean = moves.landing(room, furni, anchor.step(direction)) != null
}
