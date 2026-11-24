package zel.game.economy

import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

/** What the server does with a currency: [CREDITS] is the money column, [POINTS] and [PREMIUM] the two it pays out. */
enum class CurrencyRole {
    CREDITS,
    POINTS,
    PREMIUM,
    OTHER,
    ;

    companion object {
        fun stored(code: String?): CurrencyRole =
            entries.firstOrNull { it.name.equals(code, ignoreCase = true) } ?: OTHER
    }
}

/** One currency the content defines; [points] marks the activity points a player holds besides credits. */
class Currency(
    val id: Int,
    val code: String,
    val name: String,
    val points: Boolean,
    val role: CurrencyRole = CurrencyRole.OTHER,
)

/** The currencies by id, code and role, as the content defines them; -1 is the credits, the money column. */
object Currencies {
    /** What a role has when the content defines none: no ledger accepts it. */
    const val MISSING = -2

    private val byId = AtomicReference<Map<Int, Currency>>(emptyMap())

    fun install(all: List<Currency>) {
        byId.set(all.associateBy { it.id })
        listOf(CurrencyRole.POINTS, CurrencyRole.PREMIUM).filter { id(it) == MISSING }.forEach {
            LOGGER.warn("currencies: no currency has the role {}; what pays in it fails", it.name.lowercase())
        }
    }

    private val LOGGER = LoggerFactory.getLogger(Currencies::class.java)

    fun name(id: Int): String = byId.get()[id]?.name ?: "Points type $id"

    fun idOf(code: String): Int? =
        byId
            .get()
            .values
            .firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
            ?.id

    fun id(role: CurrencyRole): Int =
        byId
            .get()
            .values
            .firstOrNull { it.role == role }
            ?.id ?: MISSING

    fun pointIds(): List<Int> =
        byId
            .get()
            .values
            .filter { it.points }
            .map { it.id }
            .sorted()
}
