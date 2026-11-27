package zel.room.unit.pet

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.behaviour.state.StateShow
import zel.room.unit.RoomUnit
import zel.room.unit.path.ObstacleBar
import java.util.concurrent.ConcurrentHashMap

class HorseJumps(
    private val pets: RoomPets,
    private val counted: (scope: RoomScope, rider: RoomUnit, streak: Int) -> Unit,
) {
    class Jump(
        val obstacleId: Int,
        val state: Int,
        val clean: Boolean,
        val startAt: Long,
    ) {
        var started = false
        var rider: RoomUnit? = null

        /** The horse lands and the obstacle rests again at this time. */
        var endsAt = 0L
    }

    private val inTheAir: MutableSet<RoomUnit> = ConcurrentHashMap.newKeySet()

    /** True while [rider]'s horse is in the air. Any thread. */
    fun holds(rider: RoomUnit): Boolean = rider in inTheAir

    fun clean(
        scope: RoomScope,
        rider: RoomUnit,
        obstacle: RoomFurni,
    ) = start(scope, rider, Jump(obstacle.id, CLEAN_STATE, clean = true, scope.now() + CLEAN_DELAY_MILLIS))

    fun onBar(
        scope: RoomScope,
        rider: RoomUnit,
        obstacle: RoomFurni,
    ) {
        val state = pets.host.random.nextInt(ROUGH_STATES) + 1
        start(scope, rider, Jump(obstacle.id, state, clean = false, scope.now()))
    }

    private fun start(
        scope: RoomScope,
        rider: RoomUnit,
        jump: Jump,
    ) {
        val placed = pets.walking.horseOf(scope, rider)?.let { pets.byIndex(scope, it.index) } ?: return
        if (placed.jump?.started == true) return
        placed.jump = jump
    }

    fun tick(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ) {
        val jump = placed.jump ?: return
        val now = scope.now()
        val obstacle = scope.room.furni[jump.obstacleId]
        if (!jump.started && now >= jump.startAt) begin(scope, placed, unit, jump, obstacle)
        val inAir = jump.started && unit.statuses[PetLines.POSTURE] == JUMP
        if (inAir && landed(jump, unit, obstacle, now)) settle(jump, unit)
        if (jump.started && now >= jump.endsAt) {
            obstacle?.takeIf { it.state != RESTING }?.let { StateShow.data(scope, it, RESTING) }
            clear(placed)
        }
    }

    fun clear(placed: RoomPet) {
        placed.jump?.rider?.let { inTheAir -= it }
        placed.jump = null
    }

    private fun begin(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        jump: Jump,
        obstacle: RoomFurni?,
    ) {
        val now = scope.now()
        jump.started = true
        jump.endsAt = now + JUMP_MILLIS
        unit.setStatus(PetLines.POSTURE, JUMP)
        unit.changed = true
        obstacle?.let { StateShow.data(scope, it, jump.state.toString()) }
        val rider = placed.rider?.let { scope.room.units[it] } ?: return
        jump.rider = rider
        inTheAir += rider
        if (jump.clean) {
            placed.jumpStreak += 1
            counted(scope, rider, placed.jumpStreak)
        } else {
            placed.jumpStreak = 0
        }
    }

    private fun landed(
        jump: Jump,
        unit: RoomUnit,
        obstacle: RoomFurni?,
        now: Long,
    ): Boolean {
        val offBar = obstacle == null || unit.tile !in ObstacleBar.middle(obstacle)
        return now >= jump.endsAt || (offBar && now >= jump.endsAt - JUMP_MILLIS + SETTLE_MILLIS)
    }

    private fun settle(
        jump: Jump,
        unit: RoomUnit,
    ) {
        unit.dropStatus(PetLines.POSTURE)
        unit.changed = true
        jump.rider?.let { inTheAir -= it }
    }

    private companion object {
        const val JUMP = "jmp 0"
        const val RESTING = "0"
        const val CLEAN_STATE = 4
        const val ROUGH_STATES = 3
        const val CLEAN_DELAY_MILLIS = 250L
        const val JUMP_MILLIS = 2000L
        const val SETTLE_MILLIS = 600L
    }
}
