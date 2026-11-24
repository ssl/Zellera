package zel.wired.link

data class LinkRow(
    val networkId: Int,
    val name: String,
    val count: Int,
    val linkedHere: Boolean,
)
