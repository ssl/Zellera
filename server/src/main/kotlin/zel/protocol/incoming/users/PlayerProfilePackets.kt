package zel.protocol.incoming.users

import org.slf4j.LoggerFactory
import zel.db.SqlFailure
import zel.game.achievements.GameEvents
import zel.game.achievements.count
import zel.game.messenger.RelationSummary
import zel.game.players.Gender
import zel.game.players.Mottos
import zel.game.players.Player
import zel.game.players.PlayerBadge
import zel.game.players.PlayerProfile
import zel.game.players.figure.Figure
import zel.game.players.figure.LookValidation
import zel.game.players.inventory.BadgeCase
import zel.game.players.inventory.BadgeLeaderboard
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.net.clients.ClientConnection
import zel.protocol.generated.ChangeMottoInCodec
import zel.protocol.generated.GetBadgeInfoInCodec
import zel.protocol.generated.GetBadgeLeaderboardInCodec
import zel.protocol.generated.GetExtendedProfileByNameInCodec
import zel.protocol.generated.GetExtendedProfileInCodec
import zel.protocol.generated.GetRelationshipStatusInfoInCodec
import zel.protocol.generated.GetSelectedBadgesInCodec
import zel.protocol.generated.GetWardrobeInCodec
import zel.protocol.generated.SaveWardrobeOutfitIn
import zel.protocol.generated.SaveWardrobeOutfitInCodec
import zel.protocol.generated.SetActivatedBadgesIn
import zel.protocol.generated.SetActivatedBadgesInCodec
import zel.protocol.generated.UpdateFigureDataInCodec
import zel.protocol.outgoing.OutgoingMessage
import zel.protocol.outgoing.rooms.users.UserChange
import zel.protocol.outgoing.users.BadgeInfo
import zel.protocol.outgoing.users.BadgeLeaderboardResult
import zel.protocol.outgoing.users.ProfileFriends
import zel.protocol.outgoing.users.UpdateUserLook
import zel.protocol.outgoing.users.UserBadges
import zel.protocol.outgoing.users.UserProfile
import zel.protocol.outgoing.users.UserWardrobe
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.player
import zel.protocol.typed.signedIn
import zel.server.GameServices

