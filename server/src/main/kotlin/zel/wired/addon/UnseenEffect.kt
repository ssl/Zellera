package zel.wired.addon

import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.box.EffectPicker
import zel.wired.firing.Firing

class UnseenEffect(
    override val furniId: Int,
    override val settings: BoxSettings,
) : EffectPicker {
    override val precedence = PRECEDENCE

    private val seen = LinkedHashSet<Int>()

    override fun pick(
        effects: List<EffectBox>,
        firing: Firing,
    ): List<EffectBox> {
        val next =
            effects.firstOrNull { it.furniId !in seen }
                ?: effects.firstOrNull()?.also { seen.clear() }
                ?: return emptyList()
        seen += next.furniId
        return listOf(next)
    }

    /** A picker that outranks it (wf_xtra_random) picks instead, and this one stays dark. */
    override fun blinks(firing: Firing): Boolean =
        firing.addons.none { it is EffectPicker && it.precedence > precedence }

    companion object {
        const val PRECEDENCE = 2
    }
}
