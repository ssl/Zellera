package zel.wired.variable

data class PermanentWrite(
    val entityType: Int,
    val userId: Int,
    val variableId: String,
    val value: Int,
    val action: Int,
)
