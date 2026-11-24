package zel.protocol.navigator

import zel.content.ClientStrings
import zel.core.Configuration
import zel.game.navigation.BlockStyle
import zel.game.navigation.DisplayMode
import zel.game.navigation.EventCategory
import zel.game.navigation.NavigatorSavedSearch
import zel.game.players.PlayerSettings.NavigatorWindow
import zel.protocol.generated.CategoriesWithVisitorCountOut
import zel.protocol.generated.CategoriesWithVisitorCountOutCodec
import zel.protocol.generated.GetCategoriesWithPlayerCountInCodec
import zel.protocol.generated.GetPlayerEventCatsInCodec
import zel.protocol.generated.GetPlayerFlatCatsInCodec
import zel.protocol.generated.GetPopularRoomTagsInCodec
import zel.protocol.generated.NavigatorAddCollapsedCategoryInCodec
import zel.protocol.generated.NavigatorAddSavedSearchInCodec
import zel.protocol.generated.NavigatorCollapsedCategoriesOut
import zel.protocol.generated.NavigatorCollapsedCategoriesOutCodec
import zel.protocol.generated.NavigatorDeleteSavedSearchInCodec
import zel.protocol.generated.NavigatorLiftedRoomsOut
import zel.protocol.generated.NavigatorLiftedRoomsOutCodec
import zel.protocol.generated.NavigatorMetaDataOut
import zel.protocol.generated.NavigatorMetaDataOutCodec
import zel.protocol.generated.NavigatorRemoveCollapsedCategoryInCodec
import zel.protocol.generated.NavigatorSavedSearchesOutCodec
import zel.protocol.generated.NavigatorSetSearchCodeViewModeInCodec
import zel.protocol.generated.NewNavigatorInitInCodec
import zel.protocol.generated.NewNavigatorPreferencesOutCodec
import zel.protocol.generated.PlayerEventCatsOut
import zel.protocol.generated.PlayerEventCatsOutCodec
import zel.protocol.generated.PlayerFlatCatsOutCodec
import zel.protocol.generated.PopularRoomTagsResultOut
import zel.protocol.generated.PopularRoomTagsResultOutCodec
import zel.protocol.generated.SetNewNavigatorWindowPreferencesIn
import zel.protocol.generated.SetNewNavigatorWindowPreferencesInCodec
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.protocol.typed.player
import zel.room.listing.RoomListings
import zel.room.port.RoomPort
import kotlin.math.abs

