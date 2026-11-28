package zel.room.chat

import zel.protocol.generated.ExpressionOut
import zel.protocol.generated.ExpressionOutCodec
import zel.protocol.generated.PlayerTypingOut
import zel.protocol.generated.PlayerTypingOutCodec
import zel.protocol.generated.SpecialSystemChatOut
import zel.protocol.generated.SpecialSystemChatOutCodec
import zel.room.RoomScope
import zel.room.event.PlayerAction
import zel.room.event.RoomEvent
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.UnitStatus
import java.util.Collections
import java.util.WeakHashMap

class Gestures(
    private val dances: Dances,
    private val active: (RoomScope, RoomUnit) -> Unit,
    private val showNow: (RoomScope, RoomUnit) -> Unit,
    private val sleep: (RoomScope, RoomUnit) -> Unit = { _, _ -> },
    private val clubMember: (Int) -> Boolean = { false },
    private val lift: (RoomScope, RoomUnit) -> Unit = { _, _ -> },
) {
    private val waves: MutableMap<RoomUnit, ArrayDeque<Long>> = Collections.synchronizedMap(WeakHashMap())

    fun dance(
        scope: RoomScope,
        unit: RoomUnit,
        id: Int,
    ) {
        if (id !in 0..LAST_DANCE) return
        if (id >= FIRST_CLUB_DANCE && !isClubMember(unit)) return
        active(scope, unit)
        dances.dance(scope, unit, id)
        if (id > 0) scope.raise(RoomEvent.Action(unit.index, PlayerAction.DANCE, id.toString()))
    }

    fun expression(
        scope: RoomScope,
        unit: RoomUnit,
        id: Int,
    ) {
        if (id <= 0) return
        if (id in CLUB_EXPRESSIONS && !isClubMember(unit)) return
        if (id != IDLE) active(scope, unit)
        if (id == SIXTY_SEVEN) dances.stop(scope, unit)
        scope.out.send(Lane.Now, Audience.Room, ExpressionOutCodec, ExpressionOut(unit.index, id))
        if (id == IDLE) sleep(scope, unit)
        if (id == SIXTY_SEVEN) {
            val line = SpecialSystemChatOut(unit.index, id)
            scope.out.send(Lane.Now, Audience.Room, SpecialSystemChatOutCodec, line)
        }
        ACTIONS[id]?.let { scope.raise(RoomEvent.Action(unit.index, it, "")) }
        if (id == WAVE) waved(scope, unit)
    }

    private fun waved(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        val now = scope.now()
        val seen = waves.getOrPut(unit) { ArrayDeque() }
        seen.addLast(now)
        while (seen.isNotEmpty() && seen.first() < now - WAVES_MILLIS) seen.removeFirst()
        if (seen.size < WAVES) return
        seen.clear()
        lift(scope, unit)
    }

    fun sign(
        scope: RoomScope,
        unit: RoomUnit,
        id: Int,
    ) {
        if (id < 0) return
        active(scope, unit)
        unit.setStatus(UnitStatus.SIGN, id.toString())
        showNow(scope, unit)
        unit.statuses.remove(UnitStatus.SIGN)
        scope.raise(RoomEvent.Action(unit.index, PlayerAction.SIGN, id.toString()))
    }

    fun typing(
        scope: RoomScope,
        unit: RoomUnit,
        typing: Boolean,
    ) {
        val message = PlayerTypingOut(unit.index, if (typing) 1 else 0)
        scope.out.send(Lane.Now, Audience.Room, PlayerTypingOutCodec, message)
    }

    private fun isClubMember(unit: RoomUnit): Boolean {
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return false
        return clubMember(playerId)
    }

    private companion object {
        const val LAST_DANCE = 4
        const val FIRST_CLUB_DANCE = 2
        val CLUB_EXPRESSIONS = setOf(2, 3)
        const val IDLE = 5
        private const val WAVE = 1
        private const val WAVES = 3
        private const val WAVES_MILLIS = 4_000L
        const val SIXTY_SEVEN = 67

        val ACTIONS =
            mapOf(
                1 to PlayerAction.WAVE,
                2 to PlayerAction.BLOW_KISS,
                3 to PlayerAction.LAUGH,
                7 to PlayerAction.THUMBS_UP,
                IDLE to PlayerAction.IDLE,
                SIXTY_SEVEN to PlayerAction.SIXTY_SEVEN,
            )
    }
}
