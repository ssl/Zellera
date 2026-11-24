package zel.core.config

class ConfigRegistry(
    keys: List<ConfigKey>,
) {
    /** A value its key cannot be read from. */
    data class Unusable(
        val key: String,
        val value: String,
        val problem: String,
    )

    private val byName: Map<String, ConfigKey> = keys.sortedBy(ConfigKey::name).associateBy(ConfigKey::name)

    init {
        require(byName.size == keys.size) {
            "config keys listed twice: ${keys.groupBy(ConfigKey::name).filterValues { it.size > 1 }.keys}"
        }
    }

    fun names(): Set<String> = byName.keys

    /** Only the listed keys are looked at: the settings table holds many more, each read where it is used. */
    fun unusableIn(values: Map<String, String>): List<Unusable> =
        byName.values.mapNotNull { key ->
            val value = values[key.name] ?: return@mapNotNull null
            key.kind.problemWith(value)?.let { Unusable(key.name, value, it) }
        }

    companion object {
        val shipped: ConfigRegistry by lazy { ConfigRegistry(ConfigKeys.all()) }
    }
}
