package zel.room.furni.behaviour.use

import zel.game.items.StateFields
import zel.game.players.Player
import zel.protocol.generated.RentableSpaceRentFailedOut
import zel.protocol.generated.RentableSpaceRentFailedOutCodec
import zel.protocol.generated.RentableSpaceRentOkOut
import zel.protocol.generated.RentableSpaceRentOkOutCodec
import zel.protocol.generated.RentableSpaceStatusOut
import zel.protocol.generated.RentableSpaceStatusOutCodec
import zel.protocol.generated.StuffData
import zel.room.Room
import zel.room.RoomScope
import zel.room.entry.DoorCheck
import zel.room.furni.RoomFurni
import zel.room.furni.Usable
import zel.room.furni.behaviour.special.SpecialStuff
import zel.room.furni.behaviour.state.StateShow
import zel.room.furni.place.FurniPickup
import zel.room.furni.place.Placeable
import zel.room.furni.place.StuffOnWire
import zel.room.map.Point
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.path.ClosedToWalks
import zel.room.unit.path.Passage
import zel.util.SECONDS_PER_DAY

class RentableSpace(
    private val player: (Int) -> Player?,
    private val pickups: () -> FurniPickup?,
    private val rentDays: () -> Int,
    private val tileCredits: () -> Int,
    private val nowSeconds: () -> Int,
    private val renterName: (Int) -> String,
    private val freed: (renter: Int, spaceId: Int) -> Unit,
) : Usable,
    Placeable,
    StuffOnWire,
    ClosedToWalks {
    private data class Rent(
        val renter: Int,
        val end: Int,
    )

    private fun rentOf(furni: RoomFurni): Rent =
        StateFields.read(furni.state)?.let { Rent(it.number(RENTER), it.number(UNTIL)) } ?: Rent(0, 0)

    private fun rented(furni: RoomFurni): Boolean = rentOf(furni).end > nowSeconds()

    override fun stuff(furni: RoomFurni): StuffData {
        val rent = rentOf(furni)
        val values = if (rent.end > nowSeconds()) mapOf("renterId" to rent.renter.toString()) else emptyMap()
        return SpecialStuff.map(furni, values)
    }

    override fun closed(
        room: Room,
        furni: RoomFurni,
    ): Boolean = rented(furni)

    override fun passage(furni: RoomFurni): Passage = Passage.Rented(rentOf(furni).renter)

    override fun onPickedUp(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        val rent = rentOf(furni)
        if (rent.renter > 0) free(rent.renter, furni.id)
        furni.state = FREE
    }

    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) = status(scope, furni, playerId)

    fun status(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
    ) {
        val asker = player(playerId) ?: return
        vacateIfOver(scope, furni)
        val rent = rentOf(furni)
        val isRented = rent.end > nowSeconds()
        val status =
            RentableSpaceStatusOut(
                isRented,
                errorFor(asker, furni),
                if (isRented) rent.renter else 0,
                if (isRented) renterName(rent.renter) else "",
                if (isRented) rent.end - nowSeconds() else 0,
                cost(furni),
            )
        scope.out.send(Lane.Now, Audience.Player(playerId), RentableSpaceStatusOutCodec, status)
    }

    fun rent(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
    ) {
        val renter = player(playerId) ?: return
        vacateIfOver(scope, furni)
        val error = errorFor(renter, furni)
        if (error != 0) {
            val failed = RentableSpaceRentFailedOut(error)
            return scope.out.send(Lane.Now, Audience.Player(playerId), RentableSpaceRentFailedOutCodec, failed)
        }
        val cost = cost(furni)
        if (cost > 0 && !renter.wallet.takeCredits(cost)) {
            val failed = RentableSpaceRentFailedOut(NOT_ENOUGH_CREDITS)
            return scope.out.send(Lane.Now, Audience.Player(playerId), RentableSpaceRentFailedOutCodec, failed)
        }
        val end = nowSeconds() + rentDays() * SECONDS_PER_DAY
        renter.stats.rentedSpaceId = furni.id
        renter.stats.rentedUntil = end
        StateShow.whole(
            scope,
            furni,
            StateFields
                .write()
                .number(RENTER, playerId)
                .number(UNTIL, end)
                .state(),
        )
        val ok = RentableSpaceRentOkOut(end)
        scope.out.send(Lane.Now, Audience.Player(playerId), RentableSpaceRentOkOutCodec, ok)
    }

    fun cancel(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        level: Int,
    ) {
        val mayEnd = furni.ownerId == playerId || level >= DoorCheck.OWNER || rentOf(furni).renter == playerId
        if (!mayEnd) return
        end(scope, furni)
        status(scope, furni, playerId)
    }

    fun rentedBy(
        room: Room,
        playerId: Int,
        tile: Point,
    ): Boolean =
        room.furni.all().any { furni ->
            furni.behaviour === this && rentOf(furni).renter == playerId && rented(furni) && tile in furni.tiles()
        }

    /** A rent that ran out ends at the next look at the space: the renter's furni go back to him. */
    private fun vacateIfOver(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        val rent = rentOf(furni)
        if (rent.renter > 0 && rent.end <= nowSeconds()) end(scope, furni)
    }

    private fun end(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        val rent = rentOf(furni)
        if (rent.renter > 0) {
            val inside = furni.tiles().toSet()
            pickups()?.sweep(scope, rent.renter) { other ->
                other.ownerId == rent.renter && other !== furni && other.tiles().any { it in inside }
            }
            free(rent.renter, furni.id)
        }
        StateShow.whole(scope, furni, FREE)
    }

    private fun free(
        renter: Int,
        spaceId: Int,
    ) {
        val stats = player(renter)?.stats ?: return freed(renter, spaceId)
        if (stats.rentedSpaceId != spaceId) return
        stats.rentedSpaceId = 0
        stats.rentedUntil = 0
    }

    private fun errorFor(
        asker: Player,
        furni: RoomFurni,
    ): Int {
        val stats = asker.stats
        return when {
            rented(furni) -> ALREADY_RENTED
            stats.isRentingSpace && stats.rentedSpaceId != furni.id -> ONLY_ONE
            stats.club.clubExpiresAt < nowSeconds() -> NO_CLUB
            cost(furni) > asker.profile.credits -> NOT_ENOUGH_CREDITS
            else -> 0
        }
    }

    /** One rent costs the same for every tile, however long it lasts. */
    private fun cost(furni: RoomFurni): Int = maxOf(0, tileCredits()) * furni.definition.width * furni.definition.length

    private companion object {
        const val FREE = ""
        const val RENTER = "renter"
        const val UNTIL = "until"
        const val ALREADY_RENTED = 100
        const val ONLY_ONE = 103
        const val NOT_ENOUGH_CREDITS = 200
        const val NO_CLUB = 203
    }
}
