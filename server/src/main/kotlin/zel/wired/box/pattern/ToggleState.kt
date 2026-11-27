package zel.wired.box.pattern

import zel.protocol.generated.HeightMapUpdateOutCodec
import zel.protocol.generated.ItemsStateUpdateOut
import zel.protocol.generated.ItemsStateUpdateOutCodec
import zel.room.furni.HeightMapChange
import zel.room.furni.RoomFurni
import zel.room.furni.StoredValues
import zel.room.furni.Toggleable
import zel.room.furni.place.Interactions
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.wired.box.BoxCodes
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class ToggleState(
    override val furniId: Int,
    override val settings: BoxSettings,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val back = settings.ints.firstOrNull() == PREVIOUS
        Targets.furni(firing, this).filter(Companion::toggles).forEach { furni ->
            if (!changesHeights(firing, furni) { toggle(firing, furni, back) }) return@forEach
            if (furni.isWall) {
                val update =
                    ItemsStateUpdateOut(listOf(ItemsStateUpdateOut.List1Item(furni.id, furni.state)))
                firing.flash.later { it.out.send(Lane.Now, Audience.Room, ItemsStateUpdateOutCodec, update) }
            } else {
                firing.flash.changed(furni.id)
            }
        }
    }

    private fun toggle(
        firing: Firing,
        furni: RoomFurni,
        back: Boolean,
    ): Boolean {
        val own = furni.behaviour as? Toggleable
        if (own != null) return own.toggle(firing.scope, furni, back)
        val count = furni.definition.stateCount
        if (count <= 1) return false
        val current = (furni.state.toIntOrNull() ?: 0).mod(count)
        furni.state = (if (back) current - 1 else current + 1).mod(count).toString()
        furni.dirty = true
        return true
    }

    companion object {
        const val PREVIOUS = 1

        fun changesHeights(
            firing: Firing,
            furni: RoomFurni,
            change: () -> Boolean,
        ): Boolean {
            val heights = HeightMapChange.of(firing.scope.room, furni.tiles())
            val changed = change()
            if (changed) {
                heights.updates().forEach {
                    firing.scope.out.send(Lane.NextTick, Audience.Room, HeightMapUpdateOutCodec, it)
                }
            }
            return changed
        }

        /** Toggle, toggle-to-random, match-to-snapshot and the furni-state variable all write through this. */
        fun toggles(furni: RoomFurni): Boolean {
            val type = Interactions.of(furni.definition)
            return !BoxCodes.isBox(type) &&
                !StoredValues.holds(type) &&
                type !in NEVER &&
                NEVER_PREFIXES.none(type::startsWith)
        }

        /** Look states wired leaves to the furni's own use: walkers, rides, games, pets and the like. */
        private val NEVER =
            setOf(
                "wf_link",
                "chance.dice",
                "transit.door",
                "transit.tile",
                "roller.belt",
                "state.switch",
                "plate.pressure",
                "plate.pressure.group",
                "gate.oneway",
                "puzzle.box",
                "area.tent",
                "water.area",
                "effect.gym",
                "transit.hopper",
                "vending.plain",
                "vending.any_side",
                "outfit.clothing",
                "counter.hockey",
                "effect.trap",
            )

        private val NEVER_PREFIXES =
            listOf(
                "game.tileclaim.",
                "game.freeze.",
                "game.football.goal.",
                "game.football.score.",
                "game.tag.",
                "pet.",
            )
    }
}
