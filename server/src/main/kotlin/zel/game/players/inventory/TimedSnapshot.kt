package zel.game.players.inventory

import java.util.concurrent.atomic.AtomicLong

/** Loaded at most once per [ttlMs]; after [invalidate] again once [minGapMs] has passed since the last load. */
internal class TimedSnapshot<T : Any>(
    private val ttlMs: Long,
    empty: T,
    private val minGapMs: Long = 0L,
    private val loader: () -> T?,
) {
    private val generation = AtomicLong()

    @Volatile
    private var value: T = empty

    @Volatile
    private var expiresAt = 0L

    @Volatile
    private var loadedAt = NEVER

    fun get(now: Long = System.currentTimeMillis()): T {
        if (expiresAt > now) return value
        synchronized(this) {
            if (expiresAt > now) return value
            val loadingAt = generation.get()
            loader()?.let { value = it }
            loadedAt = now
            expiresAt = now + ttlMs
            if (generation.get() != loadingAt) expiresAt = now + minGapMs
            return value
        }
    }

    fun invalidate() {
        generation.incrementAndGet()
        expiresAt = minOf(expiresAt, loadedAt + minGapMs)
    }

    private companion object {
        const val NEVER = Long.MIN_VALUE / 2
    }
}
