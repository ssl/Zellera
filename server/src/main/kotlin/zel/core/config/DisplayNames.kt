package zel.core.config

import java.util.concurrent.atomic.AtomicReference

/** The names players read for the build pass and the club, as the content defines them in `display_names`. */
object DisplayNames {
    private const val BUILD_PASS = "build_pass"
    private const val CLUB = "club"

    private val names = AtomicReference<Map<String, String>>(emptyMap())

    fun install(all: Map<String, String>) {
        names.set(all)
    }

    val buildPass: String get() = of(BUILD_PASS)
    val club: String get() = of(CLUB)

    private fun of(code: String): String = names.get()[code]?.takeIf { it.isNotBlank() } ?: code
}
