package zel.wired.trigger

import zel.room.event.RoomEvent
import zel.room.unit.UnitKind
import zel.wired.addon.Placeholders
import zel.wired.box.BoxSettings
import zel.wired.box.TriggerBox
import zel.wired.firing.Firing
import java.util.Locale

class SaysSomething(
    override val furniId: Int,
    override val settings: BoxSettings,
) : TriggerBox {
    private val ownerOnly = (settings.ints.getOrNull(OWNER_ONLY) ?: 0) != 0
    private val mode = settings.ints.getOrNull(MODE) ?: 0
    private val hides = (settings.ints.getOrNull(HIDE) ?: 0) != 0

    /** The text as written; one with a `$(name)` placeholder is filled per firing (see [wanted]). */
    private val written = normal(settings.text)
    private val filled = settings.text.contains("$(")

    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean {
        if (event !is RoomEvent.Says || (ownerOnly && !byOwner(event, firing))) return false
        val captures = firing.addons.filterIsInstance<LineCapture>().filter { it.capturesIn(settings.text) }
        if (captures.isEmpty()) return matchesLine(event.text, firing)
        return captures.all { it.capture(firing, settings.text, event.said, mode != MODE_WHOLE) }
    }

    override fun passed(firing: Firing) {
        if (hides) (firing.event as? RoomEvent.Says)?.hiding?.hide()
    }

    private fun matchesLine(
        line: String,
        firing: Firing,
    ): Boolean {
        if (mode == MODE_ANY) return true
        val wanted = wanted(firing)
        return when (mode) {
            MODE_WHOLE -> wanted.isNotEmpty() && spoken(line) == wanted
            else -> wanted.isNotEmpty() && spoken(line).contains(wanted)
        }
    }

    /**
     * The text to match: `$(name)` takes the stack's text output add-on's value at this firing, so the trigger fires
     * on the value and not on the literal.
     */
    private fun wanted(firing: Firing): String =
        if (filled) normal(Placeholders.fill(firing, settings.text)) else written

    private fun byOwner(
        event: RoomEvent.Says,
        firing: Firing,
    ): Boolean {
        val room = firing.scope.room
        return (room.units[event.unitIndex]?.kind as? UnitKind.Player)?.playerId == room.info.ownerId
    }

    private fun normal(text: String): String = spoken(text.trim())

    /**
     * The line as spoken (chat already dropped its trailing spaces), in lower case: leading spaces stay, so a
     * whole-line ' open 4711' is not 'open 4711'.
     */
    private fun spoken(line: String): String = line.lowercase(Locale.ROOT)

    companion object {
        const val MODE = 1
        private const val MODE_CONTAINS = 0
        const val MODE_ANY = 2
        private const val OWNER_ONLY = 0
        private const val HIDE = 2
        private const val MODE_WHOLE = 1

        fun inContainsMode(settings: BoxSettings): Boolean = (settings.ints.getOrNull(MODE) ?: 0) == MODE_CONTAINS
    }
}
