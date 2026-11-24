package zel.wired.variable.addon

import zel.protocol.generated.WiredWebApiKeyResultOut
import zel.protocol.generated.WiredWebApiKeyResultOutCodec
import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.room.tick.Audience
import zel.room.tick.Lane
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class WebApiBoxes(
    private val store: WebApiKeyStore,
    secret: String,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val random = SecureRandom()
    private val sealKey: ByteArray? =
        secret.takeIf { it.isNotEmpty() }?.let { sha256("wired-api-keys:$it") }

    private val pending = ConcurrentHashMap<Int, MutableMap<String, MutableSet<String>>>()

    fun generate(
        scope: RoomScope,
        playerId: Int,
        boxId: Int,
        isRead: Boolean,
    ) {
        val box = scope.room.furni[boxId] ?: return
        if (Interactions.of(box.definition) != AddonCodes.WEB_API || box.ownerId != playerId) return
        val key = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(KEY_BYTES).also(random::nextBytes))
        pending.computeIfAbsent(boxId) { HashMap() }.computeIfAbsent(kind(isRead)) { HashSet() }.add(key)
        scope.out.send(
            Lane.Now,
            Audience.Player(playerId),
            WiredWebApiKeyResultOutCodec,
            WiredWebApiKeyResultOut(boxId, isRead, key),
        )
    }

    private fun kind(isRead: Boolean) = if (isRead) READ else WRITE

    fun opened(box: RoomFurni): String {
        pending.remove(box.id)
        val keys = store.keys(box.id)
        if (keys.isEmpty()) return ""
        return "${shown(keys[READ])}\t${shown(keys[WRITE])}"
    }

    private fun shown(key: StoredApiKey?): String =
        when {
            key == null -> ""
            else -> key.sealed?.let(::unseal) ?: masked(key)
        }

    private fun masked(key: StoredApiKey) = key.hint + "*".repeat(KEY_LENGTH - key.hint.length)

    fun apply(
        room: Room,
        box: RoomFurni,
        text: String,
    ): Boolean {
        val rows = text.split('\t')
        val saved = store.keys(box.id)
        val waiting = pending[box.id].orEmpty()
        val decided =
            listOf(READ, WRITE).withIndex().associate { (index, kind) ->
                kind to decide(rows.getOrElse(index) { "" }, saved[kind], waiting[kind].orEmpty())
            }
        if (decided.values.any { it == Decision.Refused }) return false
        decided.forEach { (kind, decision) -> carryOut(room, box, kind, decision, saved[kind]) }
        return true
    }

    private fun decide(
        sent: String,
        saved: StoredApiKey?,
        waiting: Set<String>,
    ): Decision =
        when {
            sent.isEmpty() -> Decision.Clear
            saved != null && (isSame(sent, saved) || sent == masked(saved)) -> Decision.Keep
            sent in waiting -> Decision.Save(sent)
            else -> Decision.Refused
        }

    private fun isSame(
        sent: String,
        saved: StoredApiKey,
    ) = MessageDigest.isEqual(sha256(sent), saved.hash)

    private fun carryOut(
        room: Room,
        box: RoomFurni,
        kind: String,
        decision: Decision,
        saved: StoredApiKey?,
    ) {
        if (decision == Decision.Clear && saved != null) store.delete(box.id, kind)
        if (decision is Decision.Save) {
            store.save(box.id, kind, room.info.id, box.ownerId, stored(decision.key), clock())
            pending[box.id]?.get(kind)?.remove(decision.key)
        }
    }

    private fun stored(key: String) = StoredApiKey(sha256(key), key.take(HINT), seal(key))

    fun hasWriteKey(box: RoomFurni): Boolean = store.keys(box.id).containsKey(WRITE)

    fun pickedUp(box: RoomFurni) {
        pending.remove(box.id)
        store.deleteAll(box.id)
    }

    private fun seal(key: String): ByteArray? {
        val secret = sealKey ?: return null
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(CIPHER)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(secret, AES), GCMParameterSpec(TAG_BITS, nonce))
        return nonce + cipher.doFinal(key.toByteArray(Charsets.US_ASCII))
    }

    private fun unseal(sealed: ByteArray): String? {
        val secret = sealKey ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(CIPHER)
            val spec = GCMParameterSpec(TAG_BITS, sealed, 0, NONCE_BYTES)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(secret, AES), spec)
            String(cipher.doFinal(sealed, NONCE_BYTES, sealed.size - NONCE_BYTES), Charsets.US_ASCII)
        }.getOrNull()
    }

    private fun sha256(text: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.US_ASCII))

    private sealed interface Decision {
        data object Clear : Decision

        data object Keep : Decision

        data object Refused : Decision

        data class Save(
            val key: String,
        ) : Decision
    }

    companion object {
        const val READ = "read"
        const val WRITE = "write"
        private const val KEY_BYTES = 32
        private const val KEY_LENGTH = 43
        private const val HINT = 4
        private const val NONCE_BYTES = 12
        private const val TAG_BITS = 128
        private const val AES = "AES"
        private const val CIPHER = "AES/GCM/NoPadding"
    }
}
