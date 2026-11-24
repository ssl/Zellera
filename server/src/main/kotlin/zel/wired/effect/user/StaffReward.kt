package zel.wired.effect.user

data class StaffReward(
    val isBadge: Boolean,
    val code: String,
    val chance: Int,
) {
    companion object {
        fun parse(text: String): List<StaffReward> =
            text.split(';').mapNotNull { row ->
                val parts = row.split(',')
                if (parts.size == ROW_PARTS && parts[1].isNotEmpty()) {
                    StaffReward(parts[0] == BADGE, parts[1], parts[2].toIntOrNull() ?: 0)
                } else {
                    null
                }
            }

        private const val ROW_PARTS = 3
        private const val BADGE = "0"
    }
}
