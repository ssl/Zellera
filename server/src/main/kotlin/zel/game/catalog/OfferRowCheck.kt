package zel.game.catalog

import java.sql.ResultSet

internal class OfferRowCheck(
    private val id: Int,
    private val credits: Int,
    private val points: Int,
    private val pointsType: Int,
    private val amount: Int,
    private val editionSize: Int,
    private val editionSold: Int,
) {
    fun problems(): List<String> =
        listOfNotNull(
            atLeast("price_credits", credits, 0),
            atLeast("price_currency", points, 0),
            atLeast("currency", pointsType, 0),
            atLeast("amount", amount, 1),
            atLeast("limited_total", editionSize, 0),
            atMost("limited_sold", editionSold, editionSize),
        )

    private fun atLeast(
        column: String,
        value: Int,
        floor: Int,
    ): String? = "catalogue offer $id: $column must be at least $floor, is $value".takeIf { value < floor }

    private fun atMost(
        column: String,
        value: Int,
        ceiling: Int,
    ): String? =
        "catalogue offer $id: $column must be at most $ceiling, is $value".takeIf {
            value < 0 ||
                value > ceiling
        }

    companion object {
        fun of(row: ResultSet): OfferRowCheck =
            OfferRowCheck(
                id = row.getInt("id"),
                credits = row.getInt("price_credits"),
                points = row.getInt("price_currency"),
                pointsType = row.getInt("currency"),
                amount = row.getInt("amount"),
                editionSize = row.getInt("limited_total"),
                editionSold = row.getInt("limited_sold"),
            )
    }
}
