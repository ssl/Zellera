package zel.protocol.typed

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.netty.channel.Channel
import io.netty.util.AttributeKey
import org.slf4j.LoggerFactory
import zel.protocol.generated.ClientHelloInCodec
import zel.protocol.generated.MessageRegistry

private const val HELLO_PREFIX = "zellera-"

/** The hello's wire id, the same in every version: it is read before the version is known. */
const val HELLO_WIRE = 1
private const val MAX_NUMBER_DIGITS = 6

/** The wire ids one client version gives the messages. */
class ClientVersion(
    val number: Int,
    incoming: Map<String, Int>,
    private val outgoing: Map<String, Int>,
) {
    val name: String = HELLO_PREFIX + number

    private val incomingNames: Map<Int, String> =
        incoming.entries.associate { (message, id) -> id to message } + (HELLO_WIRE to ClientHelloInCodec.name)

    init {
        require(incomingNames.size == incoming.size + 1) {
            "client version $name gives two client messages one id, or one the hello's"
        }
        require(outgoing.values.toSet().size == outgoing.size) {
            "client version $name gives two server messages one id"
        }
    }

    fun incoming(wire: Int): String? = incomingNames[wire]

    fun outgoing(message: String): Int? = outgoing[message]
}

/**
 * The client versions the game speaks, from protocol/clients.json, by number. A client's hello names one:
 * `zellera-<number>`, then nothing or a dash and any text.
 */
object ClientVersions {
    private const val FILE = "/protocol/clients.json"
    private const val INCOMING = "incoming"
    private const val OUTGOING = "outgoing"
    private val log = LoggerFactory.getLogger(ClientVersions::class.java)
    private val SPOKEN: AttributeKey<ClientVersion> = AttributeKey.valueOf("zelClientVersion")

    val all: Map<Int, ClientVersion> = read()

    /** The version number a hello names; null when it is not such a hello. */
    fun number(hello: String?): Int? {
        val rest = hello?.takeIf { it.startsWith(HELLO_PREFIX) }?.substring(HELLO_PREFIX.length) ?: return null
        val digits = rest.substringBefore('-')
        return digits.takeIf { it.length in 1..MAX_NUMBER_DIGITS && it.all { c -> c in '0'..'9' } }?.toInt()
    }

    fun named(hello: String?): ClientVersion? = number(hello)?.let { all[it] }

    /** The version a first message names, when that message is the hello. */
    fun saidBy(
        wire: Int,
        hello: String,
    ): ClientVersion? = if (wire == HELLO_WIRE) named(hello) else null

    fun of(channel: Channel): ClientVersion? = channel.attr(SPOKEN).get()

    fun accept(
        channel: Channel,
        version: ClientVersion,
    ) {
        channel.attr(SPOKEN).set(version)
    }

    private fun read(): Map<Int, ClientVersion> {
        val stream = checkNotNull(ClientVersions::class.java.getResourceAsStream(FILE)) { "$FILE is missing" }
        val root = stream.reader(Charsets.UTF_8).use { JsonParser.parseReader(it).asJsonObject }
        return root
            .entrySet()
            .associate { (key, tables) ->
                val number = requireNotNull(key.toIntOrNull()) { "$FILE: version $key is not a number" }
                val version = tables.asJsonObject
                val incoming = ids(version.getAsJsonObject(INCOMING))
                number to ClientVersion(number, incoming, ids(version.getAsJsonObject(OUTGOING)))
            }.also(::warnAboutMissing)
    }

    private fun warnAboutMissing(versions: Map<Int, ClientVersion>) {
        for (version in versions.values) {
            val missing = MessageRegistry.outgoing.map { it.name }.filter { version.outgoing(it) == null }
            if (missing.isNotEmpty()) log.warn("client version {} has no id for {}", version.name, missing)
        }
    }

    private fun ids(table: JsonObject): Map<String, Int> =
        table.entrySet().associate { (message, id) -> message to id.asInt }
}
