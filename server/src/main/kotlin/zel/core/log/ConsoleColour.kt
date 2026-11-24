package zel.core.log

import java.util.concurrent.atomic.AtomicBoolean

/** Whether console lines carry colour: `server.console.colour` says `on` or `off`, anything else asks the terminal. */
object ConsoleColour {
    const val SETTING = "server.console.colour"

    private val decided = AtomicBoolean(decide("", System.getenv(), attachedToTerminal()))

    val on: Boolean get() = decided.get()

    fun follow(setting: String) {
        decided.set(decide(setting, System.getenv(), attachedToTerminal()))
    }

    /** Output that goes to a file or a pipe stays plain, so the service's log never holds escape codes. */
    fun decide(
        setting: String,
        environment: Map<String, String>,
        terminal: Boolean,
    ): Boolean =
        when (setting.trim().lowercase()) {
            "on" -> true
            "off" -> false
            else -> terminal && environment["NO_COLOR"].isNullOrEmpty() && environment["TERM"] != "dumb"
        }

    private fun attachedToTerminal(): Boolean = System.console()?.isTerminal == true
}
