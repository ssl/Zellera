package zel.game

data class ServerInfo(
    val version: String,
    val build: () -> String,
    val uptimeSeconds: () -> Int,
)
