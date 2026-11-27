package zel.room

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.orFallback
import zel.game.items.FurniParams
import zel.game.items.Item
import zel.game.items.ItemInteraction
import zel.room.furni.FurniBehaviour
import zel.room.furni.behaviour.use.ColorWheel
import zel.room.furni.behaviour.use.FurniExchange
import zel.room.furni.behaviour.use.FurniUsePackets
import zel.room.furni.behaviour.use.FxBox
import zel.room.furni.behaviour.use.GameClothing
import zel.room.furni.behaviour.use.Gifts
import zel.room.furni.behaviour.use.JdbcPaperStore
import zel.room.furni.behaviour.use.Jukebox
import zel.room.furni.behaviour.use.JukeboxStore
import zel.room.furni.behaviour.use.LoveLock
import zel.room.furni.behaviour.use.Mannequin
import zel.room.furni.behaviour.use.MannequinLooks
import zel.room.furni.behaviour.use.PetBowl
import zel.room.furni.behaviour.use.PostIts
import zel.room.furni.behaviour.use.RentableSpace
import zel.room.furni.behaviour.use.RoomPaint
import zel.room.furni.behaviour.use.StickyPole
import zel.room.furni.place.FurniPickup
import zel.room.port.RoomPort

internal class FurniUseWiring(
    private val services: RoomEngineServices,
    private val core: CoreWiring,
    private val furni: FurniWiring,
    private val pickups: FurniPickup,
    port: () -> RoomPort?,
    behaviourOf: (String) -> FurniBehaviour,
) {
    val paint =
        RoomPaint(core.furniInventory, JdbcPaperStore(services.dataSource, services.persist)) { playerId, deco ->
            services.progress(playerId, deco, 1)
        }
    val jukebox =
        Jukebox(
            { services.items()?.items() },
            JukeboxStore(services.dataSource),
            services.clients::onlinePlayer,
            core.roomWrites,
            services.progress,
            ::jukeboxCapacity,
        ) { scope, playerId -> core.leaving.doorOf(scope.room).levelOf(playerId) }
    private val loveLock = LoveLock(services.clients::onlinePlayer)
    val rentableSpace =
        RentableSpace(
            services.clients::onlinePlayer,
            { pickups },
            { services.config.getInt(RENT_DAYS_KEY, DEFAULT_RENT_DAYS) },
            { services.config.getInt(RENT_PRICE_KEY, DEFAULT_RENT_PRICE) },
            core.nowSeconds,
            furni.special.host::playerName,
            ::freeStoredRent,
        )
    val gifts =
        Gifts(
            { services.items()?.items() },
            { pickups },
            core.furniInventory,
            services.persist,
            { roomId, job -> core.rooms.get()?.post(roomId, 0, job) },
            furni.state::later,
            core.nowSeconds,
        ) { behaviourOf(ItemInteraction.DEFAULT) }
    private val mannequin = Mannequin(MannequinLooks(services.clients::onlinePlayer, port))
    val postIts =
        PostIts({ text, player -> services.wordFilter()?.filter(text, player) ?: text }) { roomId, rowId ->
            furni.deleteSpent(roomId, rowId)
        }

    val behaviours: List<Pair<String, FurniBehaviour>> =
        listOf(
            "gift.box" to gifts,
            Gifts.RECYCLER_BOX to gifts,
            "pet.drink" to PetBowl(core.walking, furni.state::later),
            "music.jukebox" to jukebox,
            "lock.love" to loveLock,
            "area.rental" to rentableSpace,
            "note.pole" to StickyPole,
            "outfit.mannequin" to mannequin,
            "effect.box" to
                FxBox(services.clients::onlinePlayer) { scope, box, removeMillis, onDeleted ->
                    furni.special.spend(scope, box, removeMillis, onDeleted = onDeleted)
                },
        )

    fun packets(registry: RoomRegistry): FurniUsePackets =
        FurniUsePackets(
            registry,
            core.leaving,
            postIts,
            ColorWheel(furni.state),
            mannequin,
            FurniExchange(
                pickups,
                furni::deleteSpent,
                GameClothing(services.dataSource, services.persist, services.catalog),
            ),
            paint,
            gifts,
            jukebox,
            furni.soundMachine,
            loveLock,
            rentableSpace,
        )

    /** The discs its params name; for a jukebox that names none, one wider than a tile is a large one. */
    private fun jukeboxCapacity(definition: Item): Int =
        FurniParams.int(definition.params, 0)?.takeIf { it > 0 }
            ?: if (definition.width * definition.length > 1) {
                services.config.getInt(JUKEBOX_LARGE_KEY, JUKEBOX_LARGE)
            } else {
                services.config.getInt(JUKEBOX_NORMAL_KEY, JUKEBOX_NORMAL)
            }

    private fun freeStoredRent(
        renter: Int,
        spaceId: Int,
    ) = services.persist(
        Runnable {
            log.orFallback("freeing the rent of player $renter", Unit) {
                Sql(services.dataSource).write(FREE_RENT) {
                    int(renter)
                    int(spaceId)
                }
            }
        },
    )

    private companion object {
        val log: Logger = LoggerFactory.getLogger(FurniUseWiring::class.java)
        const val FREE_RENT =
            "UPDATE player_stats SET rented_space_furni_id = NULL, rented_space_ends_at = NULL " +
                "WHERE player_id = ? AND rented_space_furni_id = ?"
        const val RENT_DAYS_KEY = "furni.rentable_space.rent.days"
        const val DEFAULT_RENT_DAYS = 7
        const val RENT_PRICE_KEY = "furni.rentable_space.tile.credits"
        const val DEFAULT_RENT_PRICE = 10
        const val JUKEBOX_LARGE_KEY = "furni.jukebox.tracks.large"
        const val JUKEBOX_LARGE = 20
        const val JUKEBOX_NORMAL_KEY = "furni.jukebox.tracks.normal"
        const val JUKEBOX_NORMAL = 10
    }
}
