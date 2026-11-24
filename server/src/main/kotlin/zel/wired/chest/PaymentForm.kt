package zel.wired.chest

data class PaymentForm(
    val mode: Int = Contract.MODE_ANYTHING,
    val receiveText: String = "",
    val layout: String = Contract.LAYOUT_GENERIC,
)
