package zel.wired.chest

data class Term(
    val credits: Boolean,
    val amount: Int,
    val wall: Boolean = false,
    val typeId: Int = 0,
    val extra: String = "",
) {
    companion object {
        const val MAX_CREDITS = 100_000
        const val MAX_FURNI = 500

        fun ceiling(credits: Boolean): Int = if (credits) MAX_CREDITS else MAX_FURNI
    }
}
