package zel.room.furni.place

import zel.game.items.FurniHeights
import zel.game.items.ItemInteraction
import zel.protocol.generated.ItemAddOut
import zel.protocol.generated.ItemRemoveOut
import zel.protocol.generated.ItemUpdateOut
import zel.protocol.generated.ObjectAddOut
import zel.protocol.generated.ObjectRemoveOut
import zel.protocol.generated.ObjectUpdateOut
import zel.protocol.generated.StuffData
import zel.room.furni.FurniKind
import zel.room.furni.RoomFurni
import zel.room.furni.StackHelperState
import zel.room.furni.behaviour.special.SpecialStuff

object FurniWire {
    /** The expiration seconds of furni that never expires, which is every furni. */
    private const val NEVER_EXPIRES = -1

    private const val NOT_USABLE = 0
    private const val NO_PICKER = -1
    private const val RIGHTS_USE = 1
    private const val EVERYONE_USES = 2
    private const val WIRED_PREFIX = "wf_"

    /** Wired boxes whose state 0 goes out as "0" whatever their state count. */
    private val SENDS_ZERO = listOf("wf_trg_", "wf_act_", "wf_cnd_", "wf_slc_", "wf_xtra_", "wf_var_")

    private val EVERYONE =
        setOf(
            "transit.door",
            "transit.tile",
            "state.switch",
            "vending.plain",
            "vending.any_side",
            "vending.effect",
            "puzzle.box",
            "basic.terminal",
        )

    private val NOBODY = setOf("crack.master", "effect.tile", "effect.gym", "state.pyramid")

    private val RIGHTS_ALWAYS = setOf("tile.stack", "tile.walk", "gate.plain", "pet.box")

    private val PLAIN =
        setOf(
            "default",
            "basic.bed",
            "display.trophy",
            "area.tent",
            "water.area",
            "effect.box",
            "effect.tile",
            "pet.drink",
            // plate.pressure is not plain: a released wf_pressureplate goes out as "0". The group plate stays plain.
            "plate.pressure.group",
            "tile.click",
            // Like default: a one-state table would send its 0 as "". Every crafting table has two states and
            // sends "0".
            "crafting.altar",
        )

    fun objectAdd(furni: RoomFurni): ObjectAddOut {
        val anchor = requireNotNull(furni.anchor) { "furni ${furni.id} is not on the floor" }
        return ObjectAddOut(
            furni.id,
            furni.definition.spriteId,
            anchor.x,
            anchor.y,
            furni.direction,
            FurniHeights.formatZ(furni.z),
            furni.heightOnWire,
            extra(furni),
            stuff(furni),
            NEVER_EXPIRES,
            usagePolicy(furni),
            furni.placerId,
            ObjectAddOut.Branch1.Else,
            furni.ownerName,
        )
    }

    /** Carries the furni's own usage policy, as ObjectAdd does. */
    fun objectUpdate(furni: RoomFurni): ObjectUpdateOut {
        val anchor = requireNotNull(furni.anchor) { "furni ${furni.id} is not on the floor" }
        return ObjectUpdateOut(
            furni.id,
            furni.definition.spriteId,
            anchor.x,
            anchor.y,
            furni.direction,
            FurniHeights.formatZ(furni.z),
            furni.heightOnWire,
            extra(furni),
            stuff(furni),
            NEVER_EXPIRES,
            usagePolicy(furni),
            furni.placerId,
            ObjectUpdateOut.Branch1.Else,
        )
    }

    fun objectRemove(
        furni: RoomFurni,
        picker: Int = furni.ownerId,
    ): ObjectRemoveOut = ObjectRemoveOut(furni.id.toString(), false, picker, 0, false)

    /**
     * A floor furni leaving the view while it stays in the room: a wired move put it on a tile an area hide that is
     * on hides. Nobody picked it up (-1) and the last flag is set.
     */
    fun objectHidden(furni: RoomFurni): ObjectRemoveOut =
        ObjectRemoveOut(furni.id.toString(), false, NO_PICKER, 0, true)

    fun itemAdd(furni: RoomFurni): ItemAddOut =
        ItemAddOut(
            furni.id.toString(),
            furni.definition.spriteId,
            furni.wallPosition.orEmpty(),
            wallState(furni),
            NEVER_EXPIRES,
            usagePolicy(furni),
            furni.placerId,
            furni.ownerName,
        )

    fun itemUpdate(furni: RoomFurni): ItemUpdateOut =
        ItemUpdateOut(
            furni.id.toString(),
            furni.definition.spriteId,
            furni.wallPosition.orEmpty(),
            wallState(furni),
            NEVER_EXPIRES,
            usagePolicy(furni),
            furni.placerId,
        )

    fun itemRemove(
        furni: RoomFurni,
        picker: Int = furni.ownerId,
    ): ItemRemoveOut = ItemRemoveOut(furni.id.toString(), picker, false)

    fun stuff(furni: RoomFurni): StuffData {
        (furni.behaviour as? StuffOnWire)?.let { return it.stuff(furni) }
        return SpecialStuff.legacy(furni, floorState(furni))
    }

    fun floorState(furni: RoomFurni): String {
        if (furni.kind == FurniKind.StackHelper) return ""
        if (furni.kind == FurniKind.WalkTile) {
            return StackHelperState
                .of(furni.state)
                .heightText
                .takeUnless { it == "0" }
                .orEmpty()
        }
        val state = furni.state.ifEmpty { "0" }
        val interaction = furni.definition.interaction
        // wf_ furni without a behaviour of their own (the game tokens, labels, contract chests) count as plain too.
        // Wired boxes never do: they send their 0 as "0", single-state ones too.
        val box = SENDS_ZERO.any(interaction.name::startsWith)
        val token =
            !box && interaction.name.startsWith(WIRED_PREFIX) && interaction.family == ItemInteraction.Family.NONE
        val plain = interaction.name in PLAIN || token
        return if (state == "0" && plain && furni.definition.stateCount <= 1) "" else state
    }

    private fun extra(furni: RoomFurni): Int =
        when {
            furni.behaviour is ExtraOnWire -> furni.behaviour.extra(furni)
            furni.kind == FurniKind.WalkTile && StackHelperState.of(furni.state).multiWalk -> 1
            else -> 0
        }

    /** A wall item's stored state as it is, an empty one too. */
    private fun wallState(furni: RoomFurni): String =
        if (Interactions.of(furni.definition) == Interactions.POST_IT) {
            furni.state.substringBefore(' ')
        } else {
            furni.state
        }

    /**
     * The usage policy is fixed per furni type: a type with a known value sends it. The rule below covers the other
     * types.
     */
    fun usagePolicy(furni: RoomFurni): Int {
        furni.definition.usagePolicy?.let { return it }
        val type = Interactions.of(furni.definition)
        return when {
            type in EVERYONE -> EVERYONE_USES
            type in NOBODY -> NOT_USABLE
            furni.definition.stateCount > 1 || type in RIGHTS_ALWAYS || Interactions.isWiredBox(type) -> RIGHTS_USE
            else -> NOT_USABLE
        }
    }
}
