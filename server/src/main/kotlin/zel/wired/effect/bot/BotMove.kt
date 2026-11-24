package zel.wired.effect.bot

import zel.room.unit.bot.RoomBots
import zel.room.unit.walk.Walking
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.effect.user.Destinations
import zel.wired.firing.Firing
import kotlin.random.Random

class BotMove(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val bots: RoomBots,
    private val walking: Walking,
    private val random: Random,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val tiles = Destinations.of(firing, this).mapNotNull { it.anchor }
        BotChoice.bots(firing, this, bots).forEach { (_, unit) ->
            // The furni the bot stands on can be drawn too: then it stays.
            val tile = tiles.randomOrNull(random)
            if (tile != null && tile != unit.tile) walking.walkTo(firing.scope, unit, tile, byWired = true)
        }
    }
}
