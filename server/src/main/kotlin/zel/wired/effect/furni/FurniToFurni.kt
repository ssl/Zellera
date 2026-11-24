package zel.wired.effect.furni

import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.box.WiredBox
import zel.wired.firing.Firing
import zel.wired.firing.FurniSource
import zel.wired.firing.Targets
import kotlin.random.Random

class FurniToFurni(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val moves: FurniMoves,
    private val walls: WallMoves,
    private val random: Random = Random.Default,
) : EffectBox,
    TriesMoves {
    override val negative = false
    override val needsUser = false

    private val resolved: WiredBox =
        object : WiredBox {
            override val furniId = this@FurniToFurni.furniId
            override val settings = this@FurniToFurni.settings.copy(furniSources = sources())
        }

    override fun tryMoves(
        firing: Firing,
        trial: MoveTrial,
    ) {
        val moving = Targets.furni(firing, resolved, slot = 0)
        val destinations = Targets.furni(firing, resolved, slot = 1)
        if (destinations.isEmpty()) return
        moving.forEachIndexed { n, furni ->
            val destination = destinations[n % destinations.size]
            if (destination !== furni && !furni.isWall) tileOf(destination)?.let { trial.move(furni, it) }
        }
    }

    /**
     * Each mover draws a random destination, whose tile is taken before any mover leaves: a mover drawn to another
     * mover goes to that one's old tile. tryMoves keeps the n mod count pairing.
     */
    override fun run(firing: Firing) {
        val moving = Targets.furni(firing, resolved, slot = 0)
        val destinations = Targets.furni(firing, resolved, slot = 1)
        if (destinations.isEmpty()) return
        val plan =
            moving.mapNotNull { furni ->
                val destination = destinations[random.nextInt(destinations.size)]
                if (destination === furni) null else tileOf(destination)?.let { furni to it }
            }
        plan.forEach { (furni, tile) -> send(firing, furni, tile) }
    }

    private fun send(
        firing: Firing,
        furni: RoomFurni,
        tile: Point,
    ) {
        if (furni.isWall) walls.moveTo(firing, furni, tile) else moves.move(firing, furni, tile)
    }

    private fun sources(): List<Int> {
        val first = settings.furniSources.getOrNull(0) ?: FurniSource.TRIGGERING
        val second = settings.furniSources.getOrNull(1) ?: FurniSource.PICKED
        val bothPicked = settings.picks.isNotEmpty() && settings.picks2.isNotEmpty()
        val moving = if (first == FurniSource.TRIGGERING && bothPicked) FurniSource.PICKED else first
        // the first pick list belongs to the target when the movers come from elsewhere
        val listTaken = moving == FurniSource.PICKED
        val target = if (second == FurniSource.PICKED && listTaken) FurniSource.PICKED_SECOND else second
        return listOf(moving, target)
    }

    private fun tileOf(furni: RoomFurni): Point? = furni.anchor ?: WallPlace.of(furni)?.tile
}
