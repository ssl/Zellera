package zel.protocol.outgoing.inventory

import zel.game.bots.Bot
import zel.protocol.generated.BotAddedToInventoryOut
import zel.protocol.generated.BotAddedToInventoryOutCodec
import zel.protocol.outgoing.OutgoingMessage

class AddBot(
    val bot: Bot,
) : OutgoingMessage<BotAddedToInventoryOut>(BotAddedToInventoryOutCodec) {
    override fun message() =
        BotAddedToInventoryOut(bot.id, bot.name, bot.motto, BotGenderLetter.of(bot), bot.figure, true)
}
