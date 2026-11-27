package zel.wired.chest

import zel.protocol.generated.ErrorReportOut
import zel.protocol.generated.ErrorReportOutCodec
import zel.protocol.generated.WiredContractContentsOutCodec
import zel.protocol.generated.WiredContractUpdateResultOut
import zel.protocol.generated.WiredContractUpdateResultOutCodec
import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.place.Interactions
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.util.GameZone
import zel.wired.form.WiredMasks
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

class ContractWindow(
    private val contracts: ChestContracts,
    private val rules: ContractRules,
    private val masks: (Room) -> WiredMasks,
    private val filtered: (Int, String) -> String = { _, text -> text },
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun open(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        id: Int,
    ) {
        val furni = scope.room.furni[id] ?: return
        val kind = ContractKind.fromCode(Interactions.of(furni.definition)) ?: return
        val contract = contracts.of(scope.room, furni) ?: return
        when {
            !masks(scope.room).mayRead(level) -> {
                scope.refuseChestUse(playerId, NO_READ)
            }

            contract.isUnreadable -> {
                report(scope, playerId, UNREADABLE_REPORT)
            }

            else -> {
                check(contract.kind == kind)
                scope.out.send(
                    Lane.Now,
                    Audience.Player(playerId),
                    WiredContractContentsOutCodec,
                    ContractWire.contents(id, contract),
                )
            }
        }
    }

    fun update(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        save: ContractSave,
    ) {
        val furni = scope.room.furni[save.id] ?: return
        val kind = ContractKind.fromCode(Interactions.of(furni.definition)) ?: return
        if (!masks(scope.room).mayModify(level)) return scope.refuseChestUse(playerId, NO_WRITE)
        when (val outcome = rules.check(kind, save.copy(text = filtered(playerId, save.text)))) {
            ContractOutcome.Broken -> {
                report(scope, playerId, BROKEN_REPORT)
            }

            ContractOutcome.Unreadable -> {
                contracts.of(scope.room, furni)?.let { contracts.save(scope.room, furni, it.madeUnreadable()) }
            }

            is ContractOutcome.Refused -> {
                result(scope, playerId, save.id, ok = false, outcome.code)
            }

            is ContractOutcome.Saved -> {
                contracts.save(scope.room, furni, outcome.contract)
                result(scope, playerId, save.id, ok = true, "")
            }
        }
    }

    private fun result(
        scope: RoomScope,
        playerId: Int,
        id: Int,
        ok: Boolean,
        code: String,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(playerId),
        WiredContractUpdateResultOutCodec,
        WiredContractUpdateResultOut(id, ok, code),
    )

    private fun report(
        scope: RoomScope,
        playerId: Int,
        messageId: Int,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(playerId),
        ErrorReportOutCodec,
        ErrorReportOut(messageId, 1, STAMP.format(Instant.ofEpochMilli(clock()))),
    )

    private companion object {
        const val NO_READ = "wiredfurni.error.no_read_rights"
        const val NO_WRITE = "wiredfurni.error.no_write_rights"
        const val UNREADABLE_REPORT = 3090
        const val BROKEN_REPORT = 3091
        val PATTERN: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d/yy, h:mm a", Locale.US)

        /** In the game zone as it is now, also after a settings reload changes it. */
        val STAMP: DateTimeFormatter get() = PATTERN.withZone(GameZone.current)
    }
}
