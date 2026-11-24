package zel.game.players

internal fun <T : Any> T?.required(): T = checkNotNull(this) { "a game service is not started" }
