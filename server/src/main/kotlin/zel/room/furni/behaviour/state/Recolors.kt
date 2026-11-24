package zel.room.furni.behaviour.state

import zel.game.items.RecolorablePalettes
import zel.protocol.generated.FurniRecolorUpdateOut
import zel.protocol.generated.FurniRecolorUpdateOutCodec
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.FurniOut
import zel.room.furni.place.PlaceError
import zel.room.tick.Audience
import zel.room.tick.Lane

class Recolors(
    private val part: StatePart,
    private val palettes: () -> RecolorablePalettes = RecolorablePalettes::get,
) {
    @Suppress("LongParameterList") // one parameter per field of the message
    fun recolor(
        scope: RoomScope,
        playerId: Int,
        furniId: Int,
        target: Int,
        red: Int,
        green: Int,
        blue: Int,
    ) {
        if (!part.hooks.hasRights(scope, playerId)) {
            FurniOut.error(scope, playerId, PlaceError.NotOwner)
            return
        }
        if (listOf(red, green, blue).any { it !in 0..MAX_CHANNEL } || target !in SCOPES) return
        val furni = scope.room.furni[furniId] ?: return
        val block = furni.behaviour as? Recolorable ?: return
        val rgb = (red shl RED) or (green shl GREEN) or blue
        if (!palettes().allows(furni.definition.name, rgb)) return
        val painted = targets(scope, furni, block, target).filter { palettes().allows(it.definition.name, rgb) }
        painted.forEach { StateShow.set(it, block.stored(block.state(it), rgb)) }
        part.hooks.saveNow(scope.room)
        scope.out.send(
            Lane.Now,
            Audience.Room,
            FurniRecolorUpdateOutCodec,
            FurniRecolorUpdateOut(
                listOf(
                    FurniRecolorUpdateOut.List1Item(
                        painted.map { it.id },
                        red.toShort(),
                        green.toShort(),
                        blue.toShort(),
                    ),
                ),
            ),
        )
    }

    private fun targets(
        scope: RoomScope,
        furni: RoomFurni,
        block: Recolorable,
        target: Int,
    ): List<RoomFurni> {
        if (target == THIS) return listOf(furni)
        val color = block.color(furni)
        return scope.room.furni.all().filter {
            it.behaviour is Recolorable &&
                (target == SAME_COLOR || it.definition.id == furni.definition.id) &&
                (target == SAME_TYPE || block.color(it) == color)
        }
    }

    private companion object {
        const val THIS = 0
        const val SAME_COLOR = 1
        const val SAME_TYPE = 2
        val SCOPES = 0..3
        const val MAX_CHANNEL = 255
        const val RED = 16
        const val GREEN = 8
    }
}
