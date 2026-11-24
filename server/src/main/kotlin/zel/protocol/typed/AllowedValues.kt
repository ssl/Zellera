package zel.protocol.typed

import zel.net.clients.ClientConnection
import zel.protocol.generated.AllowedField
import zel.protocol.generated.AllowedRule

class AllowedValues(
    private val lookups: AllowedLookups = AllowedLookups.NONE,
    private val dropping: () -> Boolean = { false },
    private val runLater: (() -> Unit) -> Unit = { it() },
) {
    class Verdict(
        val runHandler: Boolean,
        val after: (() -> Unit)?,
    )

    fun <T> check(
        client: ClientConnection,
        rows: List<AllowedField<T>>,
        message: T,
    ): Verdict {
        if (rows.isEmpty()) return GO
        val bad = ArrayList<String>(0)
        val unknown = ArrayList<String>(0)
        val later = ArrayList<LaterCheck>(0)
        for (row in rows) {
            val value = row.read(message) ?: continue
            val found = checked(client, row, value, later) ?: continue
            (if (row.holdsId) unknown else bad) += reason(row, found)
        }
        if (unknown.isNotEmpty()) {
            client.warnings.unknownId(
                client.warnings.current,
                unknown.joinToString("; ").take(MAX_REASON),
            )
        }
        if (bad.isNotEmpty()) client.warnings.refused(bad.joinToString("; ").take(MAX_REASON))
        val after = if (later.isEmpty()) null else laterCheck(client, later)
        val runHandler = bad.isEmpty() || !dropping()
        return if (runHandler && after == null) GO else Verdict(runHandler, after)
    }

    private fun <T> checked(
        client: ClientConnection,
        row: AllowedField<T>,
        value: Any,
        later: MutableList<LaterCheck>,
    ): Any? {
        val rule = row.rule
        if (rule is AllowedRule.Lookup && rule.whole) {
            return value.takeUnless { lookups.allows(rule.name, client, it) ?: true }
        }
        val items = if (value is List<*>) value.take(MAX_LIST_ITEMS).filterNotNull() else listOf(value)
        for (item in items) {
            if (row.holdsId && isNoId(item)) continue
            when (allows(rule, client, item)) {
                true -> Unit
                false -> return item
                null -> later += LaterCheck(row, (rule as AllowedRule.Lookup).name, item)
            }
        }
        return null
    }

    private fun laterCheck(
        client: ClientConnection,
        checks: List<LaterCheck>,
    ): () -> Unit {
        val packet = client.warnings.current
        return {
            runLater {
                val unknown =
                    checks
                        .filterNot { lookups.laterAllows(it.lookup, it.value) }
                        .distinctBy { it.row.label }
                        .joinToString("; ") { reason(it.row, it.value) }
                if (unknown.isNotEmpty()) client.warnings.unknownId(packet, unknown.take(MAX_REASON))
            }
        }
    }

    private fun isNoId(value: Any): Boolean = (value is Number && value.toLong() <= 0) || value == ""

    private fun allows(
        rule: AllowedRule,
        client: ClientConnection,
        value: Any,
    ): Boolean? =
        when (rule) {
            is AllowedRule.Range -> value !is Number || value.toLong() in rule.min..rule.max
            is AllowedRule.Values -> plain(value) in rule.values
            is AllowedRule.Lookup -> lookups.allows(rule.name, client, value)
        }

    private fun plain(value: Any): Any = if (value is Number) value.toLong() else value

    private fun <T> reason(
        row: AllowedField<T>,
        value: Any,
    ): String = "${row.label}: ${if (row.holdsId) "unknown id" else "not allowed"} (${shown(value)})"

    private fun shown(value: Any): String =
        value
            .toString()
            .take(MAX_VALUE)
            .replace(CONTROL, "?")

    private class LaterCheck(
        val row: AllowedField<*>,
        val lookup: String,
        val value: Any,
    )

    private companion object {
        val GO = Verdict(runHandler = true, after = null)
        const val MAX_REASON = 255
        const val MAX_VALUE = 60
        const val MAX_LIST_ITEMS = 20
        val CONTROL = Regex("""\p{Cntrl}""")
    }
}
