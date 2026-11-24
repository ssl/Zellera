package zel.room.furni.behaviour.state

object CustomValues {
    /** The width of the furni row's state column. */
    const val MAX_STORED = 1024

    fun read(
        stored: String,
        defaults: Map<String, String>,
    ): LinkedHashMap<String, String> {
        val values = LinkedHashMap(defaults)
        stored.split(';').forEach { pair ->
            val separator = pair.indexOf('=')
            if (separator > 0) values[pair.substring(0, separator)] = pair.substring(separator + 1)
        }
        return values
    }

    fun write(values: Map<String, String>): String = values.entries.joinToString("") { "${it.key}=${it.value};" }
}
