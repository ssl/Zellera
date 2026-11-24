package zel.wired.variable.addon

class BridgeRefusal(
    val status: Int,
    val code: String,
) : RuntimeException(code)
