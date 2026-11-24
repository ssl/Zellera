package zel.wired.effect.user

import zel.room.event.RoomEvent
import zel.room.unit.RoomUnit
import zel.wired.addon.Placeholders
import zel.wired.firing.Firing

class TextPlaceholders(
    private val extra: PlaceholderValues = PlaceholderValues.None,
) {
    /** Each value and the filled text keep at most [Placeholders.MAX_TEXT] characters; a text without any is kept. */
    fun fill(
        firing: Firing,
        receiver: RoomUnit?,
        text: String,
    ): String {
        if (!text.contains(OPEN)) return text
        val values = values(firing) + extra.of(firing, receiver)
        return Placeholders.replace(text, PATTERN, values.mapValues { Placeholders.cap(it.value) })
    }

    private fun values(firing: Firing): Map<String, String> {
        val said = (firing.event as? RoomEvent.Says)?.text
        val signal = firing.signal
        val trigger = said ?: signal?.text?.takeIf { it.isNotEmpty() }
        val own = signal?.placeholders.orEmpty().mapKeys { it.key.lowercase() }
        return if (trigger == null) own else own + (TRIGGER_TEXT to trigger)
    }

    private companion object {
        const val OPEN = "$("
        const val TRIGGER_TEXT = "trigger_text"
        val PATTERN = Regex("""\$\(([^()]+)\)""")
    }
}
