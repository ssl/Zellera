package zel.game.campaign.calendar

/**
 * The door text has `%name%` placeholders for the parts. A prize with a [day] is that door's; one without is drawn.
 * [currency] is a currency code as the purse books it; null pays nothing.
 */
data class CalendarPrize(
    val id: Int,
    val text: String,
    val image: String,
    val currency: Int? = null,
    val amount: Int = 0,
    val badge: String = "",
    val furniId: Int? = null,
    val clubKind: String = "",
    val clubDays: Int = 0,
    val day: Int? = null,
) {
    val givesClub: Boolean get() = clubKind.isNotBlank() && clubDays > 0

    val pays: Boolean get() = currency != null && amount > 0

    /** Placeholders of parts this prize lacks stay as written. */
    fun doorText(furniName: String?): String =
        PLACEHOLDER.replace(text) { match -> partOf(match.groupValues[1], furniName) ?: match.value }

    private fun partOf(
        name: String,
        furniName: String?,
    ): String? =
        when (name) {
            "amount" -> amount.toString().takeIf { pays }
            "badge" -> badge.ifBlank { null }
            "days" -> clubDays.toString().takeIf { givesClub }
            "item" -> furniName
            else -> null
        }

    private companion object {
        val PLACEHOLDER = Regex("%([a-z_]+)%")
    }
}
