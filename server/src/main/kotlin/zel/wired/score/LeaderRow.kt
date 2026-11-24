package zel.wired.score

data class LeaderRow(
    val rank: Int,
    val holder: LeaderHolder,
    val text: String?,
    val level: String? = null,
)
