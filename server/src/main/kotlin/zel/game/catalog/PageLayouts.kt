package zel.game.catalog

import org.slf4j.LoggerFactory
import zel.core.rules.rules
import zel.game.catalog.layouts.PageForm
import zel.game.catalog.layouts.PageLayoutRow
import java.util.concurrent.ConcurrentHashMap

/** The layouts the content defines, by lowercase code, minus those a switched-off game rule hides. */
typealias PageLayouts = ConcurrentHashMap<String, PageForm>

private val LOGGER = LoggerFactory.getLogger("zel.game.catalog.PageLayouts")

fun PageLayouts.install(rows: List<PageLayoutRow>) {
    val shown =
        rows
            .filter { ruleOn(it.needs) }
            .mapNotNull { row ->
                try {
                    row.code.lowercase() to row.form()
                } catch (e: IllegalArgumentException) {
                    LOGGER.error("catalog: layout {} is left out: {}", row.code, e.message)
                    null
                }
            }.toMap()
    keys.retainAll(shown.keys)
    putAll(shown)
}

private fun ruleOn(needs: PageLayoutRow.Needs): Boolean =
    when (needs) {
        PageLayoutRow.Needs.NONE -> true
        PageLayoutRow.Needs.RECYCLER -> rules.recycler
        PageLayoutRow.Needs.MARKETPLACE -> rules.marketplace
    }
