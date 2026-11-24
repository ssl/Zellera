package zel.protocol.achievements

data class AchievementEntry(
    val id: Int,
    val step: Step,
    val reward: Reward,
    val progress: Int,
    val finished: Boolean,
    val listing: Listing,
    val state: Short,
) {
    data class Step(
        val level: Int,
        val badge: String,
        val from: Int,
        val to: Int,
    )

    data class Reward(
        val amount: Int,
        val currency: Int,
    )

    data class Listing(
        val category: String,
        val subcategory: String,
        val steps: Int,
        val method: Int,
    )
}
