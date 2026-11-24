package zel.wired.chest

import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets
import zel.wired.variable.RoomVariables

class GiveFromChest(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val coins: Boolean,
    private val gifts: Gifts,
    private val chests: RoomChests,
    private val notices: ChestNotices,
    private val variables: RoomVariables,
) : EffectBox {
    override val negative = false
    override val needsUser = true

    override fun run(firing: Firing) {
        val scope = firing.scope
        val usable =
            Targets
                .furni(firing, this, 0)
                .mapNotNull { furni -> chests.chest(furni)?.let { furni to it } }
                .filter { it.second.wiredEnabled && it.second.isCoins == coins }
        val units = Targets.units(firing, this, 0)
        if (usable.isEmpty()) {
            if (!coins) {
                units.firstNotNullOfOrNull { it.playerId() }?.let { notices.failureTo(scope, it) }
            }
            return
        }
        val amount = amount(firing)
        if (amount <= 0 && settings.intAt(GIVE_ALL) == 0) return
        units.forEach { unit ->
            unit.playerId()?.let { player ->
                val gift =
                    Gifts.Gift(
                        player,
                        furniId,
                        amount,
                        settings.intAt(GIVE_ALL) != 0,
                        settings.text,
                        settings.intAt(POPUP) != 0,
                        scope.room.info.ownerId,
                    )
                if (coins) {
                    gifts.credits(scope, gift, usable, settings.intAt(OPTION))
                } else {
                    gifts.furni(scope, gift, usable, settings.intAt(OPTION))
                }
            }
        }
    }

    private fun amount(firing: Firing): Int {
        if (settings.intAt(FROM_VARIABLE) == 0) return settings.intAt(AMOUNT).coerceAtLeast(0)
        val read = variables.readFor(firing, this, 0, settings.intAt(VARIABLE_TARGET), 1, 1)
        return read?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt() ?: 0
    }

    private companion object {
        const val GIVE_ALL = 0
        const val AMOUNT = 1
        const val FROM_VARIABLE = 2
        const val VARIABLE_TARGET = 3
        const val POPUP = 4
        const val OPTION = 5
    }
}
