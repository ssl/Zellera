package zel.wired.chest

data class ChestType(
    val kind: ChestKind,
    val baseCapacity: Int,
    val step: Int,
    val maxSteps: Int,
    val starter: Boolean = false,
) {
    companion object {
        private const val COIN_CAPACITY = 5000
        private const val COIN_STEPS = 19
        private const val FURNI_CAPACITY = 1000
        private const val FURNI_STEPS = 9
        private const val STARTER_CAPACITY = 100
        private const val COINS_PREFIX = "wf_storage_coins"
        private const val FURNI_PREFIX = "wf_storage_furni"
        private const val STARTER = "wf_storage_furni_starter"

        val CODES: List<String> =
            listOf("wf_storage_coins1", "wf_storage_coins2", "wf_storage_furni1", "wf_storage_furni2", STARTER)

        fun of(code: String): ChestType? =
            when {
                code == STARTER -> ChestType(ChestKind.FURNI, STARTER_CAPACITY, 0, 0, starter = true)
                code.startsWith(COINS_PREFIX) -> ChestType(ChestKind.COINS, COIN_CAPACITY, COIN_CAPACITY, COIN_STEPS)
                code.startsWith(FURNI_PREFIX) -> ChestType(ChestKind.FURNI, FURNI_CAPACITY, FURNI_CAPACITY, FURNI_STEPS)
                else -> null
            }
    }
}
