package zel.wired.effect.furni

data class MoveStyle(
    val duration: Int = FurniMoves.SLIDE_MILLIS,
    val animated: Boolean = true,
    val turnsSlide: Boolean = false,
    val keepsAltitude: Boolean = false,
)
