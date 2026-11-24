package zel.wired.usage

object UsageLimit {
    private const val FIRST_LEVEL = 6

    private val ROWS =
        listOf(
            UsageAllowance(2500.0, 12, 50, 37),
            UsageAllowance(2813.0, 14, 56, 42),
            UsageAllowance(3125.0, 15, 62, 46),
            UsageAllowance(3438.0, 17, 68, 51),
            UsageAllowance(3750.0, 18, 75, 56),
            UsageAllowance(4063.0, 20, 81, 60),
            UsageAllowance(4375.0, 21, 87, 65),
            UsageAllowance(4688.0, 23, 93, 70),
            UsageAllowance(5000.0, 25, 100, 75),
            UsageAllowance(5313.0, 26, 112, 84),
            UsageAllowance(5625.0, 28, 112, 84),
            UsageAllowance(5937.5, 29, 118, 89),
            UsageAllowance(6250.0, 31, 125, 93),
            UsageAllowance(6563.0, 32, 131, 98),
            UsageAllowance(6563.0, 34, 137, 103),
            UsageAllowance(7188.0, 35, 143, 107),
            UsageAllowance(7500.0, 37, 150, 112),
            UsageAllowance(7813.0, 39, 156, 117),
            UsageAllowance(8125.0, 40, 162, 121),
            UsageAllowance(8438.0, 42, 168, 126),
            UsageAllowance(8750.0, 43, 175, 131),
            // Level 27: 9062.5 exactly.
            UsageAllowance(9062.5, 45, 181, 135),
            UsageAllowance(9375.0, 46, 187, 140),
            UsageAllowance(9688.0, 48, 193, 145),
            UsageAllowance(10000.0, 50, 200, 150),
        )

    fun of(level: Int): UsageAllowance = ROWS[(level - FIRST_LEVEL).coerceIn(ROWS.indices)]
}
