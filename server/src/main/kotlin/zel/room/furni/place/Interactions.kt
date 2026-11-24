package zel.room.furni.place

import zel.game.items.Item
import zel.game.pets.PetBoxes
import zel.room.furni.behaviour.special.PetFurni

object Interactions {
    const val ROLLER = "roller.belt"
    const val WATER = "water.area"
    const val POST_IT = "note.sticky"
    const val STICKY_POLE = "note.pole"
    const val DIMMER = "room.dimmer"
    const val WEB_API = "wf_xtra_var_web_api"

    val ON_EMPTY_TILES: Set<String> = setOf(ROLLER, "game.tileclaim.tile", "game.freeze.tile")

    val ONE_PER_ROOM: Map<String, PlaceError> =
        mapOf(
            DIMMER to PlaceError.MaxDimmers,
            "music.jukebox" to PlaceError.MaxSoundFurni,
            "control.wired" to PlaceError.TypeLimit,
            "control.chooser" to PlaceError.TypeLimit,
            WEB_API to PlaceError.TypeLimit,
        )

    private val WIRED_BOX_PREFIXES =
        listOf("wf_trg_", "wf_act_", "wf_cnd_", "wf_slc_", "wf_xtra_", "wf_var_", "wf_contract_")

    private const val INVISIBLE = "is_invisible"

    private val STATE_CONTROLLERS = setOf("control.invisible", "control.handitems")

    fun isWiredBox(type: String): Boolean = WIRED_BOX_PREFIXES.any(type::startsWith)

    fun changesViewerStates(definition: Item): Boolean =
        definition.params.split(',').any { it.trim() == INVISIBLE } || of(definition) in STATE_CONTROLLERS

    fun of(definition: Item): String = definition.interaction.name

    fun behaviourKey(definition: Item): String =
        if (PetBoxes.isBox(definition.name)) PetFurni.PET_BOX_TYPE else of(definition)
}
