package zel.wired.effect.bot

import zel.room.unit.bot.RoomBots
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.effect.user.Destinations
import zel.wired.effect.user.Teleports
import zel.wired.firing.Firing
import kotlin.random.Random

class BotTeleport(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val bots: RoomBots,
    private val teleports: Teleports,
    private val random: Random,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val furni = Destinations.of(firing, this)
        if (furni.isEmpty()) return
        BotChoice.bots(firing, this, bots).forEach { (_, unit) ->
            teleports.start(firing.scope, unit, furni.random(random))
        }
    }
}
