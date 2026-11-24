package zel.wired.effect.furni

import zel.room.furni.RoomFurni
import zel.wired.Hundredths
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class SetAltitude(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val moves: FurniMoves,
    private val walls: WallMoves,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val amount = (settings.ints.getOrNull(AMOUNT) ?: 0).toLong()
        val operator = settings.ints.getOrNull(OPERATOR) ?: RAISE
        Targets.furni(firing, this).forEach { furni ->
            if (furni.isWall) wall(firing, furni, amount, operator) else floor(firing, furni, amount, operator)
        }
    }

    private fun floor(
        firing: Firing,
        furni: RoomFurni,
        amount: Long,
        operator: Int,
    ) {
        val z = applied(Hundredths.of(furni.z), amount, operator).coerceIn(0, Hundredths.of(MAX_Z))
        moves.lift(firing, furni, Hundredths.toTiles(z))
    }

    private fun wall(
        firing: Firing,
        item: RoomFurni,
        amount: Long,
        operator: Int,
    ) {
        val current = WallPlace.of(item)?.altitude(firing.scope.room) ?: return
        val altitude = applied(current.toLong(), amount, operator).coerceIn(0, Int.MAX_VALUE.toLong())
        walls.lift(firing, item, altitude.toInt())
    }

    private fun applied(
        current: Long,
        amount: Long,
        operator: Int,
    ): Long =
        when (operator) {
            RAISE -> current + amount
            LOWER -> current - amount
            SET -> amount
            else -> current
        }

    companion object {
        const val INVALID_ALTITUDE = "The height can't be below zero."
        const val RAISE = 0
        const val LOWER = 1
        const val SET = 2
        private const val AMOUNT = 0
        private const val OPERATOR = 1
        private const val MAX_Z = 79.0
    }
}
