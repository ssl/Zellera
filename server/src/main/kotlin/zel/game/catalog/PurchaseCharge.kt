package zel.game.catalog

internal data class PurchaseCharge<T>(
    val value: T,
    val credits: Int,
    val points: Int,
    val pointsType: Int,
)
