package zel.wired.addon

import zel.protocol.generated.ChatOut
import zel.protocol.generated.ChatOutCodec
import zel.protocol.generated.ObjectsDataUpdateOut
import zel.protocol.generated.ObjectsDataUpdateOutCodec
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Interactions
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.tick.RoomTiming
import zel.room.unit.UnitKind
import zel.wired.BoxSettingsSource
import zel.wired.signal.FrameRow
import zel.wired.signal.LinkMail
import java.util.Collections
import java.util.WeakHashMap

class LinkSpeakers(
    private val forms: BoxSettingsSource,
) {
    private val lastFlip = Collections.synchronizedMap(WeakHashMap<RoomFurni, Long>())

    fun heard(
        scope: RoomScope,
        linkId: Int,
        mail: LinkMail,
    ) {
        val room = scope.room
        room.furni
            .all()
            .filter { Interactions.of(it.definition) == LinkSpeaker.CODE }
            .forEach { furni ->
                val speaker = LinkSpeaker(furni.id, forms.settings(room, furni) ?: return@forEach)
                val text = text(speaker, linkId, mail)
                if (text != null) say(scope, furni, text)
            }
    }

    private fun text(
        speaker: LinkSpeaker,
        linkId: Int,
        mail: LinkMail,
    ): String? =
        when {
            !speaker.listensTo(linkId) -> {
                null
            }

            mail is LinkMail.Message && speaker.saysMessages -> {
                mail.text.takeIf { it.isNotEmpty() }
            }

            mail is LinkMail.Frame && speaker.saysFrames -> {
                mail.rows.joinToString("\n", transform = ::line).takeIf {
                    it.isNotEmpty()
                }
            }

            else -> {
                null
            }
        }

    private fun line(row: FrameRow): String =
        "[b]${row.name}[/b]: ${row.text.ifEmpty { row.number?.toString().orEmpty() }.take(MAX_ROW_TEXT)}"

    private fun say(
        scope: RoomScope,
        speaker: RoomFurni,
        text: String,
    ) {
        val at = speaker.anchor ?: return
        if (scope.now() - (lastFlip[speaker] ?: Long.MIN_VALUE / 2) >= RoomTiming.TICK_MILLIS) {
            lastFlip[speaker] = scope.now()
            speaker.state = if (speaker.state == ON) OFF else ON
            val flip =
                ObjectsDataUpdateOut(
                    listOf(ObjectsDataUpdateOut.List1Item(speaker.id, FurniWire.stuff(speaker))),
                )
            scope.out.send(Lane.Now, Audience.Room, ObjectsDataUpdateOutCodec, flip)
        }
        scope.room.units
            .all()
            .forEach { unit ->
                val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return@forEach
                val bubble =
                    ChatOut(
                        NO_SPEAKER,
                        text,
                        0,
                        STYLE,
                        emptyList(),
                        NO_TRACKING,
                        unit.index,
                        NO_WIDTH,
                        true,
                        ChatOut.Trailing4(at.x, at.y),
                    )
                scope.out.send(Lane.Now, Audience.Player(playerId), ChatOutCodec, bubble)
            }
    }

    private companion object {
        const val NO_SPEAKER = -1
        const val NO_TRACKING = -1
        const val NO_WIDTH = -1
        const val STYLE = 253

        /** A frame is one bubble of up to twelve lines; a row keeps a line's worth of it. */
        const val MAX_ROW_TEXT = 100

        const val ON = "1"
        const val OFF = "0"
    }
}
