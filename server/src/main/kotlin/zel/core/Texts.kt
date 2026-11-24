package zel.core

import org.slf4j.LoggerFactory
import javax.sql.DataSource

/** Safe from any thread. */
class Texts(
    private val byKey: Map<String, String>,
) {
    fun text(
        key: String,
        fallback: String = "",
    ): String = byKey[key] ?: fallback.also { log.error("no text for key {}", key) }

    fun textOr(
        key: String,
        fallback: String,
    ): String = byKey[key] ?: fallback

    companion object {
        private val log = LoggerFactory.getLogger(Texts::class.java)

        fun load(dataSource: DataSource?): Texts =
            Texts(buildMap { NameValueTable("texts") { dataSource }.each(::put) })
    }
}
