package zel.wired.score

import zel.protocol.generated.PacketWriter
import zel.room.Room
import zel.room.furni.RoomFurni
import zel.wired.box.BoxSettings
import zel.wired.form.DialogContexts
import zel.wired.form.DialogSpec
import zel.wired.variable.Availability
import zel.wired.variable.RoomVariables

class LeaderboardContexts(
    private val boards: Leaderboards,
    private val variables: () -> RoomVariables?,
    private val next: DialogContexts,
) : DialogContexts {
    override fun write(
        room: Room,
        box: RoomFurni,
        spec: DialogSpec,
        out: PacketWriter,
    ): Int =
        spec.contexts.count { type ->
            if (type == LEADERBOARD) {
                entry(room, box, out)
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

    private fun entry(
        room: Room,
        box: RoomFurni,
        out: PacketWriter,
    ) {
        val stacked = boards.stacked(room, box)
        val anchor = box.anchor
        out
            .int(LEADERBOARD)
            .int(boards.publishedId(room, box.id))
            .int(anchor?.x ?: 0)
            .int(anchor?.y ?: 0)
        val listed = stacked.take(LISTED)
        out.int(stacked.size).int(listed.size)
        listed.forEach { (id, name) ->
            val entry = variables()?.find(room, id)?.entry
            out.string(id).string(name)
            out.boolean(entry == null || stored(entry.availability))
            out.boolean(entry?.flags?.hasValue != false)
            out.int(0)
        }
    }

    /** A reference reads its source room's stored values, so it counts as stored. */
    private fun stored(availability: Int): Boolean =
        Availability.isStored(availability) || availability == Availability.REFERENCE

    private companion object {
        const val LEADERBOARD = 8
        const val LISTED = 10
    }
}
