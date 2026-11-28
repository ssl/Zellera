package zel.security

import com.google.gson.JsonParser
import io.netty.util.AttributeKey
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.core.Configuration
import zel.net.clients.ClientConnection
import zel.net.server.proxy.ClientAddress
import zel.protocol.Outbound
import zel.protocol.generated.CatalogPageExpirationOut
import zel.protocol.generated.CatalogPageExpirationOutCodec
import zel.protocol.generated.EventLogIn
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.HexFormat
import java.util.Random
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class ClientCheck(
    private val config: Configuration,
    private val suspicions: SuspicionLog,
    private val schedule: (Runnable, Long) -> Unit,
    private val random: Random = SecureRandom(),
    private val send: (ClientConnection, Outbound<*>) -> Unit = { client, message -> client.send(message) },
) {
    data class Question(
        val offset: Int,
        val length: Int,
        val nonce: String,
    )

    private class Pending(
        val question: Question,
        val expected: String,
    )

    private val warned = AtomicBoolean()

    fun loggedIn(client: ClientConnection) {
        if (!config.getBoolean(ON_KEY, false)) return
        val delay = FIRST_ASK_MILLIS + random.nextLong(LAST_ASK_MILLIS - FIRST_ASK_MILLIS + 1)
        schedule(Runnable { ask(client) }, delay)
    }

    fun ask(client: ClientConnection) {
        if (client.isClosed || !config.getBoolean(ON_KEY, false)) return
        val launcher = served() ?: return
        if (!sameRelease(client.releaseName, launcher.release)) {
            log.debug("client check skipped: hello '{}', served '{}'", client.releaseName, launcher.release)
            return
        }
        val size = runCatching { Files.size(launcher.script) }.getOrNull() ?: return
        if (size < MIN_LENGTH || size > Int.MAX_VALUE) return
        val question = question(size.toInt())
        val expected = runCatching { answer(read(launcher.script, question), question.nonce) }.getOrNull() ?: return
        val pending = Pending(question, expected)
        client.channel.attr(PENDING).set(pending)
        send(
            client,
            Outbound(
                CatalogPageExpirationOutCodec,
                CatalogPageExpirationOut(question.offset, question.nonce, question.length, MARK),
            ),
        )
        schedule(Runnable { timeOut(client, pending) }, ANSWER_MILLIS)
    }

    fun answered(
        client: ClientConnection,
        m: EventLogIn,
    ) {
        if (m.category != CATEGORY || m.eventType != TYPE) return
        val pending = client.channel.attr(PENDING).get() ?: return
        if (pending.question.nonce != m.extraString) return
        if (!client.channel.attr(PENDING).compareAndSet(pending, null)) return
        val given =
            m.action
                .trim()
                .lowercase()
                .toByteArray(Charsets.US_ASCII)
        if (MessageDigest.isEqual(given, pending.expected.toByteArray(Charsets.US_ASCII))) {
            log.debug("client check passed for player {}", client.player?.profile?.id)
        } else {
            record(client, Suspicion.CLIENT_WRONG, "wrong")
        }
    }

    private fun timeOut(
        client: ClientConnection,
        pending: Pending,
    ) {
        if (client.channel.attr(PENDING).compareAndSet(pending, null)) record(client, Suspicion.CLIENT_SILENT, "none")
    }

    private fun record(
        client: ClientConnection,
        kind: Suspicion,
        detail: String,
    ) = suspicions.record(
        client.player?.profile?.id,
        ClientAddress.of(client.channel),
        client.device?.usable("id"),
        kind,
        detail,
    )

    private fun question(size: Int): Question {
        val length = MIN_LENGTH + random.nextInt(minOf(MAX_LENGTH, size) - MIN_LENGTH + 1)
        val offset = random.nextInt(size - length + 1)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        return Question(offset, length, HexFormat.of().formatHex(nonce))
    }

    private data class Launcher(
        val script: Path,
        val release: String,
    )

    private fun served(): Launcher? {
        val folder = config.getValue(FOLDER_KEY, "").trim()
        val found =
            folder.takeIf(String::isNotEmpty)?.let { dir ->
                runCatching {
                    val root = Path.of(dir)
                    val manifest = JsonParser.parseString(Files.readString(root.resolve(MANIFEST))).asJsonObject
                    val script = root.resolve(manifest.get("script").asString).normalize()
                    check(script.startsWith(root.normalize())) { "the script is outside the client folder" }
                    Launcher(script, manifest.get("release").asString)
                }.getOrNull()
            }
        if (found == null && warned.compareAndSet(false, true)) {
            log.warn("client check: no served client at '{}' ({})", folder, FOLDER_KEY)
        }
        return found
    }

    companion object {
        const val ON_KEY = "security.client_check.is_on"
        const val FOLDER_KEY = "security.client_check.folder"
        const val CATEGORY = "zel"
        const val TYPE = "integrity"
        const val MARK = "zel.integrity"
        const val MIN_LENGTH = 4096
        const val MAX_LENGTH = 65536
        const val FIRST_ASK_MILLIS = 30_000L
        const val LAST_ASK_MILLIS = 300_000L
        const val ANSWER_MILLIS = 20_000L
        private const val NONCE_BYTES = 16
        private const val MANIFEST = "client.json"
        private val PENDING: AttributeKey<Pending> = AttributeKey.valueOf("client-check")
        private val log: Logger = LoggerFactory.getLogger(ClientCheck::class.java)

        fun sameRelease(
            hello: String,
            served: String,
        ): Boolean = served.isNotEmpty() && (hello == served || hello.endsWith("-$served"))

        fun answer(
            bytes: ByteArray,
            nonce: String,
        ): String {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(nonce.toByteArray(Charsets.UTF_8), "HmacSHA256"))
            return HexFormat.of().formatHex(mac.doFinal(bytes))
        }

        fun read(
            script: Path,
            question: Question,
        ): ByteArray =
            FileChannel.open(script, StandardOpenOption.READ).use { channel ->
                val buffer = ByteBuffer.allocate(question.length)
                var at = question.offset.toLong()
                while (buffer.hasRemaining()) {
                    val got = channel.read(buffer, at)
                    check(got >= 0) { "the launcher ended early" }
                    at += got
                }
                buffer.array()
            }
    }
}
