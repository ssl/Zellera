package zel.wired.usage

data class UsageAllowance(
    val usage: Double,
    val furniVariables: Int,
    val userVariables: Int,
    val roomVariables: Int,
)
