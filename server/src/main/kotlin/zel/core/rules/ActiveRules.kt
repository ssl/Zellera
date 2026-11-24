package zel.core.rules

import java.util.concurrent.atomic.AtomicReference

object ActiveRules {
    private val current = AtomicReference<GameRules?>()

    fun get(): GameRules = current.get() ?: synchronized(this) { current.get() ?: GameRules.load().also(current::set) }

    fun set(next: GameRules?) = current.set(next)
}

val rules: GameRules get() = ActiveRules.get()
