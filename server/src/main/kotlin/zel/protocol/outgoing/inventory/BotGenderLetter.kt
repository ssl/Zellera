package zel.protocol.outgoing.inventory

import zel.game.bots.Bot

object BotGenderLetter {
    fun of(bot: Bot): String =
        bot.gender
            .toString()
            .lowercase()
            .take(1)
}
