package zel.game.bots

import java.sql.Connection

fun interface BotEnvironment {
    fun connection(): Connection
}
