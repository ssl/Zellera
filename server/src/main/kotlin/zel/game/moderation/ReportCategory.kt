package zel.game.moderation

data class ReportCategory(
    val name: String,
    val topics: List<ReportTopic>,
)
