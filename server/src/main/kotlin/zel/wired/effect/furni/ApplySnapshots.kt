package zel.wired.effect.furni

import zel.room.RoomScope
import zel.wired.WiredRooms
import zel.wired.box.BoxSettings
import zel.wired.firing.Firing
import zel.wired.firing.FiringStart
import zel.wired.firing.Selection
import zel.wired.store.RoomBoxForms
import zel.wired.variable.HeldScope

class ApplySnapshots(
    private val forms: RoomBoxForms,
    private val wired: () -> WiredRooms?,
    private val match: (Int, BoxSettings) -> MatchToSnapshot,
) {
    fun apply(
        scope: RoomScope,
        level: Int,
        boxId: Int,
    ) {
        if (!forms.masks(scope.room).mayModify(level)) return
        val furni = scope.room.furni[boxId] ?: return
        val settings = forms.settings(scope.room, furni) ?: return
        if (settings.snapshots.isEmpty()) return
        val rooms = wired() ?: return
        val held = HeldScope(scope)
        rooms.within(held) { flash, chain ->
            val firing = Firing(held, null, FiringStart(null, null), Selection(), chain, flash, emptyList())
            match(boxId, settings).applyAll(firing)
        }
        held.flush()
    }
}