class NavigatorPackets(
    private val rooms: RoomListings,
    private val config: Configuration,
    private val live: () -> RoomPort,
) : PacketPackage {
    override val name = "navigator"

    private val saveSearchCooldown = ClientCooldown("navigator.addsavedsearch", SAVE_SEARCH_GAP_MS)
    private val deleteSearchCooldown = ClientCooldown("navigator.deletesavedsearch", DELETE_SEARCH_GAP_MS)
    private val sectionCooldown = ClientCooldown("navigator.section", SECTION_GAP_MS)
    private val windowCooldown = ClientCooldown("navigator.window", WINDOW_GAP_MS)

    override val routes =
        listOf(
            PacketRoute(NewNavigatorInitInCodec) { s, _ -> initNavigator(s) },
            PacketRoute(SetNewNavigatorWindowPreferencesInCodec) { s, m ->
                if (windowCooldown.acquire(s.client)) rememberWindow(s, m)
            },
            PacketRoute(GetPlayerEventCatsInCodec) { s, _ ->
                val listed =
                    EventCategory.configured(config).map {
                        PlayerEventCatsOut.CategoriesItem(it.id, it.caption, it.isVisible)
                    }
                s.send(PlayerEventCatsOutCodec, PlayerEventCatsOut(listed))
            },
            PacketRoute(GetPlayerFlatCatsInCodec) { s, _ ->
                s.send(PlayerFlatCatsOutCodec, NavigatorMessages.flatCategories(rooms.categoriesInOrder()))
            },
            PacketRoute(GetCategoriesWithPlayerCountInCodec) { s, _ -> countVisitors(s) },
            PacketRoute(GetPopularRoomTagsInCodec) { s, _ ->
                val ranked = rooms.tags().mapIndexed { index, tag -> PopularRoomTagsResultOut.TagsItem(tag, index + 1) }
                s.send(PopularRoomTagsResultOutCodec, PopularRoomTagsResultOut(ranked))
            },
            PacketRoute(NavigatorAddCollapsedCategoryInCodec) { s, m ->
                if (sectionCooldown.acquire(s.client)) layouts(s).display(m.category, DisplayMode.COLLAPSED)
            },
            PacketRoute(NavigatorRemoveCollapsedCategoryInCodec) { s, m ->
                if (sectionCooldown.acquire(s.client)) layouts(s).display(m.category, DisplayMode.VISIBLE)
            },
            PacketRoute(NavigatorSetSearchCodeViewModeInCodec) { s, m ->
                if (!sectionCooldown.acquire(s.client)) return@PacketRoute
                val known = rooms.categoryByKey(m.category)?.captionKey ?: m.category
                layouts(s).setStyle(known, if (m.viewMode == 1) BlockStyle.TILES else BlockStyle.ROWS)
            },
            PacketRoute(NavigatorAddSavedSearchInCodec) { s, m ->
                if (saveSearchCooldown.acquire(s.client)) saveSearch(s, m.searchCode, m.filter)
            },
            PacketRoute(NavigatorDeleteSavedSearchInCodec) { s, m ->
                if (deleteSearchCooldown.acquire(s.client)) deleteSearch(s, m.searchId)
            },
        )

    private fun layouts(s: PacketSession) = s.player.stats.navigatorLayouts

    private fun initNavigator(s: PacketSession) {
        val contexts = CONTEXTS.map { NavigatorMetaDataOut.TopLevelContextsItem(it, emptyList()) }
        s.send(NavigatorMetaDataOutCodec, NavigatorMetaDataOut(contexts))
        s.send(NavigatorLiftedRoomsOutCodec, NavigatorLiftedRoomsOut(emptyList()))
        s.send(NavigatorCollapsedCategoriesOutCodec, NavigatorCollapsedCategoriesOut(collapsedByDefault()))
        sendSavedSearches(s)
        val window = s.player.stats.settings.navigatorWindow
        if (window.isPlaced) s.send(NewNavigatorPreferencesOutCodec, NavigatorMessages.windowPreferences(window))
    }

    private fun rememberWindow(
        s: PacketSession,
        m: SetNewNavigatorWindowPreferencesIn,
    ) {
        val window = NavigatorWindow(m.x, m.y, m.width, m.height, m.openSearches, m.resultsMode)
        val plausible =
            window.isPlaced && window.width <= MAX_WINDOW_SIDE && window.height <= MAX_WINDOW_SIDE &&
                abs(window.x) <= MAX_WINDOW_SIDE && abs(window.y) <= MAX_WINDOW_SIDE &&
                window.resultsMode in RESULTS_MODES
        if (plausible) {
            s.player.stats.settings
                .update(window)
        }
    }

    private fun sendSavedSearches(s: PacketSession) {
        s.send(NavigatorSavedSearchesOutCodec, NavigatorMessages.savedSearches(s.player.profile.savedSearches))
    }

    private fun saveSearch(
        s: PacketSession,
        code: String,
        filter: String,
    ) {
        val profile = s.player.profile
        if (profile.savedSearches.size < MAX_SAVED_SEARCHES) {
            val limit = NavigatorTextLimit.SAVED_SEARCH
            profile.saveSearch(NavigatorSavedSearch(limit.clean(code), limit.clean(filter)))
        }
        sendSavedSearches(s)
    }

    private fun deleteSearch(
        s: PacketSession,
        searchId: Int,
    ) {
        val profile = s.player.profile
        val found = profile.savedSearches.firstOrNull { it.id == searchId } ?: return
        profile.deleteSearch(found)
        sendSavedSearches(s)
    }

    private fun countVisitors(s: PacketSession) {
        val port = live()
        val visitors =
            port.loadedRoomIds().groupBy(port::categoryOf).mapValues { (_, ids) -> ids.sumOf(port::userCount) }
        val counted =
            rooms.categoriesForRank(s.player.profile.rank.id).map {
                CategoriesWithVisitorCountOut.CategoriesItem(it.id, visitors[it.id] ?: 0, it.maxUserCount)
            }
        s.send(CategoriesWithVisitorCountOutCodec, CategoriesWithVisitorCountOut(counted))
    }

    private fun collapsedByDefault(): List<String> {
        val named = ClientStrings[ClientStrings.COLLAPSED_CATEGORY] ?: return COLLAPSED_CATEGORIES
        val at = COLLAPSED_CATEGORIES.indexOf(COLLAPSED_AFTER) + 1
        return COLLAPSED_CATEGORIES.take(at) + named + COLLAPSED_CATEGORIES.drop(at)
    }

    private companion object {
        const val MAX_SAVED_SEARCHES = 50
        const val SAVE_SEARCH_GAP_MS = 1000L
        const val DELETE_SEARCH_GAP_MS = 500L
        const val SECTION_GAP_MS = 100L
        const val WINDOW_GAP_MS = 500L

        /** Pixels; wider than any screen the client runs on. */
        const val MAX_WINDOW_SIDE = 10_000
        val RESULTS_MODES = 0..1
        val CONTEXTS = listOf("official_view", "landing_view", "roomads_view", "myworld_view")

        const val COLLAPSED_AFTER = "category__global personal space"

        val COLLAPSED_CATEGORIES =
            listOf(
                "new_ads",
                "friend_finding",
                "staffpicks",
                "with_friends",
                "with_rights",
                "query",
                "recommended",
                "my_groups",
                "favorites",
                "history",
                "top_promotions",
                "campaign_target",
                "friends_rooms",
                "groups",
                "metadata",
                "history_freq",
                "highest_score",
                "competition",
                "category__Agencies",
                "category__Role Playing",
                "category__Global Chat & Discussi",
                "category__GLOBAL BUILDING AND DE",
                "category__global party",
                "category__global games",
                "category__global fansite",
                "category__global help",
                "category__Trading",
                "category__global personal space",
                "category__TRADING",
                "category__global official",
                "category__global trade",
                "category__global reviews",
                "category__global bc",
                "category__global personal space",
                "eventcategory__Hottest Events",
                "eventcategory__Parties & Music",
                "eventcategory__Role Play",
                "eventcategory__Help Desk",
                "eventcategory__Trading",
                "eventcategory__Games",
                "eventcategory__Debates & Discuss",
                "eventcategory__Grand Openings",
                "eventcategory__Friending",
                "eventcategory__Jobs",
                "eventcategory__Group Events",
            )
    }
}
