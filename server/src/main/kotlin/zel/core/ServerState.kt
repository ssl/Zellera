package zel.core

interface ServerState {
    val isReady: Boolean
    val isShuttingDown: Boolean
    val uptimeSeconds: Int
}
