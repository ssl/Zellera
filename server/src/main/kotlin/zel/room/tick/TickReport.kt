package zel.room.tick

import org.slf4j.LoggerFactory

interface TickReport {
    fun failed(
        roomId: Int,
        error: Throwable,
    )

    fun slow(
        roomId: Int,
        millis: Long,
    )

    companion object {
        private val log = LoggerFactory.getLogger(TickReport::class.java)

        val Log: TickReport =
            object : TickReport {
                override fun failed(
                    roomId: Int,
                    error: Throwable,
                ) = log.error("room {}: tick failed", roomId, error)

                override fun slow(
                    roomId: Int,
                    millis: Long,
                ) = log.warn("room {}: slow tick, {} ms", roomId, millis)
            }
    }
}
