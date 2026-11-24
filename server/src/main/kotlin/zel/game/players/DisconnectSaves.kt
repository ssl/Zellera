package zel.game.players

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicReference

/** A disconnect's save, claimable once: by the writer, or by a login that arrives first and runs it itself. */
class DisconnectSaves(
    private val writer: Executor,
) {
    private val open = ConcurrentHashMap<Int, Save>()

    inner class Save internal constructor(
        val playerId: Int,
    ) {
        private val unclaimed = AtomicReference<Runnable?>()
        private val done = CountDownLatch(1)

        /** A writer that refuses the work gets it run here and now. */
        fun queue(work: Runnable) {
            unclaimed.set(work)
            try {
                writer.execute(::claim)
            } catch (refused: RejectedExecutionException) {
                log.debug("writer refuses the save of player {}; running it inline", playerId, refused)
                claim()
            }
        }

        fun cancel() = finish()

        internal fun claim() {
            val work = unclaimed.getAndSet(null) ?: return
            try {
                work.run()
            } catch (
                // any failure still has to release the login waiting on this save
                @Suppress("TooGenericExceptionCaught") failure: Exception,
            ) {
                log.error("the disconnect save of player {} failed", playerId, failure)
            } finally {
                finish()
            }
        }

        internal fun awaitDone() {
            claim()
            done.await()
        }

        private fun finish() {
            open.remove(playerId, this)
            done.countDown()
        }
    }

    /** A player can only have one save open at a time. */
    fun begin(playerId: Int): Save {
        require(playerId > 0) { "player id must be positive, not $playerId" }
        val save = Save(playerId)
        check(open.putIfAbsent(playerId, save) == null) { "player $playerId already has a disconnect save open" }
        return save
    }

    fun await(playerId: Int): Boolean {
        open[playerId]?.awaitDone()
        return true
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(DisconnectSaves::class.java)
    }
}
