package zel.protocol.navigator

import org.slf4j.LoggerFactory
import zel.db.orFallback
import zel.game.navigation.BlockStyle
import zel.game.navigation.DisplayMode
import zel.game.navigation.LandingView
import zel.game.navigation.Navigator
import zel.game.navigation.NavigatorBlock
import zel.game.navigation.NavigatorView
import zel.game.navigation.OfficialView
import zel.game.navigation.RoomSearchField
import zel.game.navigation.SearchAction
import zel.game.navigation.seesHiddenRooms
import zel.game.players.Player
import zel.room.listing.ListedRoom
import zel.room.listing.RoomCategory
import zel.room.listing.RoomListings

class NavigatorSearch(
    private val rooms: RoomListings,
    private val navigator: Navigator,
) {
    fun answer(
        player: Player,
        requestedView: String,
        requestedText: String,
    ): SearchAnswer? {
        if (requestedView.length > LONGEST_VIEW) return null
        val query = requestedText.take(LONGEST_TEXT)
        val view = if (requestedView == "query" && query.isEmpty()) LandingView.VIEW_NAME else requestedView
        return when {
            view == GROUPS -> SearchAnswer(GROUPS, query, listOf(groupsBlock(player, query)))
            answersNoBlock(view, query) -> SearchAnswer(view, query, emptyList())
            else -> filtered(player, view, query)
        }
    }

    @Suppress("ReturnCount") // one return per answer
    private fun filtered(
        player: Player,
        view: String,
        query: String,
    ): SearchAnswer? {
        var navView: NavigatorView? = navigator.views[view]
        var category: RoomCategory? = rooms.categoryByKey(view)
        val landingCategory = LandingView.categoryOfSearch(view, rooms.categoriesInOrder())
        if (landingCategory != null) {
            if (query.isEmpty()) {
                return SearchAnswer(
                    view,
                    query,
                    listOf(navigator.landing.categorySearch(view, landingCategory, player)),
                )
            }
            category = landingCategory
        }
        val widens = query.isNotEmpty() && LandingView.expandsTextSearch(view)
        if (navView == null && !widens) {
            directSearchRooms(view, player)?.let { found ->
                return SearchAnswer(view, query, listOf(directBlock(player, view, query, found)))
            }
        }
        if (widens) navView = navigator.views[LandingView.VIEW_NAME]
        if (query.isEmpty()) return navView?.let { SearchAnswer(view, query, it.blocks(player)) }

        val typed = if (navView != null) FieldText.of(query) else FieldText(ANY_FIELD, query)
        val searchedView = navView ?: navigator.views[LandingView.VIEW_NAME] ?: return null
        val categoryId = (category ?: rooms.categoryByKey(LandingView.VIEW_NAME))?.id ?: -1
        return textSearch(player, searchedView, typed, TextSearch(view, query, widens), categoryId)
    }

    /** The room field named before a colon ("owner:sam") and the text after it. */
    private data class FieldText(
        val field: String,
        val text: String,
    ) {
        companion object {
            fun of(query: String): FieldText {
                val colon = query.indexOf(':')
                if (colon < 0) return FieldText(ANY_FIELD, query)
                return FieldText(query.take(colon), query.drop(colon + 1))
            }
        }
    }

    private data class TextSearch(
        val view: String,
        val query: String,
        val widens: Boolean,
    )

    /** Null when the database fails it, which is logged. */
    private fun textSearch(
        player: Player,
        navView: NavigatorView,
        typed: FieldText,
        search: TextSearch,
        categoryId: Int,
    ): SearchAnswer? {
        val field = RoomSearchField.named(typed.field)
        return LOGGER.orFallback("navigator search in ${search.view} for '${search.query}'", null) {
            val blocks =
                navView
                    .searchBlocks(player, field, typed.text, categoryId)
                    .map { if (typed.field == GROUP_FIELD) roomsOfGroupsNamed(it, typed.text.lowercase()) else it }
                    .map { it.narrowedTo(field, typed.text) }
            val answered =
                when {
                    search.view == LandingView.VIEW_NAME -> navigator.landing.textSearch(player, matchingRooms(blocks))
                    search.widens -> navigator.landing.expandedTextSearch(player, search.view, matchingRooms(blocks))
                    else -> queryBlock(player, blocks)
                }
            SearchAnswer(search.view, search.query, answered)
        }
    }

    private fun roomsOfGroupsNamed(
        block: NavigatorBlock,
        lowerText: String,
    ) = block.keeping { room -> room.inGuild() && room.guildName.lowercase().contains(lowerText) }

    private fun directBlock(
        player: Player,
        view: String,
        query: String,
        found: List<ListedRoom>,
    ): NavigatorBlock {
        val settings = player.stats.navigatorLayouts
        return NavigatorBlock(
            code = view,
            text = query,
            rooms = found,
            action = directSearchAction(view),
            layout = settings.styleOf(view, BlockStyle.ROWS),
            display = settings.displayOf(view, DisplayMode.VISIBLE),
            narrowsOnText = true,
        )
    }

    private fun directSearchRooms(
        view: String,
        player: Player,
    ): List<ListedRoom>? =
        when (view) {
            "popular" -> rooms.popularPadded()
            "hot" -> rooms.visibleTo(player, navigator.hottest(Int.MAX_VALUE))
            else -> navigator.blockRooms(view, player)
        }

    private fun groupsBlock(
        player: Player,
        query: String,
    ): NavigatorBlock {
        val settings = player.stats.navigatorLayouts
        return groupsResult(
            rooms.groupSearch(query.trim(), RoomListings.GROUP_SEARCH_MAX_ROOMS),
            settings.styleOf(GROUPS, BlockStyle.ROWS),
            settings.displayOf(GROUPS, DisplayMode.VISIBLE),
            seesHiddenRooms(player),
        )
    }

    private fun queryBlock(
        player: Player,
        blocks: List<NavigatorBlock>,
    ): List<NavigatorBlock> {
        val found = HashMap<Int, ListedRoom>()
        blocks.forEach { block -> block.rooms.forEach { found[it.id] = it } }
        return listOf(NavigatorBlock("query", "", found.values.toList(), showsHidden = seesHiddenRooms(player)))
    }

    companion object {
        const val LONGEST_VIEW = 110
        private const val LONGEST_TEXT = 64
        private const val GROUPS = "groups"
        private const val ANY_FIELD = "anything"
        private const val GROUP_FIELD = "group"
        private val LOGGER = LoggerFactory.getLogger(NavigatorSearch::class.java)

        private val FILTER_CODES = setOf("category", "tag", "roomname", "owner")

        private val LANDING_VIEW_FIXED = setOf("hot", "popular", "recommended")

        fun answersNoBlock(
            view: String,
            query: String,
        ): Boolean = (OfficialView.NAME == view && query.isNotEmpty()) || view in FILTER_CODES

        fun directSearchAction(view: String): SearchAction =
            if (view in LANDING_VIEW_FIXED) SearchAction.BACK else SearchAction.NONE

        fun groupsResult(
            found: List<ListedRoom>,
            mode: BlockStyle,
            display: DisplayMode,
            showsHidden: Boolean,
        ) = NavigatorBlock(
            code = GROUPS,
            text = "",
            rooms = found,
            action = SearchAction.BACK,
            layout = mode,
            display = display,
            showsHidden = showsHidden,
            roomOrder = ListedRoom.BUSIEST_THEN_BY_NAME,
        )

        fun matchingRooms(blocks: List<NavigatorBlock>): List<ListedRoom> {
            val found = LinkedHashMap<Int, ListedRoom>()
            blocks.forEach { block -> block.rooms.forEach { found.putIfAbsent(it.id, it) } }
            return ArrayList(found.values)
        }
    }
}
