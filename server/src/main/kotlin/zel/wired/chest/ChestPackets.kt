package zel.wired.chest

import zel.protocol.generated.CloseChestInCodec
import zel.protocol.generated.LockAllChestsInCodec
import zel.protocol.generated.OpenChestAndGetContentsInCodec
import zel.protocol.generated.SetChestOptionsInCodec
import zel.protocol.generated.SetChestPreferencesInCodec
import zel.protocol.generated.StartAddingToChestInCodec
import zel.protocol.generated.UpgradeChestInCodec
import zel.protocol.generated.WiredOpenContractInCodec
import zel.protocol.generated.WiredTradeAddDeleteItemsInCodec
import zel.protocol.generated.WiredTradeCancelInCodec
import zel.protocol.generated.WiredTradeConfirmInCodec
import zel.protocol.generated.WiredTransactionGetChestLogsInCodec
import zel.protocol.generated.WiredTransactionGetLogDetailsInCodec
import zel.protocol.generated.WiredTransactionGetRoomLogsInCodec
import zel.protocol.generated.WiredValidationErrorOut
import zel.protocol.generated.WiredValidationErrorOutCodec
import zel.protocol.generated.WithdrawAllFromChestInCodec
import zel.protocol.generated.WithdrawCoinsFromChestInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.Room
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.wired.form.WiredMasks
import zel.wired.protocol.withWiredLevel

class ChestPackets(
    private val registry: RoomRegistry,
    private val desk: ChestDesk,
    private val masks: (Room) -> WiredMasks,
    private val levels: (Room) -> Map<Int, Int>,
) : PacketPackage {
    override val name = "wired-chests"
    override val routes =
        windowRoutes() + tableRoutes() + withdrawalRoutes() + settingsRoutes() + logRoutes() + contractRoutes()

    private fun windowRoutes(): List<PacketRoute<*>> =
        listOf(
            PacketRoute(OpenChestAndGetContentsInCodec) { session, m ->
                inRoom(session) { scope, player, _ -> desk.furni.open(scope, player, m.itemId) }
            },
            PacketRoute(CloseChestInCodec) { session, m ->
                inRoom(session) { scope, player, _ -> desk.furni.close(scope, player, m.itemId) }
            },
        )

    private fun tableRoutes(): List<PacketRoute<*>> =
        listOf(
            PacketRoute(StartAddingToChestInCodec) { session, m ->
                inRoom(session) { scope, player, level -> desk.deals.startDeposit(scope, player, level, m.chestItemId) }
            },
            PacketRoute(WiredTradeAddDeleteItemsInCodec) { session, m ->
                inRoom(session) { scope, player, _ -> desk.deals.offer(scope, player, remove = m.add, m.itemIds) }
            },
            PacketRoute(WiredTradeConfirmInCodec) { session, m ->
                inRoom(session) { scope, player, _ -> desk.deals.confirm(scope, player, m.confirm) }
            },
            PacketRoute(WiredTradeCancelInCodec) { session, _ ->
                inRoom(session) { scope, player, _ -> desk.deals.cancelled(scope, player) }
            },
        )

    private fun withdrawalRoutes(): List<PacketRoute<*>> =
        listOf(
            PacketRoute(ItemsWithdrawalCodec) { session, m ->
                inRoom(session) { scope, player, level -> desk.withdrawals.items(scope, player, level, m) }
            },
            PacketRoute(WithdrawCoinsFromChestInCodec) { session, m ->
                inRoom(session) { scope, player, level -> desk.withdrawals.coins(scope, player, level, m.int1, m.int2) }
            },
            PacketRoute(WithdrawAllFromChestInCodec) { session, m ->
                inRoom(session) { scope, player, level -> desk.withdrawals.all(scope, player, level, m.itemId) }
            },
        )

    private fun settingsRoutes(): List<PacketRoute<*>> =
        listOf(
            PacketRoute(SetChestPreferencesInCodec) { session, m ->
                inRoom(session) { scope, player, _ -> desk.settings.preferences(scope, player, m) }
            },
            PacketRoute(SetChestOptionsInCodec) { session, m ->
                inRoom(session) { scope, player, level -> desk.settings.options(scope, player, level, m) }
            },
            PacketRoute(ChestNotificationsCodec) { session, m ->
                inRoom(session) { scope, player, _ -> desk.settings.notifications(scope, player, m) }
            },
            PacketRoute(LockAllChestsInCodec) { session, m ->
                inRoom(session) { scope, player, level -> desk.settings.lockAll(scope, player, level, m.lock, m.all) }
            },
            PacketRoute(UpgradeChestInCodec) { session, m ->
                inRoom(session) { scope, player, _ -> desk.settings.upgrade(scope, player, m.itemId, m.quantity) }
            },
        )

    private fun logRoutes(): List<PacketRoute<*>> =
        listOf(
            PacketRoute(WiredTransactionGetChestLogsInCodec) { session, m ->
                inRoom(session) { scope, player, level ->
                    val readable = masks(scope.room).mayRead(level) && desk.chests.inRoom(scope.room, m.chestId) != null
                    if (readable) {
                        desk.logs.chestList(session, m.chestId, m.pageSize, m.page)
                    } else {
                        scope.refuseChestUse(player, NO_READ)
                    }
                }
            },
            PacketRoute(WiredTransactionGetRoomLogsInCodec) { session, m ->
                inRoom(session) { scope, player, level ->
                    if (masks(scope.room).mayRead(level)) {
                        desk.logs.roomList(session, scope.room.info.id, m.pageSize, m.page)
                    } else {
                        scope.refuseChestUse(player, NO_READ)
                    }
                }
            },
            PacketRoute(WiredTransactionGetLogDetailsInCodec) { session, m ->
                inRoom(session) { scope, player, level ->
                    if (masks(scope.room).mayRead(level)) {
                        desk.logs.details(session, scope.room.info.id, m.transactionId) { noPermissions(session) }
                    } else {
                        scope.refuseChestUse(player, NO_PERMISSIONS)
                    }
                }
            },
        )

    private fun contractRoutes(): List<PacketRoute<*>> =
        listOf(
            PacketRoute(WiredOpenContractInCodec) { session, m ->
                inRoom(session) { scope, player, level -> desk.contracts.open(scope, player, level, m.int1) }
            },
            PacketRoute(ContractSaveCodec) { session, m ->
                inRoom(session) { scope, player, level -> desk.contracts.update(scope, player, level, m) }
            },
        )

    private fun noPermissions(session: PacketSession) =
        session.send(WiredValidationErrorOutCodec, WiredValidationErrorOut(NO_PERMISSIONS, emptyList()))

    private fun inRoom(
        session: PacketSession,
        block: (RoomScope, Int, Int) -> Unit,
    ) = registry.withWiredLevel(session, levels, block)

    private companion object {
        const val NO_READ = "wiredfurni.error.no_read_rights"
        const val NO_PERMISSIONS = "wiredfurni.error.transaction_no_permissions"
    }
}
