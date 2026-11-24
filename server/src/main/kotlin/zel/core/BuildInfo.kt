package zel.core

import java.util.Properties

object BuildInfo {
    const val PRODUCT: String = "Zellera"

    private val written: Properties =
        Properties().apply {
            BuildInfo::class.java.getResourceAsStream("/zel-build.properties")?.use(::load)
        }

    val NAME: String = written.getProperty("name", "zellera-server")
    val VERSION: String = written.getProperty("version", "unknown")

    /** The source revision the jar is built from; empty when the build had none. */
    val HASH: String = written.getProperty("hash", "").trim()
}
