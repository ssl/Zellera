package zel.game.items

import zel.core.Configuration
import org.slf4j.LoggerFactory

class FurniTitles(
    private val location: FurnidataLocation = FurnidataLocation.Absent("no furnidata source configured"),
    private val sizeCap: Long = SIZE_CAP,
) {
    @Volatile
    private var titles: Map<String, String> = emptyMap()

    fun load() {
        when (location) {
            is FurnidataLocation.Absent -> {
                log.info("furni titles off: {}", location.reason)
            }

            is FurnidataLocation.Found -> {
                log.info("furni titles from the {}", location.label)
                val fresh = learn(FurnidataReader(location.path, sizeCap).read())
                log.info("furni titles: {} known, {} new or renamed", titles.size, fresh.size)
            }
        }
    }

    fun learn(furnitypes: List<FurnidataEntry>): List<FurnidataEntry> {
        val tidied = furnitypes.map { it.copy(name = tidy(it.name)) }
        val known = titles
        val firstOfBase = tidied.asReversed().associate { baseOf(it.classname) to it.name }
        titles = known + firstOfBase.filterKeys { it !in known } + tidied.associate { keyOf(it.classname) to it.name }
        return tidied.filter { known[keyOf(it.classname)] != it.name }
    }

    /**
     * Each class, colour variants too (`chair*2`), has its own furnidata name; a variant the furnidata does not list
     * finds its base class, a base class listed only through its variants their first one.
     */
    fun titleOf(classname: String): String? = titles[keyOf(classname)] ?: titles[baseOf(classname)]

    private fun keyOf(classname: String): String = classname.trim().lowercase()

    private fun baseOf(classname: String): String = keyOf(classname.substringBefore('*'))

    companion object {
        private val log = LoggerFactory.getLogger(FurniTitles::class.java)
        private const val SIZE_CAP = 64L * 1024 * 1024
        private const val TITLE_LIMIT = 256
        private val UNWANTED = Regex("[\\p{Cc}%]")

        fun from(config: Configuration): FurniTitles? {
            if (!config.getBoolean("furni.data.names.are_read", true)) return null
            val cap = config.getInt("furni.data.max.bytes", SIZE_CAP.toInt()).toLong()
            return FurniTitles(FurnidataLocation.configured(config), cap).also(FurniTitles::load)
        }

        /** No control characters, no `%` (text templates), at most 256 characters. */
        fun tidy(title: String?): String = UNWANTED.replace(title.orEmpty(), "").take(TITLE_LIMIT)
    }
}
