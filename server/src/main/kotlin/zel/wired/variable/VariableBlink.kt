package zel.wired.variable

import zel.room.Room
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.wired.firing.BoxWrite

/**
 * A written variable's box blinks in the writing firing's flash: its state goes to 2 x kind, or 2 x kind + 1 when it
 * already shows 2 x kind, and the var add-ons on its tile flip with every write. A read does not blink; a write outside
 * a firing has no flash to go in.
 */
object VariableBlink {
    const val UNCHANGED = 0
    const val UP = 1
    const val DOWN = 2
    const val GIVEN = 3
    const val REMOVED = 4

    private const val LEVEL_UP = "wf_xtra_var_lvlup_system"
    private const val TIME_UTIL = "wf_xtra_var_time_util"
    private const val FX_PREFIX = "wf_xtra_varfx_"
    private const val TEXT_CONNECTOR = "wf_xtra_var_text_connector"
    private const val LEADERBOARD = "wf_xtra_var_leaderboard"
    private const val SHARE = "wf_xtra_var_share"

    fun kindOf(change: VariableChange): Int =
        when (change.type) {
            ChangeType.CREATED -> GIVEN
            ChangeType.DELETED -> REMOVED
            else -> moved(change.oldValue, change.newValue)
        }

    fun moved(
        old: Long?,
        new: Long?,
    ): Int =
        when {
            old == null || new == null || old == new -> UNCHANGED
            new > old -> UP
            else -> DOWN
        }

    fun flash(
        on: VariableOn,
        boxId: Int,
        kind: Int,
        old: Long? = null,
        new: Long? = null,
    ) {
        val firing = on.firing ?: return
        val room = on.scope.room
        val box = room.furni[boxId] ?: return
        firing.flash.written(boxId, BoxWrite(kind, old, new))
        addonsBeside(room, box).forEach { firing.flash.blink(it.id) }
    }

    /** The kind several writes of one firing show: a give, else a remove, else where the value went in all. */
    fun net(writes: List<BoxWrite>): Int =
        when {
            writes.any { it.kind == GIVEN } -> GIVEN
            writes.any { it.kind == REMOVED } -> REMOVED
            writes.size == 1 -> writes.single().kind
            else -> moved(writes.first().old, writes.last().new)
        }

    /** The state a box showing [state] goes to for one write of [kind]. */
    fun written(
        state: String,
        kind: Int,
    ): String {
        val base = 2 * kind
        return if (state == base.toString()) (base + 1).toString() else base.toString()
    }

    private fun addonsBeside(
        room: Room,
        box: RoomFurni,
    ): List<RoomFurni> {
        val tile = box.anchor ?: return emptyList()
        return room.furni
            .at(tile)
            .furni
            .filter { it.id != box.id && blinksWith(Interactions.of(it.definition)) }
    }

    private fun blinksWith(code: String): Boolean =
        code == LEVEL_UP || code == TEXT_CONNECTOR || code == LEADERBOARD || code == TIME_UTIL || code == SHARE ||
            code.startsWith(FX_PREFIX)
}
