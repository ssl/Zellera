package zel.wired.effect.bot

import zel.room.RoomScope
import zel.room.unit.bot.RoomBots
import zel.room.unit.walk.Walking
import zel.wired.box.BoxKind
import zel.wired.box.BoxSettings
import zel.wired.box.BoxType
import zel.wired.box.WiredBox
import zel.wired.effect.user.Teleports
import zel.wired.form.DialogSpecs
import kotlin.random.Random

class BotEffectBoxes(
    bots: RoomBots,
    walking: Walking,
    errands: BotErrands,
    talks: BotTalks,
    teleports: Teleports,
    nameOf: (RoomScope, Int) -> String?,
    random: Random = Random.Default,
) {
    val types: List<BoxType<WiredBox>> =
        listOf(
            type("wf_act_bot_teleport") { id, s -> BotTeleport(id, s, bots, teleports, random) },
            type("wf_act_bot_move") { id, s -> BotMove(id, s, bots, walking, random) },
            type("wf_act_bot_talk") { id, s -> BotTalk(id, s, bots, talks) },
            type("wf_act_bot_talk_to_avatar") { id, s -> BotTalkToAvatar(id, s, bots, talks, nameOf) },
            type("wf_act_bot_clothes") { id, s -> BotClothes(id, s, bots) },
            type("wf_act_bot_follow_avatar") { id, s -> BotFollowAvatar(id, s, bots, errands) },
            type("wf_act_bot_give_handitem") { id, s -> BotGiveHandItem(id, s, bots, errands) },
        )

    private fun type(
        code: String,
        create: (Int, BoxSettings) -> WiredBox,
    ): BoxType<WiredBox> = DialogSpecs.boxType(code, BoxKind.Effect, create)
}