class PlayerProfilePackets(
    private val services: GameServices,
) : PacketPackage {
    private val wearCooldown = ClientCooldown("users.wear-badge", WEAR_BADGE_GAP_MS)
    private val profileCooldown = ClientCooldown("users.profile", PROFILE_READ_GAP_MS)
    private val relationsCooldown = ClientCooldown("users.relations", PROFILE_READ_GAP_MS)
    private val badgesCooldown = ClientCooldown("users.worn-badges", PROFILE_READ_GAP_MS)

    private val players get() = checkNotNull(services.game.players)

    override val name = "player-profile"

    override val routes =
        listOf(
            PacketRoute(GetExtendedProfileInCodec) { session, m ->
                if (m.playerId > 0 && profileCooldown.acquire(session.client)) {
                    profileOf(m.playerId, session.player, m.openProfile)?.let(session.client::send)
                }
            },
            PacketRoute(GetExtendedProfileByNameInCodec) { session, m ->
                if (profileCooldown.acquire(session.client)) {
                    profileFor(session.player, m.name, players::find, players::readProfile)?.let(session.client::send)
                }
            },
            PacketRoute(GetRelationshipStatusInfoInCodec) { session, m ->
                if (m.userId > 0 && relationsCooldown.acquire(session.client)) {
                    session.client.send(relationsOf(session.player, m.userId))
                }
            },
            PacketRoute(GetSelectedBadgesInCodec) { session, m ->
                if (m.userId > 0 && badgesCooldown.acquire(session.client)) session.client.send(wornBadgesOf(m.userId))
            },
            PacketRoute(GetBadgeInfoInCodec) { session, m ->
                if (m.badgeCode.isNotEmpty()) session.client.send(badgeInfo(session.player, m.badgeCode))
            },
            PacketRoute(GetBadgeLeaderboardInCodec) { session, m ->
                leaderboard(session.player, m.type, m.rarity, m.page, m.size)?.let(session.client::send)
            },
            PacketRoute(SetActivatedBadgesInCodec) { session, m ->
                if (wearCooldown.acquire(session.client)) wearBadges(session.player, m)
            },
            PacketRoute(GetWardrobeInCodec) { session, _ ->
                session.client.send(UserWardrobe(session.player.inventory.wardrobe))
            },
            PacketRoute(SaveWardrobeOutfitInCodec) { session, m -> saveOutfit(session.player, m) },
            PacketRoute(ChangeMottoInCodec) { session, m -> changeMotto(session.client, m.motto) },
            PacketRoute(UpdateFigureDataInCodec) { session, m -> changeLook(session.client, m.genderCode, m.look) },
        )

    private fun profileOf(
        playerId: Int,
        viewer: Player,
        open: Boolean,
    ): UserProfile? =
        players.find(playerId)?.let { UserProfile(it, viewer, open) }
            ?: players.readProfile(playerId)?.let { UserProfile(it, viewer, open) }

    fun profileFor(
        viewer: Player,
        name: String?,
        online: (String) -> Player?,
        stored: (String) -> PlayerProfile?,
    ): OutgoingMessage<*>? {
        val wanted = name?.trim().orEmpty()
        if (wanted.isEmpty() || wanted.length > MAX_NAME_LENGTH) return null
        return online(wanted)?.let { UserProfile(it, viewer) } ?: stored(wanted)?.let { UserProfile(it, viewer) }
    }

    /** A hidden profile shows its relations to nobody but its owner. */
    private fun relationsOf(
        viewer: Player,
        playerId: Int,
    ): ProfileFriends {
        val owner = players.find(playerId)
        val hidden = owner?.stats?.settings?.privateProfile ?: profileHidden(playerId)
        val relations =
            RelationSummary.shownTo(viewer.profile.id, playerId, hidden) {
                owner?.let { RelationSummary.of(it.messenger.friends.values) }
                    ?: checkNotNull(services.game.messengerModule).friends.relationSummary(playerId)
            }
        return ProfileFriends(playerId, relations)
    }

    /** A profile whose setting cannot be read counts as hidden. */
    private fun profileHidden(playerId: Int): Boolean =
        try {
            services.database.queries
                .select(HIDDEN_SQL, playerId)
                .first { it.getBoolean(1) } ?: false
        } catch (e: SqlFailure) {
            log.error("Reading the profile setting of player {} failed", playerId, e)
            true
        }

    private fun wornBadgesOf(playerId: Int): UserBadges {
        val worn =
            services
                .onlinePlayer(playerId)
                ?.inventory
                ?.badges
                ?.worn
                ?: BadgeCase.wornByOffline(services, playerId)
        return UserBadges(worn, playerId, services.badgeCaches.ownerCounts)
    }

    private fun badgeInfo(
        player: Player,
        code: String,
    ): BadgeInfo {
        val owned = player.inventory.badges.find(code)
        return BadgeInfo(owned?.id ?: 0, code, services.badgeCaches.ownerCounts)
    }

    /** A page past the end or an odd page size gets no answer. */
    private fun leaderboard(
        player: Player,
        type: Int,
        requestedRarity: Int,
        page: Int,
        size: Int,
    ): BadgeLeaderboardResult? {
        if (size !in MIN_LEADERBOARD_SIZE..MAX_LEADERBOARD_SIZE || page < 0) return null
        val rarity = if (type == BadgeLeaderboard.TYPE_RARITY) requestedRarity else -1
        val board = services.badgeCaches.leaderboard.board(type, rarity) ?: return null
        val total = board.total()
        if (page > 0 && page * size >= total) return null
        val profile = player.profile
        val own = BadgeLeaderboard.Entry(profile.id, profile.name, profile.figure, board.scoreOf(profile.id))
        val rank = board.rankOf(profile.id)
        return BadgeLeaderboardResult(type, rarity, page, size, total, board.page(page, size), own, rank)
    }

    /** A slot out of range drops the whole request. */
    private fun wearBadges(
        player: Player,
        m: SetActivatedBadgesIn,
    ) {
        val wanted =
            listOf(
                m.slot1 to m.badge1,
                m.slot2 to m.badge2,
                m.slot3 to m.badge3,
                m.slot4 to m.badge4,
                m.slot5 to m.badge5,
            )
        if (wanted.any { (slot, _) -> slot !in 1..BADGE_SLOTS }) return
        val badges = player.inventory.badges
        val wornBefore = badges.worn.map { it.code }.toSet()
        val chosen = LinkedHashMap<PlayerBadge, Int>()
        for ((slot, code) in wanted) {
            val badge = badges.find(code) ?: continue
            if (badge in chosen || slot in chosen.values) continue
            chosen[badge] = slot
        }
        badges.wearOnly(chosen)
        val worn = chosen.keys.toList()
        if (worn.any { it.code !in wornBefore }) PlayerActions.progress(player, CountedAction.WEAR_BADGE, 1)
        val update = UserBadges(worn, player.profile.id, services.badgeCaches.ownerCounts)
        val room = player.rooms.roomOf(player.profile.id)
        if (room == null) player.client?.send(update) else update.compose()?.let { player.rooms.send(room, it) }
    }

    private fun saveOutfit(
        player: Player,
        m: SaveWardrobeOutfitIn,
    ) {
        if (m.slotId !in 0..MAX_WARDROBE_SLOT || !Figure.isWellFormed(m.look)) return
        val gender = Gender.entries.firstOrNull { it.name == m.gender } ?: return
        player.inventory.wardrobe.save(m.slotId, m.look, gender)
    }

    private fun changeMotto(
        client: ClientConnection,
        typed: String,
    ) {
        val player = client.signedIn
        val wordFilter = checkNotNull(services.game.wordFilter)
        val maxLength = Mottos.maxLength(services.config)
        val motto = Mottos.accepted(typed, { wordFilter.filter(it, player) }, maxLength) ?: return
        val profile = player.profile
        val changed = motto != profile.motto
        profile.motto = motto
        try {
            services.database.queries.write(Mottos.STORE_SQL, motto, profile.id)
        } catch (e: SqlFailure) {
            log.error("Storing the motto of player {} failed", profile.id, e)
        }
        val rooms = services.game.rooms
        if (rooms.roomOf(profile.id) != null) {
            client.send(UserChange(player, -1))
            rooms.appearanceChanged(profile.id)
            if (changed) PlayerActions.progress(player, CountedAction.CHANGE_MOTTO, 1)
        }
        checkNotNull(services.game.achievements).count(player, GameEvents.PLAYER_MOTTO_CHANGED)
    }

    private fun changeLook(
        client: ClientConnection,
        genderCode: String,
        look: String,
    ) {
        val player = client.signedIn
        val gender = Gender.ofCode(genderCode) ?: return
        val unchecked = LookValidation.validateOnChangeLooks && LookValidation.refusesNewLooks
        if (unchecked || !Figure.isWellFormed(look)) return
        val profile = player.profile
        val before = profile.figure
        profile.figure =
            if (LookValidation.validateOnChangeLooks) LookValidation.allowedLook(player, look, gender.name) else look
        profile.gender = gender
        val inRoom = services.game.rooms.roomOf(profile.id) != null
        if (countsFigureChange(before, profile.figure, inRoom)) {
            PlayerActions.progress(player, CountedAction.CHANGE_FIGURE, 1)
        }
        services.threading.run(profile)
        client.send(UpdateUserLook(player))
        services.game.rooms.appearanceChanged(profile.id)
        player.messenger.connectionChanged(player, player.isOnline, inRoom)
        checkNotNull(services.game.achievements).count(player, GameEvents.PLAYER_LOOK_CHANGED)
    }

    companion object {
        private val log = LoggerFactory.getLogger(PlayerProfilePackets::class.java)
        private const val WEAR_BADGE_GAP_MS = 250L
        private const val PROFILE_READ_GAP_MS = 200L
        private const val HIDDEN_SQL = "SELECT hide_profile FROM player_preferences WHERE player_id = ?"
        private const val BADGE_SLOTS = 5
        private const val MAX_WARDROBE_SLOT = 8
        private const val MIN_LEADERBOARD_SIZE = 10
        private const val MAX_LEADERBOARD_SIZE = 50

        const val MAX_NAME_LENGTH = 64

        /** A new look counts for the quest only when it is seen, so only in a room. */
        internal fun countsFigureChange(
            before: String?,
            after: String?,
            inRoom: Boolean,
        ): Boolean = inRoom && before != after
    }
}
