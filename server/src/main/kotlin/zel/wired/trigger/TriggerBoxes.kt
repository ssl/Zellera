package zel.wired.trigger

import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.wired.box.BoxKind
import zel.wired.box.BoxSettings
import zel.wired.box.BoxType
import zel.wired.box.WiredBox
import zel.wired.form.BoxValidation
import zel.wired.form.DialogSpecs

class TriggerBoxes(
    playerName: (RoomScope, Int) -> String?,
    botName: (RoomScope, Int) -> String?,
) {
    val types: List<BoxType<WiredBox>> =
        listOf(
            type("wf_trg_says_something", ::SaysSomething),
            type("wf_trg_walks_off_furni", ::WalksOffFurni),
            type("wf_trg_state_changed", ::StateChanged),
            type("wf_trg_stuff_state", ::StuffState),
            type("wf_trg_enter_room") { id, s -> EnterRoom(id, s, playerName) },
            type("wf_trg_leave_room") { id, s -> EventTrigger(id, s) { it is RoomEvent.Leaving } },
            type("wf_trg_room_initializes") { id, s -> EventTrigger(id, s) { it == RoomEvent.Initialized } },
            type("wf_trg_game_starts") { id, s -> EventTrigger(id, s) { it == RoomEvent.GameStarted } },
            type("wf_trg_game_ends") { id, s -> EventTrigger(id, s) { it == RoomEvent.GameEnded } },
            type("wf_trg_collision") { id, s -> EventTrigger(id, s) { it is RoomEvent.Collision } },
            type("wf_trg_click_furni", ::ClickFurni),
            type("wf_trg_click_tile", ::ClickTile),
            type(ClickUser.CODE, ::ClickUser),
            type("wf_trg_user_performs_action", ::PerformsAction),
            type("wf_trg_score_achieved", ::ScoreAchieved),
            type("wf_trg_bot_reached_stf") { id, s -> BotReachedFurni(id, s, botName) },
            type("wf_trg_bot_reached_avtr") { id, s -> BotReachedAvatar(id, s, botName) },
            type("wf_trg_clock_counter", ::ClockCounter),
            type("wf_trg_transaction_complete") { id, s -> TransactionTrigger(id, s, completes = true) },
            type("wf_trg_transaction_fail") { id, s -> TransactionTrigger(id, s, completes = false) },
        )

    val validations: Map<String, BoxValidation> get() = TriggerRules.validations

    private fun type(
        code: String,
        create: (Int, BoxSettings) -> WiredBox,
    ): BoxType<WiredBox> = DialogSpecs.boxType(code, BoxKind.Trigger, create)
}
