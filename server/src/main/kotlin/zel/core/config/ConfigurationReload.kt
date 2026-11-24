package zel.core.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.core.Configuration

class ConfigurationReload(
    private val configuration: Configuration,
) {
    fun activate() {
        configuration.onReload(::apply)
        apply()
    }

    internal fun apply() {
        run("the game settings", ::bindAll)
    }

    private fun bindAll() {
        GameSettingBindings(configuration).applyAll()
    }

    // One failing part must not stop the others.
    @Suppress("TooGenericExceptionCaught")
    private fun run(
        name: String,
        action: () -> Unit,
    ) {
        try {
            action()
        } catch (e: Exception) {
            log.error("{} kept its old settings, the reload failed there", name, e)
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ConfigurationReload::class.java)
    }
}
