package zel.wired.chest

import zel.util.MILLIS_PER_SECOND
import java.util.concurrent.atomic.AtomicLong

class ChestLog(
    store: ChestStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val next = AtomicLong(store.lastTransactionId())

    fun nextId(): Long = next.incrementAndGet()

    fun seconds(): Int = (clock() / MILLIS_PER_SECOND).toInt()
}
