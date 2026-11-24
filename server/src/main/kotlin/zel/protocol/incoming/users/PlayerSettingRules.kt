package zel.protocol.incoming.users

/** Anything outside a range is the default. */
object PlayerSettingRules {
    private const val FREE_FLOW = 0
    private const val NORMAL = 1
    private const val EVERYONE = 0

    fun chatMode(sent: Int): Int = sent.orDefault(0..1, FREE_FLOW)

    fun bubbleWidth(sent: Int): Int = sent.orDefault(0..2, NORMAL)

    fun scrollSpeed(sent: Int): Int = sent.orDefault(0..2, NORMAL)

    fun onlineIndicator(sent: Int): Int = sent.orDefault(0..2, EVERYONE)

    private fun Int.orDefault(
        allowed: IntRange,
        default: Int,
    ): Int = if (this in allowed) this else default
}
