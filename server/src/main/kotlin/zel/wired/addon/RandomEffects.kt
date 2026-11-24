package zel.wired.addon

import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.box.EffectPicker
import zel.wired.firing.Firing
import kotlin.random.Random

class RandomEffects(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val random: Random = Random.Default,
) : EffectPicker {
    override val precedence = PRECEDENCE

    private val skip = settings.intAt(0).coerceIn(0, MAX_SKIP)
    private val amount = settings.intAt(1, 1).coerceIn(1, MAX_AMOUNT)

    private val history = ArrayDeque<Set<Int>>()

    override fun pick(
        effects: List<EffectBox>,
        firing: Firing,
    ): List<EffectBox> {
        if (effects.isEmpty()) return effects
        val recent = history.flatten().toSet()
        val (used, unused) = effects.partition { it.furniId in recent }
        val chosen = (unused.shuffled(random) + used.shuffled(random)).take(minOf(amount, effects.size))
        remember(chosen)
        return chosen
    }

    private fun remember(chosen: List<EffectBox>) {
        if (skip == 0) return
        history.addLast(chosen.map { it.furniId }.toSet())
        while (history.size > skip) history.removeFirst()
    }

    companion object {
        const val PRECEDENCE = 3
        private const val MAX_SKIP = 1000
        private const val MAX_AMOUNT = 1000
    }
}
