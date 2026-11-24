package zel.wired.effect.furni

import zel.protocol.generated.ItemsStateUpdateOut
import zel.protocol.generated.ItemsStateUpdateOutCodec
import zel.room.furni.RoomFurni
import zel.room.furni.Toggleable
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.box.pattern.ToggleState
import zel.wired.firing.Firing
import zel.wired.firing.Targets
import kotlin.random.Random

class ToggleToRandom(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val random: Random,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        Targets.furni(firing, this).filter(ToggleState::toggles).forEach { furni ->
            if (!ToggleState.changesHeights(firing, furni) { randomize(firing, furni) }) return@forEach
            if (furni.isWall) {
                val update =
                    ItemsStateUpdateOut(listOf(ItemsStateUpdateOut.List1Item(furni.id, furni.state)))
                firing.flash.later { it.out.send(Lane.Now, Audience.Room, ItemsStateUpdateOutCodec, update) }
            } else {
                firing.flash.changed(furni.id)
            }
        }
    }

    private fun randomize(
        firing: Firing,
        furni: RoomFurni,
    ): Boolean {
        val own = furni.behaviour as? Toggleable
        if (own != null) return own.toggleRandom(firing.scope, furni, random)
        val count = furni.definition.stateCount
        if (count < 2) return false
        val next = random.nextInt(count).toString()
        if (next == furni.state) return false
        furni.state = next
        furni.dirty = true
        return true
    }
}
