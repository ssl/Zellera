package zel.wired.variable

import zel.protocol.generated.PacketWriter
import zel.room.Room
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.wired.box.BoxSettings
import zel.wired.form.DialogContexts
import zel.wired.form.DialogSpec

class VariableContexts(
    private val variables: RoomVariables,
    private val next: DialogContexts,
) : DialogContexts {
    override fun write(
        room: Room,
        box: RoomFurni,
        spec: DialogSpec,
        out: PacketWriter,
    ): Int =
        spec.contexts.count { type ->
            when (type) {
                USER_HOLDERS, FURNI_HOLDERS -> holders(room, box, spec, type, out)
                GLOBAL_VALUE -> global(room, box, spec, out)
                REFERENCE_OFFERS -> offers(room, box, out)
                else -> next.write(room, box, spec.copy(contexts = listOf(type)), out) > 0
            }
        }

    override fun shownVariableIds(
        room: Room,
        box: RoomFurni,
        settings: BoxSettings,
    ): List<String> {
        val code = Interactions.of(box.definition)
        return if (code in OWN_ID_BOXES && settings.text.isNotEmpty()) {
            variables.definition(room, box)?.let { listOf(it.id) } ?: settings.variableIds
        } else {
            settings.variableIds
        }
    }

    override fun shownText(
        room: Room,
        box: RoomFurni,
        settings: BoxSettings,
    ): String = next.shownText(room, box, settings)

    private fun holders(
        room: Room,
        box: RoomFurni,
        spec: DialogSpec,
        type: Int,
        out: PacketWriter,
    ): Boolean {
        val variable = variables.definition(room, box)
        val entry = variable?.entry ?: blank(box, spec, type)
        out.int(type)
        VariableWire.block(out, entry)
        val values =
            if (variable == null) {
                emptyList()
            } else {
                variables.allHolders(room, variable).entries.mapNotNull { (holder, held) ->
                    holder.entityIn(room)?.let { it to VariableWire.shown(entry, held.value) }
                }
            }
        out.int(values.size)
        values.forEach { (id, value) -> out.int(id).int(value) }
        return true
    }

    private fun global(
        room: Room,
        box: RoomFurni,
        spec: DialogSpec,
        out: PacketWriter,
    ): Boolean {
        val variable = variables.definition(room, box)
        val entry = variable?.entry ?: blank(box, spec, GLOBAL_VALUE)
        val value =
            if (entry.availability == Availability.SHARED || variable == null) {
                0
            } else {
                variables.allHolders(room, variable)[Holder.Room]?.value ?: 0
            }
        out.int(GLOBAL_VALUE)
        VariableWire.block(out, entry)
        out.int(VariableWire.shown(value))
        return true
    }

    private fun blank(
        box: RoomFurni,
        spec: DialogSpec,
        type: Int,
    ): VariableEntry {
        val settings = spec.form.fresh
        return when (type) {
            FURNI_HOLDERS -> {
                entry(
                    "${RoomVariables.FURNI_PREFIX}:${box.id}",
                    settings.ints.getOrElse(1) { Availability.WHILE_ROOM_ACTIVE },
                    VariableTarget.FURNI,
                    RoomVariables.HELD_FLAG,
                )
            }

            GLOBAL_VALUE -> {
                entry(
                    "${RoomVariables.ROOM_PREFIX}:${box.id}",
                    settings.ints.getOrElse(0) { Availability.WHILE_ROOM_ACTIVE },
                    VariableTarget.GLOBAL,
                    RoomVariables.GLOBAL_FLAGS,
                )
            }

            else -> {
                entry(
                    "${RoomVariables.USER_PREFIX}:${box.id}",
                    settings.ints.getOrElse(0) { Availability.WHILE_IN_ROOM },
                    VariableTarget.USER,
                    RoomVariables.HELD_FLAG,
                )
            }
        }
    }

    private fun entry(
        id: String,
        availability: Int,
        target: Int,
        bits: String,
    ) = VariableEntry(id, 0, "", availability, target, VariableFlags.of(bits))

    private fun offers(
        room: Room,
        box: RoomFurni,
        out: PacketWriter,
    ): Boolean {
        val held = heldByOthers(room, box)
        val offers = variables.sharedOffers(room).filterNot { SharedToken(it.target, it.roomId, it.boxId).text in held }
        out.int(REFERENCE_OFFERS).int(offers.size)
        offers.forEach { offer ->
            out.int(offer.roomId).string(offer.roomName)
            val token = SharedToken(offer.target, offer.roomId, offer.boxId)
            val flags = VariableFlags.of(OFFER_FLAGS)
            val entry = VariableEntry(token.text, OFFER_TYPE, offer.name, Availability.SHARED, offer.target, flags)
            VariableWire.block(out, entry)
        }
        return true
    }

    private fun heldByOthers(
        room: Room,
        box: RoomFurni,
    ): Set<String> = variables.referencedTokens(room) - setOfNotNull(variables.tokenOf(room, box))

    private companion object {
        const val FURNI_HOLDERS = 1
        const val USER_HOLDERS = 2
        const val GLOBAL_VALUE = 3
        const val REFERENCE_OFFERS = 4
        const val OFFER_TYPE = 2
        const val OFFER_FLAGS = "111100011"
        val OWN_ID_BOXES =
            setOf(
                RoomVariables.USER_BOX,
                RoomVariables.FURNI_BOX,
                RoomVariables.ROOM_BOX,
                RoomVariables.CONTEXT_BOX,
            )
    }
}
