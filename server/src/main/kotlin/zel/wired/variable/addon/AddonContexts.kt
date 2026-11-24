package zel.wired.variable.addon

import zel.protocol.generated.PacketWriter
import zel.room.Room
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.wired.box.BoxSettings
import zel.wired.form.DialogContexts
import zel.wired.form.DialogSpec
import zel.wired.variable.RoomVariables
import zel.wired.variable.VariableWire

class AddonContexts(
    private val variables: () -> RoomVariables,
    private val webApi: WebApiBoxes,
    private val next: DialogContexts,
) : DialogContexts {
    override fun write(
        room: Room,
        box: RoomFurni,
        spec: DialogSpec,
        out: PacketWriter,
    ): Int =
        spec.contexts.count { type ->
            if (type == TILE_VARIABLES) {
                val entries =
                    box.anchor
                        ?.let { room.furni.at(it).furni }
                        .orEmpty()
                        .filter { Interactions.of(it.definition).startsWith(RoomVariables.BOX_PREFIX) }
                        .sortedWith(compareBy({ it.z }, { it.id }))
                        .mapNotNull { variables().definition(room, it)?.entry }
                out.int(type).int(entries.size)
                entries.forEach { VariableWire.block(out, it) }
                true
            } else {
                next.write(room, box, spec.copy(contexts = listOf(type)), out) > 0
            }
        }

    override fun shownVariableIds(
        room: Room,
        box: RoomFurni,
        settings: BoxSettings,
    ): List<String> = next.shownVariableIds(room, box, settings)

    override fun shownText(
        room: Room,
        box: RoomFurni,
        settings: BoxSettings,
    ): String =
        if (Interactions.of(box.definition) == AddonCodes.WEB_API) {
            webApi.opened(box)
        } else {
            next.shownText(room, box, settings)
        }

    private companion object {
        const val TILE_VARIABLES = 5
    }
}
