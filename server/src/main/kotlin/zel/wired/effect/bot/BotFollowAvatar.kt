package zel.wired.effect.bot

import zel.room.unit.UnitKind
import zel.room.unit.bot.RoomBots
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class BotFollowAvatar(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val bots: RoomBots,
    private val errands: BotErrands,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val chosen = BotChoice.bots(firing, this, bots)
        if (settings.ints.getOrNull(MODE) == STOP) {
            chosen.forEach { (bot, _) -> errands.stopFollowing(firing.scope, bot) }
            return
        }
        val users = Targets.units(firing, this, USER_SLOT).filter { it.kind is UnitKind.Player }
        if (users.isEmpty()) return
        chosen.forEachIndexed { i, (bot, _) -> errands.follow(firing.scope, bot, users[i % users.size]) }
    }

    companion object {
        const val MODE = 0
        const val STOP = 0
        const val USER_SLOT = 1
    }
}
