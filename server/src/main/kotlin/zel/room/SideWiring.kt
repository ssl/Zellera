package zel.room

import zel.core.rules.rules
import zel.protocol.typed.PacketPackage
import zel.room.entry.DoorCheck
import zel.room.furni.behaviour.use.PostIts
import zel.room.persist.RoomVotes
import zel.room.protocol.PlayerActionPackets
import zel.room.protocol.RoomSidePackets
import zel.room.rights.SettingsEditor
import zel.room.side.RoomAdvertising
import zel.room.side.RoomCompetitions
import zel.room.side.RoomSideActions
import zel.room.side.RoomSideServices
import zel.room.side.YoutubeDisplays
import zel.room.trade.TradePackets
import zel.room.trade.TradeRequests

internal fun sidePackets(
    services: RoomEngineServices,
    core: CoreWiring,
    registry: RoomRegistry,
    side: RoomSidePackets,
    settings: SettingsEditor,
    postIts: PostIts,
): List<PacketPackage> =
    listOf(
        side,
        RoomSideActions(
            registry,
            core.leaving,
            services.listings,
            RoomAdvertising(
                services.dataSource,
                registry,
                services.listings,
                services.catalog,
                services.wordFilter,
                core.nowSeconds,
                { services.config.getValue(PROMOTION_BADGE_KEY, "") },
                { services.units.text(BLOCKED_TEXT_KEY, BLOCKED_TEXT) },
            ),
            RoomCompetitions(services.dataSource, services.listings, core.nowSeconds, services.progress),
            YoutubeDisplays(services.youtube),
            postIts,
            side,
            core.groupRooms,
            settings::applied,
            RoomSideServices(
                services.navigator,
                services.catalog,
                { services.config.getInt(STAFF_PICKS_KEY) },
                services.progress,
            ),
        ),
        TradePackets(
            registry,
            core.leaving,
            core.trades,
            core.tradeMessages,
            core.entryStore,
            TradeRequests(core.trades, { rules.trading }, services.clients::onlinePlayer) { scope, playerId ->
                core.leaving.doorOf(scope.room).levelOf(playerId) >= DoorCheck.RIGHTS
            },
        ),
        PlayerActionPackets(
            registry,
            core.leaving,
            core.life,
            core.entryStore,
            services.clients::onlinePlayer,
            RoomVotes(services.dataSource)::vote,
        ),
    )

private const val PROMOTION_BADGE_KEY = "room.advertising.badge_code"
private const val STAFF_PICKS_KEY = "navigator.staff_picks.folder_id"
private const val BLOCKED_TEXT_KEY = "room.promotion.text_refused"
private const val BLOCKED_TEXT = "That text has words in it that we don't allow. Please reword it and try again."
