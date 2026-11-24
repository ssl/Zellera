package zel.wired.signal

import zel.core.rules.rules
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.SignalPayload
import zel.wired.firing.Targets

class SendSignal(
    override val furniId: Int,
    override val settings: BoxSettings,
    override val negative: Boolean,
    private val router: SignalRouter,
) : EffectBox {
    override val needsUser = false

    override fun run(firing: Firing) {
        val hops = (firing.signal?.hops ?: -1) + 1
        val antennas = Targets.furni(firing, this, ANTENNAS, picksForTriggering = false).map { it.id }
        if (hops >= rules.limits.wiredSignalHops) {
            // The send past the hop limit still flips its antennas; its receivers no longer run.
            firing.send { antennas.forEach { router.flipped(firing, it) } }
            return
        }
        val forwarded = rules.limits.wiredSignalForwarded
        val furni =
            Targets.uncounted(firing) {
                Targets.furni(firing, this, FORWARD, picksForTriggering = false).map { it.id }.take(forwarded)
            }
        val users = Targets.uncounted(firing) { Targets.units(firing, this).map { it.index }.take(forwarded) }
        val splitFurni = settings.ints.getOrNull(SPLIT_FURNI) == 1
        val splitUsers = settings.ints.getOrNull(SPLIT_USERS) == 1
        val furniGroups = if (splitFurni) furni.map(::listOf) else listOf(furni)
        val userGroups = if (splitUsers) users.map(::listOf) else listOf(users)
        val signals =
            antennas.asSequence().flatMap { antenna ->
                userGroups.asSequence().flatMap { users -> furniGroups.map { furni -> Triple(antenna, users, furni) } }
            }
        // The signals go once all of this run's effects ran, so a receiver reads what the others wrote (a re-roll
        // loop's check sees the new roll); the context goes as it is then.
        firing.send {
            signals.takeWhile { firing.chain.takeSignal() }.forEach { (antenna, userGroup, furniGroup) ->
                val context = firing.context.filterKeys { !it.startsWith('-') }.mapValues { it.value.value }
                val payload = SignalPayload(furniGroup, userGroup, context, hops = hops)
                val unit = if (splitUsers) userGroup.firstOrNull() else firing.triggeringUnit
                val item = if (splitFurni) furniGroup.firstOrNull() else firing.triggeringFurni
                router.send(firing, antenna, payload, unit, item)
            }
        }
    }

    companion object {
        private const val ANTENNAS = 0
        private const val FORWARD = 1
        private const val SPLIT_FURNI = 0
        private const val SPLIT_USERS = 1
    }
}
