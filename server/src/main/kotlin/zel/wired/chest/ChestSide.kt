package zel.wired.chest

import zel.game.items.OwnedFurni
import zel.protocol.generated.StuffData
import zel.room.Room
import zel.room.RoomListener
import zel.room.RoomPart
import zel.room.RoomRegistry
import zel.room.furni.FurniBehaviour
import zel.room.tick.TickPart
import zel.wired.BoxSettingsSource
import zel.wired.WiredRooms
import zel.wired.box.BoxKind
import zel.wired.form.DialogSpec
import zel.wired.form.DialogSpecs
import zel.wired.form.WiredMasks
import zel.wired.protocol.BoxUse
import zel.wired.variable.RoomVariables
import zel.wired.variable.VariableFacts

class ChestSide(
    services: ChestServices,
    wired: () -> WiredRooms?,
    forms: () -> BoxSettingsSource?,
    private val levelOf: (Room) -> Map<Int, Int>,
) {
    private val store = JdbcChestStore(services.dataSource)
    private val persist = services.persist
    private val host =
        GameChests(
            services.clients,
            services.config,
            store,
            services.writer,
            services.items,
            services.earnings,
            services.progress,
            services.poster,
            services.filterText,
            services.later,
        )
    val chests: RoomChests = RoomChests(store, services.writer) { scope, playerId -> deals.left(scope, playerId) }

    fun inventoryStuff(item: OwnedFurni): StuffData? {
        val type = ChestType.of(item.definition.interaction.name) ?: return null
        val chest = store.chest(item.dbId)?.toChest(item.dbId, type) ?: Chest(item.dbId, type)
        return if (chest.placed) ChestStuff.of(chest, item.state) else null
    }

    private val contracts =
        ChestContracts(
            store,
            services.writer,
            forms,
            object : ContractRecord.FurniTypes {
                override fun spriteOf(baseId: Int): Int = host.spriteOf(baseId)

                override fun baseOf(
                    wall: Boolean,
                    spriteId: Int,
                ): Int? = host.definition(wall, spriteId)?.id

                override fun isWall(baseId: Int): Boolean = host.isWall(baseId)
            },
        )
    private val notices: ChestNotices = ChestNotices(chests, store, host, persist)
    private val moves: ChestMoves = ChestMoves(chests, ChestLog(store), notices, host)
    private val triggers = TransactionTriggers(wired)
    private val deals: ChestDeals = ChestDeals(chests, moves, notices, host, triggers)
    private val transactions = Transactions(chests, deals, moves, notices, host, triggers)
    private val gifts = Gifts(moves, host, deals, notices)
    private val furni = ChestFurni(chests, deals, host) { room, player -> levelOf(room)[player] }

    val stock = RoomChestStock(chests)

    val parts: List<RoomPart> get() = listOf(chests, contracts, deals)

    val tick: TickPart get() = deals.tick

    val listener: RoomListener get() = chests

    /** No save of these dialogs ever passes. */
    val dialogs: Map<String, DialogSpec> =
        DialogSpecs.parse(CONTRACT_DIALOGS, BoxKind.Effect).associateBy { it.code }

    fun boxes(variables: RoomVariables): ChestBoxes =
        ChestBoxes(transactions, contracts, deals, gifts, chests, notices, variables)

    fun behaviours(use: BoxUse): List<Pair<String, FurniBehaviour>> =
        ChestType.CODES.map { it to furni } + ContractKind.entries.map { it.code to ContractFurni(use, contracts) }

    fun facts(next: VariableFacts): VariableFacts = ChestFacts(next, chests, deals)

    fun packets(
        registry: RoomRegistry,
        masks: (Room) -> WiredMasks,
    ): ChestPackets {
        val rules =
            ContractRules { wall, type ->
                host.definition(wall, type)?.canTrade == true
            }
        val desk =
            ChestDesk(
                furni,
                ChestSettings(chests, host),
                deals,
                ChestWithdrawals(chests, deals, moves, notices, host) { masks(it.room) },
                ChestLogs(store, persist),
                ContractWindow(contracts, rules, masks, host::filtered),
                chests,
            )
        return ChestPackets(registry, desk, masks, levelOf)
    }

    private companion object {
        const val CONTRACT_DIALOGS = """
        wf_contract_payment 110 i=-1,0,0 f=100:100 basic ctx=0
        wf_contract_reward 111 i=-1,0,0 f=100:100 basic ctx=0
        wf_contract_trade 112 i=-1,0,0 f=100:100 basic ctx=0
"""
    }
}
