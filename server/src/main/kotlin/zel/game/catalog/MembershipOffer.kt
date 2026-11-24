package zel.game.catalog

import zel.game.players.CLUB_MONTH_DAYS
import zel.protocol.generated.PacketWriter
import zel.util.GameZone
import zel.util.SECONDS_PER_DAY
import java.sql.ResultSet
import java.time.Instant
import java.time.LocalDate

class MembershipOffer(
    val id: Int,
    val name: String?,
    val days: Int,
    val price: OfferPrice,
    val kind: Kind,
    val isDeal: Boolean,
    val giftable: Boolean,
    private val now: () -> Int,
) {
    enum class Window {
        CLUB,
        BUILD_PASS,
        BUILD_PASS_ADDONS,
    }

    enum class Kind(
        val code: String,
        val window: Window,
        val addsDays: Boolean = true,
    ) {
        CLUB("club", Window.CLUB),
        VIP("vip", Window.CLUB),
        BUILD_PASS("build_pass", Window.BUILD_PASS),
        BUILD_PASS_ADDON("build_pass_addon", Window.BUILD_PASS_ADDONS, addsDays = false),
        ;

        companion object {
            fun fromCode(code: String?): Kind = entries.firstOrNull { it.code == code } ?: CLUB
        }
    }

    data class AfterPurchase(
        val months: Int,
        val daysAhead: Int,
        val lastDay: LocalDate,
    )

    val isGift: Boolean get() = name.orEmpty().contains("GIFT")

    fun soldIn(window: Window): Boolean = kind.window == window

    /** [memberUntil] is 0 or in the past for a non-member. */
    fun afterPurchase(memberUntil: Int): AfterPurchase {
        val current = now()
        val start = maxOf(memberUntil, current)
        val end = Instant.ofEpochSecond(start + days.toLong() * SECONDS_PER_DAY)
        return AfterPurchase(
            months = days / CLUB_MONTH_DAYS,
            daysAhead = (start - current) / SECONDS_PER_DAY + days,
            lastDay = LocalDate.ofInstant(end, GameZone.current),
        )
    }

    fun write(
        out: PacketWriter,
        memberUntil: Int,
    ) {
        val after = afterPurchase(memberUntil)
        with(out) {
            int(id).string(name.orEmpty()).boolean(false)
            int(price.credits).int(price.points).int(price.pointsType)
            boolean(kind == Kind.VIP).int(after.months)
            int(days).boolean(giftable).int(after.daysAhead)
            after.lastDay.run { int(year).int(monthValue).int(dayOfMonth) }
        }
    }

    companion object {
        /** The club window's source code for the gift dialog; the other sources list the non-gift offers. */
        const val SOURCE_VIP_GIFT = 2

        fun read(
            row: ResultSet,
            now: () -> Int,
        ): MembershipOffer =
            row.run {
                MembershipOffer(
                    getInt("id"),
                    getString("name"),
                    getInt("days"),
                    OfferPrice(getInt("price_credits"), getInt("price_currency"), getInt("currency")),
                    Kind.fromCode(getString("kind")),
                    getBoolean("is_deal"),
                    getBoolean("giftable"),
                    now,
                )
            }
    }
}
