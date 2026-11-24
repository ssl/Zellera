package zel.wired.effect.furni

import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.UnitsOnTiles
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.wired.box.BoxKind
import zel.wired.box.BoxSettings
import zel.wired.box.BoxType
import zel.wired.box.WiredBox
import zel.wired.condition.MoveProbe
import zel.wired.form.DialogSpecs
import zel.wired.timing.RoomPoster
import kotlin.random.Random

class FurniEffectBoxes(
    units: UnitsOnTiles,
    poster: RoomPoster,
    random: Random = Random.Default,
    /** Whether the room's viewers have a furni; a move in or out of view removes or adds it. */
    view: (Room) -> (RoomFurni) -> Boolean = { { true } },
    /** Where chase and flee see a user: mid-step, the tile they step from. */
    standsOn: (RoomScope, RoomUnit) -> Point = { _, unit -> unit.tile },
) {
    private val moves = FurniMoves(units, poster, view)
    private val walls = WallMoves(moves)

    val moveProbe: MoveProbe = FurniMoveProbe(moves)

    fun matchBox(
        furniId: Int,
        settings: BoxSettings,
    ) = MatchToSnapshot(furniId, settings, moves, walls)

    val types: List<BoxType<WiredBox>> =
        listOf(
            type("wf_act_move_rotate") { id, s -> MoveRotate(id, s, moves, walls, random) },
            type("wf_act_move_to_dir") { id, s -> MoveToDirection(id, s, moves, random) },
            type("wf_act_chase") { id, s -> Chase(id, s, moves, random, standsOn) },
            type("wf_act_flee") { id, s -> Flee(id, s, moves, standsOn) },
            type("wf_act_rel_mov") { id, s -> RelativeMove(id, s, moves, walls) },
            type("wf_act_move_furni_as_group") { id, s -> MoveAsGroup(id, s, moves, walls) },
            type("wf_act_set_altitude") { id, s -> SetAltitude(id, s, moves, walls) },
            type("wf_act_furni_to_furni") { id, s -> FurniToFurni(id, s, moves, walls, random) },
            type("wf_act_furni_to_user") { id, s -> FurniToUser(id, s, moves) },
            type("wf_act_toggle_to_rnd") { id, s -> ToggleToRandom(id, s, random) },
            type("wf_act_match_to_sshot") { id, s -> MatchToSnapshot(id, s, moves, walls) },
            type("wf_act_move_furni_to") { id, s -> MoveFurniTo(id, s, moves, random) },
        )

    private fun type(
        code: String,
        create: (Int, BoxSettings) -> WiredBox,
    ): BoxType<WiredBox> = DialogSpecs.boxType(code, BoxKind.Effect, create)
}
