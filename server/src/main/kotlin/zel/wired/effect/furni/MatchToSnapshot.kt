package zel.wired.effect.furni

import zel.protocol.generated.ItemsStateUpdateOut
import zel.protocol.generated.ItemsStateUpdateOutCodec
import zel.room.Room
import zel.room.furni.FurniKind
import zel.room.furni.RoomFurni
import zel.room.furni.behaviour.state.AreaHide
import zel.room.furni.behaviour.state.Controller
import zel.room.map.Point
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.box.FurniSnapshot
import zel.wired.box.WiredBox
import zel.wired.box.pattern.ToggleState
import zel.wired.firing.Firing
import zel.wired.firing.FurniSource
import zel.wired.firing.Targets

class MatchToSnapshot(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val moves: FurniMoves,
    private val walls: WallMoves,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    private val matchState = settings.ints.getOrNull(STATE) == ON
    private val matchDirection = settings.ints.getOrNull(DIRECTION) == ON
    private val matchPosition = settings.ints.getOrNull(POSITION) == ON
    private val matchAltitude = settings.ints.getOrNull(ALTITUDE) == ON

    private val resolved: WiredBox =
        object : WiredBox {
            override val furniId = this@MatchToSnapshot.furniId
            override val settings =
                this@MatchToSnapshot.settings.copy(
                    furniSources = this@MatchToSnapshot.settings.furniSources.map(::picksForTriggering),
                )
        }

    override fun run(firing: Firing) {
        Targets.furni(firing, resolved).forEach { furni ->
            settings.snapshots.firstOrNull { it.furniId == furni.id }?.let { apply(firing, furni, it) }
        }
    }

    fun applyAll(firing: Firing) {
        settings.snapshots.forEach { saved ->
            firing.scope.room.furni[saved.furniId]
                ?.let { apply(firing, it, saved) }
        }
    }

    private fun picksForTriggering(source: Int): Int =
        if (source == FurniSource.TRIGGERING && settings.picks.isNotEmpty()) FurniSource.PICKED else source

    private fun apply(
        firing: Firing,
        furni: RoomFurni,
        saved: FurniSnapshot,
    ) {
        if (matchState) state(firing, furni, saved)
        if (furni.isWall) {
            wall(firing, furni, saved)
        } else {
            floor(firing, furni, saved)
        }
    }

    private fun state(
        firing: Firing,
        furni: RoomFurni,
        saved: FurniSnapshot,
    ) {
        if (!ToggleState.toggles(furni)) return
        val area = furni.behaviour as? AreaHide
        if (area != null) {
            // Its snapshot keeps only on / off: it switches and keeps its area.
            if (area.switchTo(firing.scope, furni, AreaHide.snapshotOn(saved.state), flash = firing.flash)) {
                firing.flash.changed(furni.id)
            }
            return
        }
        val next =
            if (furni.kind == FurniKind.Recolorable) {
                saved.state.substringBefore(COLOUR) + COLOUR + furni.state.substringAfter(COLOUR, "")
            } else {
                saved.state
            }
        if (next == furni.state) return
        ToggleState.changesHeights(firing, furni) {
            furni.state = next
            furni.dirty = true
            true
        }
        Controller.written(firing.scope, furni)
        if (furni.isWall) {
            val update = ItemsStateUpdateOut(listOf(ItemsStateUpdateOut.List1Item(furni.id, next)))
            firing.flash.later { it.out.send(Lane.Now, Audience.Room, ItemsStateUpdateOutCodec, update) }
        } else {
            firing.flash.changed(furni.id)
        }
    }

    private fun floor(
        firing: Firing,
        furni: RoomFurni,
        saved: FurniSnapshot,
    ) {
        val anchor = furni.anchor ?: return
        val direction = if (matchDirection) saved.direction else furni.direction
        val z = if (matchAltitude) saved.z else null
        val tile = if (matchPosition) Point(saved.x, saved.y) else anchor
        // A furni already on its saved tile and direction only changes height: a lift, which takes its riders along.
        val inPlace = tile == anchor && direction == furni.direction
        when {
            !inPlace -> moves.move(firing, furni, tile, direction, z = z, turnSlides = true)
            z != null -> moves.lift(firing, furni, z)
        }
        if (matchPosition && z != null && furni.anchor == tile && furni.z != z) moves.lift(firing, furni, z)
    }

    private fun wall(
        firing: Firing,
        item: RoomFurni,
        saved: FurniSnapshot,
    ) {
        val room = firing.scope.room
        val place = saved.wall?.let(WallPlace::parse) ?: return
        val now = WallPlace.of(item) ?: return
        val altitude = if (matchAltitude) place.altitude(room) else now.altitude(room)
        val right = if (matchDirection) place.right else now.right
        val reachable = room.floor.contains(place.tile)
        val moved =
            matchPosition && reachable && (now.tile != place.tile || now.localX != place.localX || now.right != right)
        val sideOnly = moved && now.tile == place.tile && now.localX == place.localX
        when {
            sideOnly -> walls.changeSide(firing, item, place.copy(right = right, altitude = altitude))
            moved -> walls.placeAt(firing, item, place.copy(right = right, altitude = altitude))
            matchDirection && now.right != place.right -> walls.turn(firing, item)
        }
        if (matchAltitude && !moved) walls.lift(firing, item, altitude)
    }

    companion object {
        const val STATE = 0
        const val DIRECTION = 1
        const val POSITION = 2
        const val ALTITUDE = 3
        private const val ON = 1
        private const val COLOUR = ';'

        /** Whether [furni] has the state a snapshot keeps; an area hide's is only whether it is on. */
        fun sameState(
            furni: RoomFurni,
            saved: String,
        ): Boolean = if (furni.behaviour is AreaHide) AreaHide.matches(furni, saved) else furni.state == saved

        fun snapshots(
            room: Room,
            picks: List<Int>,
        ): List<FurniSnapshot> =
            picks.mapNotNull { pick ->
                room.furni[kotlin.math.abs(pick)]?.let { furni ->
                    val tile = furni.anchor ?: WallPlace.of(furni)?.tile ?: Point(0, 0)
                    FurniSnapshot(furni.id, furni.state, furni.direction, tile.x, tile.y, furni.z, furni.wallPosition)
                }
            }
    }
}
