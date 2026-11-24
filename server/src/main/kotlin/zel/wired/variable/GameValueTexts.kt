package zel.wired.variable

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.game.icons.IconRoster
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import javax.sql.DataSource

class GameValueTexts(
    private val site: () -> String,
    private val dataSource: DataSource,
    /** Database writer threads must never wait on the network. */
    private val background: (Runnable) -> Unit = { Thread.ofVirtual().name("value-texts").start(it) },
    private val clock: () -> Long = System::currentTimeMillis,
) : ValueTables {
    @Volatile
    private var tables: Map<String, ValueTexts> = emptyMap()

    private val loading = AtomicBoolean()

    @Volatile
    private var nextTry = 0L

    override fun of(id: String): ValueTexts? {
        if (id !in IDS) return null
        if (tables.isEmpty() && clock() >= nextTry && loading.compareAndSet(false, true)) background(Runnable(::load))
        return tables[id]
    }

    @Suppress("TooGenericExceptionCaught") // a failed read is logged and tried again after a pause
    private fun load() {
        try {
            val texts = parse(download())
            val icons = ValueTexts(icons(texts))
            tables =
                mapOf(
                    HAND_ITEM to ValueTexts(numbered(texts, HAND_ITEMS)),
                    EFFECT to ValueTexts(numbered(texts, EFFECTS)),
                    ICON to icons,
                    EVENT_ICON to icons,
                )
        } catch (e: Exception) {
            nextTry = clock() + RETRY_MILLIS
            log.warn("the variable value tables could not be read", e)
        } finally {
            loading.set(false)
        }
    }

    private fun download(): String {
        val request =
            HttpRequest
                .newBuilder(URI.create(site().trimEnd('/') + TEXTS_PATH))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build()
        val response = client().send(request, HttpResponse.BodyHandlers.ofInputStream())
        response.body().use { body ->
            check(response.statusCode() == HTTP_OK) { "external texts answered ${response.statusCode()}" }
            val bytes = body.readNBytes(MAX_BYTES + 1)
            check(bytes.size <= MAX_BYTES) { "external texts larger than $MAX_BYTES bytes" }
            return bytes.toString(Charsets.UTF_8)
        }
    }

    private fun client(): HttpClient =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS)).build()

    private fun icons(texts: Map<String, String>): List<Pair<Int, String>> =
        Sql(dataSource)
            .select("SELECT id, name FROM icons ORDER BY id")
            .rows { row ->
                texts["${IconRoster.textPrefix()}${row.getString(2)}_name"]?.takeIf { it.isNotEmpty() }?.let {
                    row.getInt(1) to
                        it
                }
            }.filterNotNull()

    private fun parse(body: String): Map<String, String> =
        body
            .lineSequence()
            .filter { it.indexOf('=') > 0 && !it.startsWith('#') }
            .associate { it.substringBefore('=') to it.substringAfter('=') }

    private fun numbered(
        texts: Map<String, String>,
        pattern: Regex,
    ): List<Pair<Int, String>> =
        texts
            .mapNotNull { (key, text) ->
                pattern
                    .matchEntire(key)
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull()
                    ?.takeIf { it >= 1 && text.isNotEmpty() }
                    ?.let { it to text }
            }.sortedBy { it.first }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GameValueTexts::class.java)
        const val TEXTS_PATH = "/gamedata/external_texts"
        const val TIMEOUT_SECONDS = 10L
        const val RETRY_MILLIS = 300_000L
        const val HTTP_OK = 200
        const val MAX_BYTES = 32 * 1024 * 1024
        const val HAND_ITEM = "-240"
        const val EFFECT = "-241"
        const val ICON = "-245"
        const val EVENT_ICON = "-452"
        val IDS = setOf(HAND_ITEM, EFFECT, ICON, EVENT_ICON)
        val HAND_ITEMS = Regex("handitem(\\d+)")
        val EFFECTS = Regex("fx_(\\d+)")
    }
}
