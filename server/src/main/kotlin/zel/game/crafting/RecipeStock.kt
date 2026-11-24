package zel.game.crafting

import java.util.concurrent.atomic.AtomicInteger

sealed interface RecipeStock {
    val available: Boolean

    fun take(): Boolean

    fun giveBack()

    data object Unlimited : RecipeStock {
        override val available = true

        override fun take() = true

        override fun giveBack() = Unit
    }

    class Limited(
        left: Int,
    ) : RecipeStock {
        private val pieces = AtomicInteger(left)

        override val available: Boolean get() = pieces.get() > 0

        override fun take(): Boolean = pieces.getAndUpdate { maxOf(it - 1, 0) } > 0

        override fun giveBack() {
            pieces.incrementAndGet()
        }
    }
}
