package zel.room.furni.behaviour.use

import zel.game.players.Player
import zel.protocol.generated.AddJukeboxDiskInCodec
import zel.protocol.generated.CreditFurniRedeemInCodec
import zel.protocol.generated.CustomizePlayerWithFurniInCodec
import zel.protocol.generated.FriendFurniConfirmLockInCodec
import zel.protocol.generated.GetItemDataInCodec
import zel.protocol.generated.GetJukeboxPlayListInCodec
import zel.protocol.generated.GetNowPlayingInCodec
import zel.protocol.generated.GetPlayerSongDisksInCodec
import zel.protocol.generated.GetSoundMachinePlayListInCodec
import zel.protocol.generated.PresentOpenInCodec
import zel.protocol.generated.RemoveItemInCodec
import zel.protocol.generated.RemoveJukeboxDiskInCodec
import zel.protocol.generated.RentableSpaceCancelRentInCodec
import zel.protocol.generated.RentableSpaceRentInCodec
import zel.protocol.generated.RentableSpaceStatusInCodec
import zel.protocol.generated.RequestRoomPropertySetInCodec
import zel.protocol.generated.SetAreaHideDataInCodec
import zel.protocol.generated.SetItemDataInCodec
import zel.protocol.generated.SetMannequinFigureInCodec
import zel.protocol.generated.SetMannequinNameInCodec
import zel.protocol.generated.SpinWheelOfFortuneInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.entry.DoorCheck
import zel.room.entry.RoomLeaving
import zel.room.entry.withRightsLevel
import zel.room.furni.behaviour.state.AreaHide
import zel.room.furni.behaviour.state.SoundMachine
import zel.room.furni.place.Interactions
import zel.room.map.Point

class FurniUsePackets(
    private val registry: RoomRegistry,
    private val leaving: RoomLeaving,
    notes: PostIts,
    wheel: ColorWheel,
    mannequin: Mannequin,
    exchange: FurniExchange,
    paint: RoomPaint,
    gifts: Gifts,
    jukebox: Jukebox,
    soundMachine: SoundMachine,
    lock: LoveLock,
    space: RentableSpace,
) : PacketPackage {
    override val name = "furni-use"
    override val routes =
        listOf(
            PacketRoute(GetItemDataInCodec) { session, m ->
                inRoom(session) { scope, player, _ -> notes.read(scope, player.profile.id, m.itemId) }
            },
            PacketRoute(SetItemDataInCodec) { session, m ->
                inRoom(session) { scope, _, level -> notes.write(scope, level, m.itemId, m.color, m.text) }
            },
            PacketRoute(RemoveItemInCodec) { session, m ->
                inRoom(
                    session,
                ) { scope, player, level -> notes.remove(scope, player.profile.id, level, m.itemId) }
            },
            PacketRoute(SpinWheelOfFortuneInCodec) { session, m ->
                inRoom(session) { scope, _, level ->
                    val furni = scope.room.furni[m.itemId]
                    if (furni != null &&
                        Interactions.of(furni.definition) == COLOR_WHEEL
                    ) {
                        wheel.spin(scope, level, furni)
                    }
                }
            },
            PacketRoute(SetMannequinNameInCodec) { session, m ->
                inRoom(session) { scope, _, level ->
                    val furni = scope.room.furni[m.itemId]?.takeIf { it.behaviour === mannequin }
                    if (furni != null && level >= DoorCheck.OWNER) mannequin.rename(scope, furni, m.name)
                }
            },
            PacketRoute(SetMannequinFigureInCodec) { session, m ->
                inRoom(session) { scope, player, level ->
                    val furni = scope.room.furni[m.itemId]?.takeIf { it.behaviour === mannequin }
                    if (furni != null && level >= DoorCheck.OWNER) {
                        mannequin.dress(scope, furni, player.profile.id)
                    }
                }
            },
            PacketRoute(SetAreaHideDataInCodec) { session, m ->
                inRoom(session) { scope, _, level ->
                    val furni = scope.room.furni[m.itemId]
                    val area = furni?.behaviour as? AreaHide
                    if (area != null && level >= DoorCheck.RIGHTS) {
                        val root = Point(m.rootX, m.rootY)
                        area.configure(scope, furni, root, m.width, m.length, m.invisibility, m.wallItems, m.invert)
                    }
                }
            },
            PacketRoute(GetNowPlayingInCodec) { session, _ ->
                inRoom(session) { scope, player, _ -> jukebox.nowPlaying(scope, player.profile.id) }
            },
            PacketRoute(GetPlayerSongDisksInCodec) { session, _ ->
                inRoom(session) { scope, player, _ -> jukebox.songDisks(scope, player.profile.id) }
            },
            PacketRoute(GetJukeboxPlayListInCodec) { session, _ ->
                inRoom(session) { scope, player, _ -> jukebox.playList(scope, player.profile.id) }
            },
            PacketRoute(GetSoundMachinePlayListInCodec) { session, _ ->
                inRoom(session) { scope, player, _ -> soundMachine.answer(scope, player.profile.id) }
            },
            PacketRoute(AddJukeboxDiskInCodec) { session, m ->
                inRoom(session) { scope, player, level -> jukebox.add(scope, player.profile.id, level, m.itemId) }
            },
            PacketRoute(RemoveJukeboxDiskInCodec) { session, m ->
                inRoom(
                    session,
                ) { scope, player, level -> jukebox.remove(scope, level, m.index) }
            },
            PacketRoute(FriendFurniConfirmLockInCodec) { session, m ->
                inRoom(session) { scope, player, _ ->
                    val furni = scope.room.furni[m.itemId]?.takeIf { it.behaviour === lock }
                    if (furni != null) lock.confirm(scope, furni, player.profile.id, m.confirmed)
                }
            },
            PacketRoute(RentableSpaceStatusInCodec) { session, m ->
                inRoom(session) { scope, player, _ ->
                    scope.room.furni[m.itemId]?.takeIf { it.behaviour === space }?.let {
                        space.status(scope, it, player.profile.id)
                    }
                }
            },
            PacketRoute(RentableSpaceRentInCodec) { session, m ->
                inRoom(session) { scope, player, _ ->
                    scope.room.furni[m.itemId]?.takeIf { it.behaviour === space }?.let {
                        space.rent(scope, it, player.profile.id)
                    }
                }
            },
            PacketRoute(RentableSpaceCancelRentInCodec) { session, m ->
                inRoom(session) { scope, player, level ->
                    scope.room.furni[m.itemId]?.takeIf { it.behaviour === space }?.let {
                        space.cancel(scope, it, player.profile.id, level)
                    }
                }
            },
            PacketRoute(PresentOpenInCodec) { session, m ->
                inRoom(session) { scope, player, level -> gifts.open(scope, player, level, m.int1) }
            },
            PacketRoute(RequestRoomPropertySetInCodec) { session, m ->
                inRoom(session) { scope, player, level -> paint.paint(scope, player.profile.id, level, m.itemId) }
            },
            PacketRoute(CreditFurniRedeemInCodec) { session, m ->
                inRoom(session) { scope, player, _ -> exchange.redeem(scope, player, m.itemId) }
            },
            PacketRoute(CustomizePlayerWithFurniInCodec) { session, m ->
                inRoom(session) { scope, player, level -> exchange.redeemClothing(scope, player, level, m.itemId) }
            },
        )

    private fun inRoom(
        session: PacketSession,
        block: (RoomScope, Player, Int) -> Unit,
    ) = registry.withRightsLevel(session, leaving, block)

    private companion object {
        const val COLOR_WHEEL = "chance.wheel"
    }
}
