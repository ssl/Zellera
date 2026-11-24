package zel.wired.variable.box

internal data class OperandShape(
    val fromVariable: Boolean,
    val constant: Long,
    val referenceTarget: Int,
    val sameHolder: Boolean,
    val target: Int,
)
