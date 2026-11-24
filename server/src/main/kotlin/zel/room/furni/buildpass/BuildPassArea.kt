package zel.room.furni.buildpass

import zel.core.rules.rules
import zel.game.permissions.Privilege
import zel.game.permissions.isAnyOwner
import zel.game.players.Player
import zel.room.RoomEngineServices
import zel.room.RoomRegistry
import zel.room.RoomWrites
import zel.room.furni.FurniBehaviours
import zel.room.furni.place.PlacedCount
import zel.room.furni.place.RoomLimits
import zel.room.furni.place.StoredFurniAccess
import zel.room.furni.place.UnitsOnTiles
import zel.room.furni.place.ViewerStates
import zel.room.persist.FurniData

class BuildPassArea(
    private val services: RoomEngineServices,
    private val writes: RoomWrites,
) {
    data class Placed(
        val limits: RoomLimits,
        val units: UnitsOnTiles,
        val viewerStates: ViewerStates,
        val placedCount: PlacedCount,
    )

    private val store =
        JdbcBuildPassStore(services.dataSource, { rules.memberFurniLimit })
    private val reader = BuildPassReader(store)

    val picks: BuildPassPicks = BuildPassPickups(reader, store, writes)

    fun roomSync(registry: RoomRegistry): BuildPassRoomSync =
        BuildPassRoomSync(registry, store, BuildPassRoomLock(store, writes))

    fun packets(
        registry: RoomRegistry,
        access: StoredFurniAccess,
        furniData: FurniData,
        behaviours: FurniBehaviours,
        placed: Placed,
    ): BuildPassPackets {
        val players: (Int) -> Player? = services.clients::onlinePlayer
        return BuildPassPackets(
            registry,
            access,
            reader,
            CatalogBuildPassOffers(services.catalog),
            store,
            BuildPassPlacing(
                store,
                furniData,
                writes,
                behaviours,
                isStaff = { id ->
                    players(id)?.let {
                        it.isAnyOwner || it.hasPermission(Privilege.ROOM_ENTER_ANY)
                    } == true
                },
                limits = placed.limits,
                units = placed.units,
                viewerStates = placed.viewerStates,
                placedCount = placed.placedCount,
                placedByPlayer =
                    GameBuildPassProgress(players, services.achievements, services::progressLater)::placed,
            ),
        )
    }
}
