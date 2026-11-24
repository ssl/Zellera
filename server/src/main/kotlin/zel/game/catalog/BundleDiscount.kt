package zel.game.catalog

data class BundleDiscount(
    val maximumItems: Int,
    val batchSize: Int,
    val freeItemsPerBatch: Int,
    val minimumDiscountsForBonus: Int,
    val additionalThresholds: List<Int>,
) {
    fun bonus(units: Int): Int {
        val batches = if (batchSize > 0) units / batchSize else 0
        val perBatch = batches * freeItemsPerBatch
        val batchBonus =
            if (batchSize > 0 && batches >= minimumDiscountsForBonus) {
                (if (units % batchSize == batchSize - 1) 1 else 0) + batches - minimumDiscountsForBonus
            } else {
                0
            }
        return perBatch + batchBonus + additionalThresholds.count { units >= it }
    }

    companion object {
        const val STANDARD_MAXIMUM_ITEMS = 100
        val STANDARD_THRESHOLDS: List<Int> =
            (
                "5;9;9;13;13;17;17;21;21;25;25;29;29;33;33;37;37;41;41;45;" +
                    "45;49;49;53;53;57;57;61;61;65;65;69;69;73;73;77;77;81;81;85;85;89;89;93;93;97;97"
            ).split(';').map(String::toInt)
    }
}
