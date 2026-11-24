package zel.wired.effect.user

import zel.wired.box.BoxKind
import zel.wired.box.BoxSettings
import zel.wired.box.BoxType
import zel.wired.box.WiredBox
import zel.wired.form.DialogSpecs

class UserEffectBoxes(
    parts: UserEffectParts,
) {
    val state = UserEffectState(parts.host)
    private val unfreezing = Unfreezing(state, parts.hooks, parts.games)
    val teleports =
        Teleports(
            state,
            parts.walking,
            parts.hooks,
            unfreezing,
            parts.host,
            parts.host::teleportLandMillis,
        )
    val texts = TextPlaceholders(parts.placeholders)
    private val moves = UserMoves(parts.walking, parts.poster, parts.wired)

    val types: List<BoxType<WiredBox>> =
        listOf(
            type("wf_act_teleport_to") { id, s -> TeleportToFurni(id, s, teleports, parts.random) },
            type("wf_act_teleport_to_room") { id, s ->
                TeleportToRoom(id, s, state, parts.host, parts.hooks, parts.poster)
            },
            type("wf_act_kick_user") { id, s -> KickUser(id, s, state, parts.host, parts.poster, texts) },
            type("wf_act_mute_triggerer") { id, s -> MuteTriggerer(id, s, parts.hooks, parts.host) },
            type("wf_act_show_message") { id, s -> ShowMessage(id, s, parts.host, texts) },
            type("wf_act_freeze") { id, s -> FreezeUser(id, s, unfreezing) },
            type("wf_act_unfreeze") { id, s -> UnfreezeUser(id, s, unfreezing) },
            type("wf_act_join_team") { id, s -> JoinTeam(id, s, parts.games) },
            type("wf_act_leave_team") { id, s -> LeaveTeam(id, s, parts.games) },
            type("wf_act_give_score") { id, s -> GiveScore(id, s, state, parts.games) },
            type("wf_act_give_score_tm") { id, s -> GiveTeamScore(id, s, state, parts.games) },
            type("wf_act_click_conf") { id, s -> ClickConf(id, s) },
            type("wf_act_move_rotate_user") { id, s -> MoveRotateUser(id, s, moves) },
            type("wf_act_user_to_furni") { id, s -> UserToFurni(id, s, moves, parts.random) },
            type("wf_act_control_clock") { id, s -> ControlClock(id, s, parts.games) },
            type("wf_act_adjust_clock") { id, s -> AdjustClock(id, s, parts.games) },
            type("wf_act_set_highlight") { id, s -> SetHighlight(id, s, state) },
            type("wf_act_set_visibility") { id, s -> SetVisibility(id, s, state) },
            type("wf_act_place_furni") { id, s ->
                PlaceFurni(id, s, state, parts.host, parts.units, parts.variables, wired = parts.wired)
            },
            type("wf_act_remove_furni") { id, s -> RemoveFurni(id, s, state, parts.host, parts.units) },
            type(GiveReward.CODE) { id, s -> GiveReward(id, s, parts.host, parts.random) },
            type("wf_act_log") { id, s -> WriteLog(id, s, false, texts, parts.roomLog) },
            type("wf_act_neg_log") { id, s -> WriteLog(id, s, true, texts, parts.roomLog) },
        )

    private fun type(
        code: String,
        create: (Int, BoxSettings) -> WiredBox,
    ): BoxType<WiredBox> = DialogSpecs.boxType(code, BoxKind.Effect, create)
}
