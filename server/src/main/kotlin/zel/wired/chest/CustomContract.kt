package zel.wired.chest

import zel.room.Room
import zel.wired.box.BoxSettings
import zel.wired.form.BoxValidation
import zel.wired.form.SaveOutcome
import zel.wired.form.SaveRequest

object CustomContract {
    const val CODE = "wf_xtra_custom_contract"
    private const val MISCONFIGURED = "wiredfurni.error.misconfigured_contract"
    private const val SPELLED_OUT = -1
    private const val PAY_ON = 0
    private const val PAY_TYPE = 1
    private const val PAY_VALUE = 3
    private const val REWARD_ON = 5
    private const val REWARD_TYPE = 6
    private const val REWARD_VALUE = 8
    private const val NODE_INTS = 5
    private const val NODE_WALL = 2
    private const val NODE_TYPE = 3
    private const val NODE_AMOUNT = 4
    private const val OFFSET_FIRST_NODE = 3
    private const val PLACEHOLDER_TYPE = 2

    data class Terms(
        val contract: Contract,
        val usable: Boolean,
    )

    fun terms(ints: List<Int>): Terms {
        if (ints.firstOrNull() == SPELLED_OUT) return spelledOut(ints)
        val pays = ints.int(PAY_ON) != 0
        val rewards = ints.int(REWARD_ON) != 0
        val usable = (!pays || credits(ints, PAY_TYPE)) && (!rewards || credits(ints, REWARD_TYPE))
        val give = if (pays && credits(ints, PAY_TYPE)) listOf(listOf(coins(ints.int(PAY_VALUE)))) else emptyList()
        val get = if (rewards && credits(ints, REWARD_TYPE)) listOf(coins(ints.int(REWARD_VALUE))) else emptyList()
        return Terms(contract(give, get), usable)
    }

    private fun credits(
        ints: List<Int>,
        typeAt: Int,
    ): Boolean = ints.int(typeAt) == 0 && ints.int(typeAt + 1) == 0

    private fun coins(amount: Int): Term = Term(true, amount.coerceIn(1, Term.MAX_CREDITS))

    private fun spelledOut(ints: List<Int>): Terms {
        val nodes = nodeCount(ints, 2)
        val give = (0 until nodes).map { node(ints, OFFSET_FIRST_NODE + it * NODE_INTS) }
        val rewardsAt = OFFSET_FIRST_NODE + nodes * NODE_INTS
        val get = (0 until nodeCount(ints, rewardsAt)).map { node(ints, rewardsAt + 1 + it * NODE_INTS) }
        return Terms(contract(if (give.isEmpty()) emptyList() else listOf(give), get), usable = true)
    }

    private fun nodeCount(
        ints: List<Int>,
        at: Int,
    ): Int = ints.int(at).coerceIn(0, Contract.MAX_RULES)

    private fun node(
        ints: List<Int>,
        at: Int,
    ): Term {
        val credits = ints.int(at) == 0
        return Term(
            credits = credits,
            amount = ints.int(at + NODE_AMOUNT).coerceIn(1, Term.ceiling(credits)),
            wall = ints.int(at + NODE_WALL) != 0,
            typeId = ints.int(at + NODE_TYPE),
        )
    }

    private fun spelledOutBroken(ints: List<Int>): Boolean {
        val raw = ints.int(2)
        if (raw !in 0..Contract.MAX_RULES) return true
        val rewardsAt = OFFSET_FIRST_NODE + raw * NODE_INTS
        val rewards = ints.int(rewardsAt)
        if (rewards !in 0..Contract.MAX_RULES) return true
        val starts =
            (0 until raw).map { OFFSET_FIRST_NODE + it * NODE_INTS } +
                (0 until rewards).map { rewardsAt + 1 + it * NODE_INTS }
        return starts.any { at -> ints.int(at + NODE_AMOUNT) !in 1..Term.ceiling(ints.int(at) == 0) }
    }

    private fun contract(
        give: List<List<Term>>,
        get: List<Term>,
    ): Contract = Contract(ContractKind.PAYMENT, give, get, PaymentForm(Contract.MODE_SPECIFIC, layout = ""))

    private fun List<Int>.int(at: Int): Int = getOrElse(at) { 0 }

    val validation: BoxValidation =
        object : BoxValidation {
            override fun check(
                room: Room,
                settings: BoxSettings,
            ): SaveOutcome = SaveOutcome.Saved(settings)

            override fun check(
                room: Room,
                settings: BoxSettings,
                request: SaveRequest,
            ): SaveOutcome {
                val ints = settings.ints
                return when {
                    ints.firstOrNull() == SPELLED_OUT && spelledOutBroken(ints) -> {
                        SaveOutcome.Invalid(MISCONFIGURED)
                    }

                    ints.firstOrNull() == SPELLED_OUT || request.ints.isEmpty() -> {
                        SaveOutcome.Saved(settings)
                    }

                    ints.int(PAY_ON) == 0 && ints.int(REWARD_ON) == 0 -> {
                        SaveOutcome.Invalid(MISCONFIGURED)
                    }

                    else -> {
                        SaveOutcome.Saved(settings.copy(ints = normalized(ints)))
                    }
                }
            }
        }

    private fun normalized(ints: List<Int>): List<Int> =
        ints.mapIndexed { i, value ->
            when (i) {
                PAY_VALUE, REWARD_VALUE -> value.coerceAtLeast(1)
                PAY_TYPE, REWARD_TYPE -> if (value == PLACEHOLDER_TYPE) 0 else value
                else -> value
            }
        }
}
