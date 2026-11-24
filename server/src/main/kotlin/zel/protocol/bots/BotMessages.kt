package zel.protocol.bots

import zel.game.bots.Bot
import zel.game.players.Player
import zel.protocol.generated.BotCommandConfigurationOut
import zel.protocol.generated.BotInventoryOut
import zel.protocol.outgoing.inventory.BotGenderLetter

object BotMessages {
    private const val SETTING_CHAT = 2
    private const val SETTING_NAME = 5
    private const val SETTING_MOTTO = 9

    fun inventory(player: Player) =
        BotInventoryOut(
            player.inventory.bots.all.map {
                BotInventoryOut.BotsItem(it.id, it.name, it.motto, BotGenderLetter.of(it), it.figure)
            },
        )

    fun settings(
        bot: Bot,
        settingId: Int,
    ) = BotCommandConfigurationOut(-bot.id, settingId, settingValue(bot, settingId))

    private fun settingValue(
        bot: Bot,
        settingId: Int,
    ): String =
        when (settingId) {
            SETTING_NAME -> bot.name
            SETTING_CHAT -> bot.speech.configuration()
            SETTING_MOTTO -> bot.motto
            else -> ""
        }
}
