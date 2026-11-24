package zel.room.side

import zel.game.achievements.GameEvents
import zel.game.permissions.Privilege
import zel.game.permissions.isAnyOwner
import zel.game.players.Player
import zel.protocol.generated.AddSpamWallPostItInCodec
import zel.protocol.generated.CancelEventInCodec
import zel.protocol.generated.CompetitionEntrySubmitResultOutCodec
import zel.protocol.generated.CompetitionRoomsDataOutCodec
import zel.protocol.generated.CompetitionRoomsSearchInCodec
import zel.protocol.generated.CompetitionVotingInfoOutCodec
import zel.protocol.generated.ControlYoutubeDisplayPlaybackInCodec
import zel.protocol.generated.EditEventInCodec
import zel.protocol.generated.ForwardToACompetitionRoomInCodec
import zel.protocol.generated.ForwardToASubmittableRoomInCodec
import zel.protocol.generated.ForwardToRandomCompetitionRoomInCodec
import zel.protocol.generated.GetIsPlayerPartOfCompetitionInCodec
import zel.protocol.generated.GetRoomAdPurchaseInfoInCodec
import zel.protocol.generated.GetYoutubeDisplayStatusInCodec
import zel.protocol.generated.IsPlayerPartOfCompetitionOutCodec
import zel.protocol.generated.NavigatorSearchResultBlocksOutCodec
import zel.protocol.generated.NoOwnedRoomsAlertOut
import zel.protocol.generated.NoOwnedRoomsAlertOutCodec
import zel.protocol.generated.OpenMysteryTrophyInCodec
import zel.protocol.generated.PurchaseRoomAdInCodec
import zel.protocol.generated.RoomAdPurchaseInfoOutCodec
import zel.protocol.generated.RoomCompetitionInitInCodec
import zel.protocol.generated.RoomEventCancelOut
import zel.protocol.generated.RoomEventCancelOutCodec
import zel.protocol.generated.RoomForwardOut
import zel.protocol.generated.RoomForwardOutCodec
import zel.protocol.generated.RoomInfoUpdatedOut
import zel.protocol.generated.RoomInfoUpdatedOutCodec
import zel.protocol.generated.RoomSettingsSavedOut
import zel.protocol.generated.RoomSettingsSavedOutCodec
import zel.protocol.generated.SetYoutubeDisplayPlaylistInCodec
import zel.protocol.generated.SubmitRoomToCompetitionInCodec
import zel.protocol.generated.ToggleStaffPickInCodec
import zel.protocol.generated.UpdateRoomCategoryAndTradeSettingsInCodec
import zel.protocol.generated.VoteForRoomInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.entry.DoorCheck
import zel.room.entry.RoomLeaving
import zel.room.entry.withRightsLevel
import zel.room.furni.behaviour.use.PostIts
import zel.room.listing.RoomListings
import zel.room.port.GroupRoomStore
import zel.room.protocol.RoomSidePackets

