package zel.wired.form

import zel.protocol.generated.PacketWriter
import zel.room.Room
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.room.map.Point
import zel.wired.box.BoxSettings

fun interface DialogContexts {
    fun write(
        room: Room,
        box: RoomFurni,
        spec: DialogSpec,
        out: PacketWriter,
    ): Int

    fun shownVariableIds(
        room: Room,
        box: RoomFurni,
        settings: BoxSettings,
    ): List<String> = settings.variableIds

    fun shownText(
        room: Room,
        box: RoomFurni,
        settings: BoxSettings,
    ): String = settings.text

    class Plain(
        private val variableHash: (Room) -> Int = { 0 },
        private val outputNames: (Room, Point) -> List<String> = { _, _ -> emptyList() },
    ) : DialogContexts {
        override fun write(
            room: Room,
            box: RoomFurni,
            spec: DialogSpec,
            out: PacketWriter,
        ): Int =
            spec.contexts.count { type ->
                when (type) {
                    VARIABLE_HASH -> {
                        out.int(type).int(variableHash(room))
                        true
                    }

                    TEXT_PLACEHOLDERS -> {
                        val names = placeholders(room, box)
                        out.int(type).int(names.size)
                        names.forEach { out.string(it) }
                        out.int(0)
                        true
                    }

                    else -> {
                        false
                    }
                }
            }

        private fun placeholders(
            room: Room,
            box: RoomFurni,
        ): List<String> {
            val tile = box.anchor ?: return emptyList()
            val says = room.furni.all().any { it.anchor == tile && Interactions.of(it.definition) == SAYS_TRIGGER }
            return outputNames(room, tile) + if (says) listOf(TRIGGER_TEXT) else emptyList()
        }
    }

    companion object {
        const val VARIABLE_HASH = 0
        const val TEXT_PLACEHOLDERS = 7
        const val SAYS_TRIGGER = "wf_trg_says_something"
        const val TRIGGER_TEXT = "trigger_text"
    }
}
