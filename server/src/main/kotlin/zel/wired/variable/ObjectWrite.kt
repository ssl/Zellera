package zel.wired.variable

data class ObjectWrite(
    val target: Int,
    val objectId: Int,
    val variableId: String,
    val value: Int,
    val action: Int,
)
