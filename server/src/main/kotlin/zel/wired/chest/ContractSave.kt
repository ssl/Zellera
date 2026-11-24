package zel.wired.chest

data class ContractSave(
    val id: Int,
    val type: Int,
    val give: List<List<Term>>?,
    val get: List<Term>?,
    val paymentMode: Int,
    val text: String,
    val layout: String,
    val category: Int,
    val popup: Boolean,
    val broken: Boolean,
)
