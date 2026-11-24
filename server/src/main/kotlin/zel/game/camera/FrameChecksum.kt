package zel.game.camera

internal object FrameChecksum {
    private const val SEAL_START = ",\"timestamp\" : "
    private val SEAL = Regex(",\"timestamp\" : (\\d{1,15}),\"checksum\" : (\\d{1,9}) }$")
    private val ROOM = Regex(",\"roomid\" : (\\d{1,10})")
    private val STATUS = Regex(",\"status\" : (\\d{1,6})$")

    private const val TICK_MS = 100L
    private const val STATUS_PERIOD = 23
    private const val SEED_TICK_FACTOR = 17
    private const val SEED_PERIOD = 1493
    private const val SUM_PERIOD = 255
    private const val BYTE_MASK = 0xFF
    private const val SUM_RANGE = 100
    private const val REST_OFFSET = 13
    private const val SEED_OFFSET = 29

    fun valid(text: String): Boolean {
        val cut = text.lastIndexOf(SEAL_START)
        val seal = if (cut < 0) null else SEAL.find(text, cut)?.takeIf { it.range.first == cut }
        val body = if (seal == null) "" else text.substring(0, cut)
        val room = ROOM.find(body)
        val status = STATUS.find(body)
        if (seal == null || room == null || status == null) return false
        val timestamp = seal.groupValues[1].toLong()
        val checksum = seal.groupValues[2].toLong()
        val ticks = timestamp / TICK_MS
        if (status.groupValues[1].toLong() < ticks % STATUS_PERIOD) return false
        val seed = (body.length + ticks * SEED_TICK_FACTOR) % SEED_PERIOD
        if (runningSum(body, seed, room.groupValues[1].toLong()) != timestamp % SUM_RANGE) return false
        val rest = checksum / (seed + SEED_OFFSET) - REST_OFFSET
        return checksum % (seed + SEED_OFFSET) == 0L && rest >= 0 && rest < SUM_RANGE
    }

    /** Each character is one byte (its low 8 bits). */
    private fun runningSum(
        text: String,
        seed: Long,
        roomId: Long,
    ): Long {
        var first = seed
        var second = roomId
        for (char in text) {
            first = (first + (char.code and BYTE_MASK)) % SUM_PERIOD
            second = (first + second) % SUM_PERIOD
        }
        return (first + second) % SUM_RANGE
    }
}
