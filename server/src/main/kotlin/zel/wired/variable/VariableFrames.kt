package zel.wired.variable

import zel.wired.box.WiredBox
import zel.wired.firing.Firing
import zel.wired.signal.FrameVariables

class VariableFrames(
    private val variables: () -> RoomVariables,
) : FrameVariables {
    override fun read(
        firing: Firing,
        box: WiredBox,
        row: Int,
        target: Int,
    ): Pair<String, Long>? {
        val rooms = variables()
        val value = rooms.readFor(firing, box, row, target, furniSlot = row + 1, userSlot = row) ?: return null
        val variable =
            rooms.find(
                firing.scope.room,
                box.settings.variableIds
                    .getOrNull(row)
                    .orEmpty(),
            ) ?: return null
        return variable.entry.textOf(value) to value
    }
}
