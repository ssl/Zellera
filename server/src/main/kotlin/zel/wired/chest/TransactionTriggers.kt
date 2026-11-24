package zel.wired.chest

import zel.room.RoomScope
import zel.wired.WiredRooms
import zel.wired.firing.FiringStart
import zel.wired.firing.SignalPayload
import zel.wired.trigger.TransactionTrigger

class TransactionTriggers(
    private val rooms: () -> WiredRooms?,
) {
    data class Completed(
        val multiplier: Int,
        val depositFurni: Int,
        val depositCoins: Int,
        val withdrawFurni: Int,
        val withdrawCoins: Int,
    )

    fun completed(
        scope: RoomScope,
        contractId: Int,
        unitIndex: Int?,
        moved: Completed,
    ) = run(
        scope,
        contractId,
        unitIndex,
        completes = true,
        mapOf(
            MULTIPLIER to moved.multiplier.toLong(),
            DEPOSIT_FURNI to moved.depositFurni.toLong(),
            DEPOSIT_COINS to moved.depositCoins.toLong(),
            WITHDRAW_FURNI to moved.withdrawFurni.toLong(),
            WITHDRAW_COINS to moved.withdrawCoins.toLong(),
        ),
    )

    fun failed(
        scope: RoomScope,
        contractId: Int,
        unitIndex: Int?,
        reason: Int,
    ) = run(scope, contractId, unitIndex, completes = false, mapOf(REASON to reason.toLong()))

    private fun run(
        scope: RoomScope,
        contractId: Int,
        unitIndex: Int?,
        completes: Boolean,
        values: Map<String, Long>,
    ) {
        val wired = rooms() ?: return
        if (contractId == 0 || WiredRooms.isOff(scope.room)) return
        val payload = SignalPayload(emptyList(), emptyList(), values)
        wired.runEach(scope, null) { stack ->
            val trigger = stack.trigger as? TransactionTrigger
            FiringStart(unitIndex, null, signal = payload).takeIf {
                trigger != null && trigger.completes == completes && trigger.listens(contractId)
            }
        }
    }

    private companion object {
        const val MULTIPLIER = "-470"
        const val DEPOSIT_FURNI = "-471"
        const val DEPOSIT_COINS = "-472"
        const val WITHDRAW_FURNI = "-473"
        const val WITHDRAW_COINS = "-474"
        const val REASON = "-478"
    }
}
