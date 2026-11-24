package zel.wired.variable

data class HoldersPage(
    val variableId: String,
    val page: Int,
    val pageSize: Int,
    val userTypeFilter: Int,
    val sortTypeFilter: Int,
)
