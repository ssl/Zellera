package zel.wired.chest

class ContractRules(
    private val tradable: (wall: Boolean, typeId: Int) -> Boolean,
) {
    fun check(
        kind: ContractKind,
        save: ContractSave,
    ): ContractOutcome {
        val paymentForm = save.type == ContractKind.PAYMENT.wire
        return when {
            save.broken -> {
                ContractOutcome.Broken
            }

            paymentForm && kind == ContractKind.PAYMENT && save.paymentMode == Contract.MODE_UNREADABLE -> {
                ContractOutcome.Unreadable
            }

            else -> {
                refusal(kind, save)?.let { ContractOutcome.Refused(it) } ?: ContractOutcome.Saved(contract(kind, save))
            }
        }
    }

    private fun refusal(
        kind: ContractKind,
        save: ContractSave,
    ): String? =
        when {
            wrongLists(kind, save) -> {
                "invalid_contract_type"
            }

            badRules(kind, save) -> {
                "invalid_rules"
            }

            badFurni(save) -> {
                "invalid_furni_types"
            }

            kind == ContractKind.PAYMENT && save.layout !in LAYOUTS -> {
                "invalid_layout_type"
            }

            kind == ContractKind.PAYMENT && save.text.length > RECEIVE_TEXT_LIMIT -> {
                "receive_text_limit"
            }

            kind == ContractKind.REWARD && refusedCategory(save.category) -> {
                "invalid_earning_category"
            }

            else -> {
                null
            }
        }

    private fun refusedCategory(category: Int): Boolean =
        category <= HIGHEST_REFUSED && category != Contract.CATEGORY_GAMES

    private fun wrongLists(
        kind: ContractKind,
        save: ContractSave,
    ): Boolean =
        save.type != kind.wire ||
            when (kind) {
                ContractKind.PAYMENT -> save.get != null
                ContractKind.REWARD -> save.give != null || save.get == null
                ContractKind.TRADE -> save.give == null || save.get == null
            }

    private fun badRules(
        kind: ContractKind,
        save: ContractSave,
    ): Boolean {
        val give = save.give.orEmpty()
        val get = save.get.orEmpty()
        val donation = kind == ContractKind.PAYMENT && save.paymentMode == Contract.MODE_ANYTHING
        val asks = kind != ContractKind.REWARD && !donation
        val gives = kind != ContractKind.PAYMENT
        return (asks && (give.isEmpty() || give.any { it.isEmpty() })) ||
            (gives && get.isEmpty()) ||
            (!donation && (give.size > Contract.MAX_RULES || (give + listOf(get)).any { !fits(it) }))
    }

    private fun fits(rule: List<Term>): Boolean =
        rule.size <= Contract.MAX_RULES && rule.all { it.amount in 1..Term.ceiling(it.credits) }

    private fun badFurni(save: ContractSave): Boolean {
        val terms = save.give.orEmpty().flatten() + save.get.orEmpty()
        return terms.any { !it.credits && !tradable(it.wall, it.typeId) }
    }

    private fun contract(
        kind: ContractKind,
        save: ContractSave,
    ): Contract =
        when (kind) {
            ContractKind.PAYMENT -> {
                Contract(
                    kind,
                    give = if (save.paymentMode == Contract.MODE_ANYTHING) emptyList() else save.give.orEmpty(),
                    payment = PaymentForm(save.paymentMode, save.text, save.layout),
                )
            }

            ContractKind.TRADE -> {
                Contract(kind, give = save.give.orEmpty(), get = save.get.orEmpty())
            }

            ContractKind.REWARD -> {
                Contract(
                    kind,
                    get = save.get.orEmpty(),
                    reward = RewardNotice(earningCategory(save.category), save.popup, save.text),
                )
            }
        }

    private fun earningCategory(category: Int): Int =
        if (category > Contract.CATEGORY_AGENCIES) Contract.CATEGORY_GAMES else category

    private companion object {
        const val RECEIVE_TEXT_LIMIT = 60
        const val HIGHEST_REFUSED = 12
        val LAYOUTS = setOf(Contract.LAYOUT_GENERIC, Contract.LAYOUT_GAMES)
    }
}
