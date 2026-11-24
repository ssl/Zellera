package zel.protocol.navigator

import zel.game.permissions.PlayerLimits
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.protocol.generated.CanCreateRoomInCodec
import zel.protocol.generated.CreateFlatInCodec
import zel.protocol.generated.DeleteRoomInCodec
import zel.protocol.generated.FlatCreatedOut
import zel.protocol.generated.FlatCreatedOutCodec
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.protocol.typed.player
import zel.room.listing.RoomListings

class RoomLifecyclePackets(
    rooms: RoomListings,
    creation: RoomCreation,
    deletion: RoomDeletion,
) : PacketPackage {
    override val name = "roomlifecycle"

    private val createCooldown = ClientCooldown("navigator.createroom", CREATE_GAP_MS)
    private val deleteCooldown = ClientCooldown("navigator.deleteroom", DELETE_GAP_MS)

    override val routes =
        listOf(
            PacketRoute(CanCreateRoomInCodec) { session, _ ->
                val player = session.player
                sendLimit(session, rooms.countOwned(player.profile.id), PlayerLimits.maxRooms(player))
            },
            PacketRoute(CreateFlatInCodec) { session, m ->
                if (createCooldown.acquire(session.client)) {
                    val request = NewRoom(m.name, m.description, m.modelName, m.categoryId, m.maxUsers, m.tradeType)
                    when (val result = creation.create(session.player, request)) {
                        is RoomCreationResult.LimitReached -> {
                            sendLimit(session, result.count, result.max)
                        }

                        is RoomCreationResult.Created -> {
                            session.send(FlatCreatedOutCodec, FlatCreatedOut(result.room.id, result.room.name))
                            PlayerActions.progress(session.player, CountedAction.CREATE_ROOM, 1)
                        }

                        RoomCreationResult.Refused -> {
                        }
                    }
                }
            },
            PacketRoute(DeleteRoomInCodec) { session, m ->
                if (deleteCooldown.acquire(session.client)) deletion.delete(session.client, m.roomId)
            },
        )

    private fun sendLimit(
        session: PacketSession,
        count: Int,
        max: Int,
    ) = session.client.send(CanCreateRoom(count, max))

    private companion object {
        const val CREATE_GAP_MS = 3000L
        const val DELETE_GAP_MS = 500L
    }
}
