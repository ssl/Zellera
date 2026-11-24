package zel.wired.chest

enum class ContractKind(
    val wire: Int,
    val code: String,
) {
    PAYMENT(0, "wf_contract_payment"),
    TRADE(1, "wf_contract_trade"),
    REWARD(2, "wf_contract_reward"),
    ;

    companion object {
        fun fromCode(code: String): ContractKind? = entries.firstOrNull { it.code == code }
    }
}
