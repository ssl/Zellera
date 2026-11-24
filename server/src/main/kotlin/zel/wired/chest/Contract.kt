package zel.wired.chest

data class Contract(
    val kind: ContractKind,
    val give: List<List<Term>> = emptyList(),
    val get: List<Term> = emptyList(),
    val payment: PaymentForm = PaymentForm(),
    val reward: RewardNotice = RewardNotice(),
) {
    val isDonation: Boolean get() = kind == ContractKind.PAYMENT && payment.mode == MODE_ANYTHING

    val isUnreadable: Boolean get() = kind == ContractKind.PAYMENT && payment.mode == MODE_UNREADABLE

    fun madeUnreadable(): Contract = copy(payment = payment.copy(mode = MODE_UNREADABLE))

    val paysAtOnce: Boolean get() = kind == ContractKind.REWARD || (give.isEmpty() && !isDonation && !isUnreadable)

    companion object {
        const val MAX_RULES = 8
        const val MODE_ANYTHING = 0
        const val MODE_SPECIFIC = 1
        const val MODE_UNREADABLE = 2
        const val LAYOUT_GENERIC = "generic"
        const val LAYOUT_GAMES = "games"
        const val CATEGORY_GAMES = 11
        const val CATEGORY_AGENCIES = 13
    }
}
