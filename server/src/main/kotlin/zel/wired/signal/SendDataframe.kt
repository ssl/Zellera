package zel.wired.signal

import zel.core.rules.rules
import zel.room.furni.place.Interactions
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class SendDataframe(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val messages: LinkMessages,
    private val variables: FrameVariables = FrameVariables.None,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    private val rows: List<Pair<String, String>> =
        FrameText
            .pairs(settings.text)
            .take((settings.ints.firstOrNull() ?: 0).coerceIn(0, MAX_ROWS))

    override fun run(firing: Firing) {
        val hops = (firing.signal?.hops ?: -1) + 1
        if (hops >= rules.limits.wiredSignalHops) return
        val values = rows.mapIndexedNotNull { index, (name, message) -> row(firing, index, name, message) }
        Targets
            .furni(firing, this)
            .filter { Interactions.of(it.definition) == LinkMessages.LINK }
            .forEach { messages.send(firing, it.id, LinkMail.Frame(values, hops)) }
    }

    private fun row(
        firing: Firing,
        index: Int,
        name: String,
        message: String,
    ): FrameRow? {
        val first = 1 + index * INTS_PER_ROW
        val number = settings.ints.getOrNull(first + 1)?.toLong() ?: 0L
        return when (settings.ints.getOrNull(first)) {
            TYPE_NUMBER -> {
                FrameRow(name, number.toString(), number)
            }

            TYPE_MESSAGE -> {
                FrameRow(name, message, message.toLongOrNull())
            }

            TYPE_VARIABLE -> {
                variables.read(firing, this, index, settings.intAtRow(first))?.let { (_, value) ->
                    FrameRow(name, value.toString(), value)
                }
            }

            TYPE_VARIABLE_TEXT -> {
                variables.read(firing, this, index, settings.intAtRow(first))?.let { (text, value) ->
                    FrameRow(name, text, value)
                }
            }

            else -> {
                null
            }
        }
    }

    private fun BoxSettings.intAtRow(first: Int): Int = ints.getOrNull(first + TARGET_OFFSET) ?: 0

    companion object {
        const val MAX_ROWS = 12
        const val INTS_PER_ROW = 3
        private const val TARGET_OFFSET = 2
        private const val TYPE_NUMBER = 0
        private const val TYPE_VARIABLE = 1
        private const val TYPE_VARIABLE_TEXT = 2
        private const val TYPE_MESSAGE = 3
    }
}
