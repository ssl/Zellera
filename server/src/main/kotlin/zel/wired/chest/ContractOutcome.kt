package zel.wired.chest

sealed interface ContractOutcome {
    data object Broken : ContractOutcome

    data object Unreadable : ContractOutcome

    data class Refused(
        val code: String,
    ) : ContractOutcome

    data class Saved(
        val contract: Contract,
    ) : ContractOutcome
}