class RoomSideActions(
    private val registry: RoomRegistry,
    private val leaving: RoomLeaving,
    private val listings: RoomListings,
    ads: RoomAdvertising,
    private val competitions: RoomCompetitions,
    youtube: YoutubeDisplays,
    notes: PostIts,
    private val side: RoomSidePackets,
    private val store: GroupRoomStore,
    private val settingsApplied: (Int) -> Unit,
    private val services: RoomSideServices,
) : PacketPackage {
    override val name = "room-side-actions"

    override val routes =
        listOf(
            PacketRoute(GetRoomAdPurchaseInfoInCodec) { session, _ ->
                session.client.player?.let { session.send(RoomAdPurchaseInfoOutCodec, ads.purchaseInfo(it)) }
            },
            PacketRoute(PurchaseRoomAdInCodec) { session, m ->
                val purchase =
                    RoomAdvertising.Purchase(m.pageId, m.itemId, m.roomId, m.title, m.description, m.categoryId)
                session.client.player?.let { ads.buy(session.client, it, purchase) }
            },
            PacketRoute(EditEventInCodec) { session, m ->
                val player = session.client.player ?: return@PacketRoute
                ads.edit(session.client, player, m.id, m.promotionName, m.promotionDescription)
            },
            PacketRoute(CancelEventInCodec) { session, m ->
                val player = session.client.player ?: return@PacketRoute
                if (ads.cancel(player, m.int1) && registry.roomOf(player.profile.id) != m.int1) {
                    session.send(RoomEventCancelOutCodec, RoomEventCancelOut)
                }
            },
            PacketRoute(ToggleStaffPickInCodec) { session, m -> staffPick(session, m.int1) },
            PacketRoute(UpdateRoomCategoryAndTradeSettingsInCodec) { session, m ->
                categoryAndTrade(session, m.roomId, m.categoryId, m.tradeMode)
            },
            PacketRoute(GetIsPlayerPartOfCompetitionInCodec) { session, m ->
                val player = session.client.player ?: return@PacketRoute
                session.send(IsPlayerPartOfCompetitionOutCodec, competitions.partOf(m.goalCode, player.profile.id))
            },
            PacketRoute(RoomCompetitionInitInCodec) { session, _ -> initCompetition(session) },
            PacketRoute(SubmitRoomToCompetitionInCodec) { session, m ->
                submitToCompetition(session, m.goalCode, m.level)
            },
            PacketRoute(VoteForRoomInCodec) { session, m -> voteForRoom(session, m.goalCode) },
            PacketRoute(CompetitionRoomsSearchInCodec) { session, m ->
                val competition = competitions.current()?.takeIf { it.id == m.goalId } ?: return@PacketRoute
                val (blocks, counts) = competitions.standing.page(competition, m.pageIndex)
                session.send(NavigatorSearchResultBlocksOutCodec, blocks)
                session.send(CompetitionRoomsDataOutCodec, counts)
            },
            PacketRoute(ForwardToACompetitionRoomInCodec) { session, m ->
                val competition = competitions.current()?.takeIf { it.code == m.goalCode } ?: return@PacketRoute
                if (competitions.isEntered(competition, m.roomId)) forward(session, m.roomId)
            },
            PacketRoute(ForwardToRandomCompetitionRoomInCodec) { session, m ->
                val competition = competitions.current()?.takeIf { it.code == m.goalCode } ?: return@PacketRoute
                val playerId =
                    session.client.player
                        ?.profile
                        ?.id ?: return@PacketRoute
                competitions.roomToJudge(competition, playerId)?.let { forward(session, it) }
            },
            PacketRoute(ForwardToASubmittableRoomInCodec) { session, _ ->
                if (competitions.current() == null) return@PacketRoute
                val owned =
                    session.client.player
                        ?.let { listings.ofOwner(it.profile.id) }
                        .orEmpty()
                val roomId = owned.firstOrNull()?.id
                if (roomId !=
                    null
                ) {
                    forward(session, roomId)
                } else {
                    session.client.send(NoOwnedRoomsAlertOutCodec, NoOwnedRoomsAlertOut)
                }
            },
            PacketRoute(GetYoutubeDisplayStatusInCodec) { session, m ->
                inRoom(session) { scope, player, _ -> youtube.status(scope, player.profile.id, m.itemId) }
            },
            PacketRoute(SetYoutubeDisplayPlaylistInCodec) { session, m ->
                inRoom(session) { scope, _, level ->
                    if (level >= DoorCheck.RIGHTS) youtube.choose(scope, m.itemId, m.playlistId)
                }
            },
            PacketRoute(ControlYoutubeDisplayPlaybackInCodec) { session, m ->
                inRoom(session) { scope, _, level ->
                    if (level >= DoorCheck.RIGHTS) youtube.control(scope, m.itemId, m.state)
                }
            },
            PacketRoute(AddSpamWallPostItInCodec) { session, m ->
                inRoom(
                    session,
                ) { scope, player, level -> notes.writeOnPole(scope, player, level, m.itemId, m.color, m.text) }
            },
            PacketRoute(OpenMysteryTrophyInCodec) { session, m ->
                inRoom(session) { scope, player, _ ->
                    val catalog = services.catalog() ?: return@inRoom
                    notes.engrave(scope, player, m.itemId) { definition ->
                        catalog.engravings.write(player, definition, m.text)
                    }
                }
            },
        )

    private fun forward(
        session: PacketSession,
        roomId: Int,
    ) = session.send(RoomForwardOutCodec, RoomForwardOut(roomId))

    private fun staffPick(
        session: PacketSession,
        roomId: Int,
    ) {
        val player = session.client.player ?: return
        if (!player.hasPermission(Privilege.NAVIGATOR_STAFF_PICKS)) return
        val room = listings.room(roomId) ?: return
        val picked = !room.isStaffPicked
        store.staffPick(roomId, picked)
        settingsApplied(roomId)
        val folder = services.navigator()?.publicFolders?.get(services.staffPicksCategory())
        if (picked) {
            folder?.add(roomId)
            services.progress(room.ownerId, GameEvents.ROOM_STAFF_PICK, 1)
        } else {
            folder?.remove(roomId)
        }
        side.sendInfo(player, roomId, enterRoom = false, forward = true)
    }

    private fun categoryAndTrade(
        session: PacketSession,
        roomId: Int,
        categoryId: Int,
        tradeMode: Int,
    ) {
        val player = session.client.player ?: return
        val room = listings.room(roomId) ?: return
        val mayChange = room.ownerId == player.profile.id || player.isAnyOwner
        val category = listings.category(categoryId)
        if (!mayChange || category == null || category.minRank > player.profile.rank.id) return
        if (category.isStaffOnly && !player.hasPermission(Privilege.MODERATION_TOOL)) return
        if (tradeMode !in TRADE_MODES) return
        store.categoryAndTrade(roomId, categoryId, tradeMode)
        settingsApplied(roomId)
        session.send(RoomSettingsSavedOutCodec, RoomSettingsSavedOut(roomId))
        if (registry.loaded(roomId) == null) session.send(RoomInfoUpdatedOutCodec, RoomInfoUpdatedOut(roomId))
    }

    private fun initCompetition(session: PacketSession) {
        val player = session.client.player ?: return
        val competition = competitions.current() ?: return
        val playerId = player.profile.id
        val roomId = registry.roomOf(playerId) ?: return
        val isOwner = listings.room(roomId)?.ownerId == playerId
        val phase = competitions.phase(competition)
        if (phase == Competition.Phase.ENTRIES && isOwner) {
            competitions.firstLook(competition, roomId, playerId)?.let {
                session.send(CompetitionEntrySubmitResultOutCodec, it)
            }
        }
        if (phase == Competition.Phase.VOTING && !isOwner && competitions.isEntered(competition, roomId)) {
            sendVotingInfo(session, competition, player)
        }
    }

    private fun submitToCompetition(
        session: PacketSession,
        code: String,
        level: Int,
    ) {
        val playerId =
            session.client.player
                ?.profile
                ?.id ?: return
        val competition = competitions.current()?.takeIf { it.code == code } ?: return
        if (competitions.phase(competition) != Competition.Phase.ENTRIES) return
        val roomId = registry.roomOf(playerId) ?: return
        if (listings.room(roomId)?.ownerId == playerId) sendEntryStep(session, competition, roomId, playerId, level)
    }

    private fun sendEntryStep(
        session: PacketSession,
        competition: Competition,
        roomId: Int,
        ownerId: Int,
        level: Int,
    ) {
        competitions.entryStep(competition, roomId, ownerId, level)?.let {
            session.send(CompetitionEntrySubmitResultOutCodec, it)
        }
    }

    private fun voteForRoom(
        session: PacketSession,
        code: String,
    ) {
        val player = session.client.player ?: return
        val competition = competitions.current()?.takeIf { it.code == code } ?: return
        if (competitions.phase(competition) != Competition.Phase.VOTING) return
        val roomId = registry.roomOf(player.profile.id) ?: return
        if (player.hasPermission(Privilege.PERK_VOTE_IN_COMPETITIONS)) {
            competitions.vote(competition, roomId, player.profile.id)
        }
        sendVotingInfo(session, competition, player)
    }

    private fun sendVotingInfo(
        session: PacketSession,
        competition: Competition,
        player: Player,
    ) {
        val mayVote = player.hasPermission(Privilege.PERK_VOTE_IN_COMPETITIONS)
        session.send(CompetitionVotingInfoOutCodec, competitions.votingInfo(competition, player.profile.id, mayVote))
    }

    private fun inRoom(
        session: PacketSession,
        block: (RoomScope, Player, Int) -> Unit,
    ) = registry.withRightsLevel(session, leaving, block)

    private companion object {
        val TRADE_MODES = 0..2
    }
}
