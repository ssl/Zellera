package zel.wired.signal

import zel.room.event.RoomEvent
import zel.wired.box.BoxSettings
import zel.wired.firing.Firing
import zel.wired.firing.SignalPayload

class ReceiveDataframe(
    override val furniId: Int,
    override val settings: BoxSettings,
) : LinkTrigger {
    private val rows: List<Row> =
        FrameText
            .pairs(settings.text)
            .take((settings.ints.firstOrNull() ?: 0).coerceIn(0, SendDataframe.MAX_ROWS))
            .mapIndexed { index, (name, placeholder) -> Row(name, placeholder, variable(index)) }

    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean = false

    override fun payload(mail: LinkMail): SignalPayload? {
        val frame = mail as? LinkMail.Frame ?: return null
        val sent = frame.rows.associateBy { it.name }
        val found = rows.mapNotNull { row -> sent[row.name]?.let { row to it } }
        val (toVariables, toTexts) = found.partition { (row, _) -> row.variable != null }
        val context =
            toVariables.mapNotNull { (row, value) ->
                value.number?.let { number -> row.variable?.let { it to number } }
            }
        val texts = toTexts.map { (row, value) -> row.placeholder to value.text }
        return SignalPayload(emptyList(), emptyList(), context.toMap(), placeholders = texts.toMap(), hops = frame.hops)
    }

    private fun variable(index: Int): String? = settings.variableIds.getOrNull(index)?.takeIf { it != NO_VARIABLE }

    private class Row(
        val name: String,
        val placeholder: String,
        val variable: String?,
    )

    private companion object {
        const val NO_VARIABLE = "0"
    }
}
