package zel.wired.score

data class LeaderHolder(
    val userId: Int,
    val name: String,
    val figure: String,
    val value: Long?,
    val created: Long,
    val updated: Long,
)
