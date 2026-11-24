package zel.wired.chest

import zel.room.Room
import zel.wired.box.BoxKind
import zel.wired.box.BoxSettings
import zel.wired.box.BoxType
import zel.wired.box.WiredBox
import zel.wired.form.BoxValidation
import zel.wired.form.DialogSpecs
import zel.wired.form.SaveOutcome
import zel.wired.variable.RoomVariables
import kotlin.math.absoluteValue

class ChestBoxes(
    transactions: Transactions,
    contracts: ChestContracts,
    deals: ChestDeals,
    gifts: Gifts,
    private val chests: RoomChests,
    notices: ChestNotices,
    variables: RoomVariables,
) {
    val types: List<BoxType<WiredBox>> =
        listOf(
            effect(INIT) { id, s -> InitTransaction(id, s, transactions, contracts, notices, variables) },
            effect("wf_act_cancel_transaction") { id, s -> CancelTransaction(id, s, deals) },
            effect(GIVE_FURNI) { id, s -> GiveFromChest(id, s, false, gifts, chests, notices, variables) },
            effect(GIVE_CURRENCY) { id, s -> GiveFromChest(id, s, true, gifts, chests, notices, variables) },
            addon(CustomContract.CODE) { id, s -> CustomContractBox(id, s) },
            addon(SCAN) { id, s -> ScanChest(id, s, chests, variables) },
        )

    private fun addon(
        code: String,
        create: (Int, BoxSettings) -> WiredBox,
    ): BoxType<WiredBox> = DialogSpecs.boxType(code, BoxKind.Addon, create)

    private fun effect(
        code: String,
        create: (Int, BoxSettings) -> WiredBox,
    ): BoxType<WiredBox> = DialogSpecs.boxType(code, BoxKind.Effect, create)

    val validations: Map<String, BoxValidation> =
        mapOf(
            INIT to BoxValidation { room, s -> init(room, s) },
            GIVE_FURNI to BoxValidation { room, s -> give(room, s, coins = false, REQUIRE_FURNI) },
            GIVE_CURRENCY to BoxValidation { room, s -> give(room, s, coins = true, REQUIRE_COINS) },
            SCAN to BoxValidation { room, s -> scan(room, s) },
            CustomContract.CODE to CustomContract.validation,
        ) +
            ContractKind.entries.associate { kind ->
                kind.code to
                    BoxValidation { _, _ -> SaveOutcome.Ignored }
            }

    private fun init(
        room: Room,
        settings: BoxSettings,
    ): SaveOutcome {
        val picked = settings.picks.mapNotNull { id -> chests.inRoom(room, id.absoluteValue)?.second }
        // a chest without wired, or no chest at all (only a contract), is refused
        if (picked.none() || picked.any { !it.wiredEnabled }) return SaveOutcome.Invalid(REQUIRE_CHESTS)
        val ints = settings.ints.toMutableList()
        ints[MULTIPLIER_INT] = ints[MULTIPLIER_INT].coerceIn(MIN_MULTIPLIER, MAX_MULTIPLIER)
        ints[TIMEOUT_INT] = ints[TIMEOUT_INT].coerceAtLeast(MIN_TIMEOUT)
        return SaveOutcome.Saved(settings.copy(ints = ints))
    }

    private fun give(
        room: Room,
        settings: BoxSettings,
        coins: Boolean,
        refusal: String,
    ): SaveOutcome {
        val fits =
            settings.picks.all { id ->
                val chest = chests.inRoom(room, id.absoluteValue)?.second
                chest != null && chest.wiredEnabled && chest.isCoins == coins
            }
        return if (fits) SaveOutcome.Saved(settings) else SaveOutcome.Invalid(refusal)
    }

    private fun scan(
        room: Room,
        settings: BoxSettings,
    ): SaveOutcome {
        val chestSlotPicks = settings.furniSources.getOrNull(1) == PICKED
        val fits =
            !chestSlotPicks ||
                settings.picks.all { id -> chests.inRoom(room, id.absoluteValue)?.second?.isCoins == false }
        return if (fits) SaveOutcome.Saved(settings) else SaveOutcome.Invalid(REQUIRE_FURNI)
    }

    companion object {
        private const val INIT = "wf_act_init_transaction"
        private const val GIVE_FURNI = "wf_act_give_furni"
        private const val GIVE_CURRENCY = "wf_act_give_currency"
        private const val SCAN = "wf_xtra_scan_chest_furni_by_type"
        private const val REQUIRE_CHESTS = "wiredfurni.error.require_chests"
        private const val REQUIRE_FURNI = "wiredfurni.error.require_furni_chests"
        private const val REQUIRE_COINS = "wiredfurni.error.require_coin_chests"
        private const val MULTIPLIER_INT = 1
        private const val TIMEOUT_INT = 5
        const val MIN_MULTIPLIER = 2
        const val MAX_MULTIPLIER = 500
        private const val MIN_TIMEOUT = 30
        private const val PICKED = 100
    }
}
