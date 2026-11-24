package zel.wired.chest

import zel.room.furni.place.Interactions
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets
import zel.wired.variable.RoomVariables
import kotlin.math.absoluteValue

class InitTransaction(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val transactions: Transactions,
    private val contracts: ChestContracts,
    private val notices: ChestNotices,
    private val variables: RoomVariables,
) : EffectBox {
    override val negative = false
    override val needsUser = true

    override fun run(firing: Firing) {
        val room = firing.scope.room
        val picked = (settings.picks + settings.picks2).distinct()
        val (furni, contract) =
            picked
                .firstNotNullOfOrNull { id ->
                    room.furni[id.absoluteValue]?.let { f -> contracts.of(room, f)?.let { f to it } }
                } ?: return
        val unit = Targets.units(firing, this, 0).firstOrNull() ?: return
        val playerId = unit.playerId() ?: return
        if (Interactions.of(furni.definition) == CustomContract.CODE && !contracts.customIsUsable(room, furni)) {
            notices.failureToOwner(firing.scope)
            return
        }
        transactions.open(
            firing.scope,
            TransactionRequest(
                playerId,
                unit.index,
                furni.id,
                contract,
                settings.intAt(MODE),
                multiplier(firing),
                picked.map { it.absoluteValue } + contracts.chestsOf(room, furni),
                if (settings.intAt(TIMEOUT_ON) != 0) settings.intAt(TIMEOUT) else 0,
            ),
        )
    }

    private fun multiplier(firing: Firing): Int =
        if (settings.intAt(FROM_VARIABLE) == 0) {
            settings.intAt(MULTIPLIER)
        } else {
            val read = variables.readFor(firing, this, 0, settings.intAt(VARIABLE_TARGET), 2, 1) ?: 0
            read.coerceIn(ChestBoxes.MIN_MULTIPLIER.toLong(), ChestBoxes.MAX_MULTIPLIER.toLong()).toInt()
        }

    private companion object {
        const val MODE = 0
        const val MULTIPLIER = 1
        const val FROM_VARIABLE = 2
        const val VARIABLE_TARGET = 3
        const val TIMEOUT_ON = 4
        const val TIMEOUT = 5
    }
}
