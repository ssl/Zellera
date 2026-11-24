package zel.wired.variable

data class VariableChange(
    val variableId: String,
    val boxId: Int,
    val holder: Holder,
    val origin: Int,
    val type: Int,
    val oldValue: Long?,
    val newValue: Long?,
)
