package zel.game.bots

class BotSpeech(
    lines: List<String>,
    auto: Boolean,
    random: Boolean,
    delaySeconds: Short,
) {
    private val stored = ArrayList(lines)

    var auto: Boolean = auto

    var random: Boolean = random

    var delaySeconds: Int = delaySeconds.toInt()
        set(value) {
            val most = minOf(BotLimits.maxChatDelay, Short.MAX_VALUE.toInt())
            field = value.coerceAtMost(most).coerceAtLeast(BotLimits.minChatDelay)
        }

    val lines: List<String> get() = synchronized(stored) { stored.toList() }

    fun replaceLines(lines: List<String>) {
        synchronized(stored) {
            stored.clear()
            stored.addAll(lines)
        }
    }

    fun configuration(): String {
        val lines = lines
        if (lines.all { it.isEmpty() }) return ""
        return lines.joinToString("") { "$it\r" } + ";#;$auto;#;$delaySeconds;#;$random"
    }
}
