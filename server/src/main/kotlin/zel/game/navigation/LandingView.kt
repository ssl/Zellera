package zel.game.navigation

import zel.core.Configuration
import zel.game.players.Player
import zel.room.listing.ListedRoom
import zel.room.listing.RoomCategory
import zel.room.listing.RoomListings

class LandingView(
    private val navigator: Navigator,
    private val rooms: RoomListings,
    private val config: Configuration,
) : NavigatorView {
    override val name = VIEW_NAME

    data class CategoryBlock(
        val code: String,
        val text: String,
        val action: SearchAction,
        val rooms: List<ListedRoom>,
    )

    override fun blocks(player: Player): List<NavigatorBlock> {
        val settings = player.stats.navigatorLayouts
        val popularMode = BlockStyle.fromCode(config.getInt("navigator.popular.block_style"))
        val fixed =
            FIXED_BLOCKS.mapNotNull { code ->
                navigator.blockRooms(code, player)?.takeIf { it.isNotEmpty() }?.let { found ->
                    CategoryBlock(code, "", SearchAction.MORE, found)
                }
            }
        val perBlock = config.getInt("navigator.popular.per_category")
        val byCategory = categoryBlocks(rooms.categoriesInOrder(), rooms.popularByCategory(perBlock + 1), perBlock)
        return fixed.mapIndexed { rank, block ->
            playerBlock(
                player,
                block,
                rank,
                narrowsOnText = false,
                layout = settings.styleOf(block.code, popularMode),
            )
        } + byCategory.mapIndexed { index, block -> playerBlock(player, block, fixed.size + index) }
    }

    override fun searchBlocks(
        player: Player,
        field: RoomSearchField,
        value: String,
        categoryId: Int,
    ): List<NavigatorBlock> {
        val showsHidden = seesHiddenRooms(player)
        val settings = player.stats.navigatorLayouts
        val maxResults = config.getInt("navigator.search.max_results", DEFAULT_SEARCH_RESULTS)
        return rooms
            .find(field, value, categoryId, showsHidden, maxResults)
            .entries
            .filter { it.value.isNotEmpty() }
            .mapNotNull { (id, found) -> rooms.category(id)?.let { it to found } }
            .map { (category, found) ->
                NavigatorBlock(
                    code = category.captionKey,
                    text = category.caption,
                    rooms = found,
                    action = SearchAction.MORE,
                    layout = settings.styleOf(category.captionKey),
                    display = settings.displayOf(category.captionKey),
                    narrowsOnText = true,
                    showsHidden = showsHidden,
                    rank = category.order,
                )
            }
    }

    fun textSearch(
        player: Player,
        matches: List<ListedRoom>,
    ): List<NavigatorBlock> =
        textSearchBlocks(
            matches,
            loadedIds(matches),
            navigator.blockRooms("recommended", player),
            rooms.categoriesInOrder(),
            config.getInt("navigator.popular.max_rooms"),
            config.getInt("navigator.popular.per_category"),
        ).mapIndexed { rank, block -> playerBlock(player, block, rank) }

    fun expandedTextSearch(
        player: Player,
        code: String,
        matches: List<ListedRoom>,
    ): List<NavigatorBlock> =
        listOfNotNull(
            expandedTextSearchBlock(
                code,
                matches,
                loadedIds(matches),
                navigator.blockRooms("recommended", player),
                rooms.categoriesInOrder(),
            )?.let { playerBlock(player, it, 0) },
        )

    fun categorySearch(
        view: String,
        category: RoomCategory,
        player: Player,
    ): NavigatorBlock {
        val settings = player.stats.navigatorLayouts
        return categorySearch(
            view,
            category,
            rooms.activeInCategory(category.id).filterNot { it.isPublicRoom },
            settings.styleOf(view, BlockStyle.ROWS),
            settings.displayOf(view, DisplayMode.VISIBLE),
            seesHiddenRooms(player),
        )
    }

    private fun playerBlock(
        player: Player,
        block: CategoryBlock,
        rank: Int,
        narrowsOnText: Boolean = true,
        layout: BlockStyle? = null,
    ): NavigatorBlock {
        val settings = player.stats.navigatorLayouts
        return NavigatorBlock(
            code = block.code,
            text = block.text,
            rooms = block.rooms,
            action = block.action,
            layout = layout ?: settings.styleOf(block.code),
            display = settings.displayOf(block.code),
            narrowsOnText = narrowsOnText,
            showsHidden = seesHiddenRooms(player),
            rank = rank,
        )
    }

    companion object {
        const val VIEW_NAME = "landing_view"

        const val CATEGORY_PREFIX = "category__"

        const val RECOMMENDED_BLOCK_SIZE = 7

        private const val DEFAULT_SEARCH_RESULTS = 10

        val FIXED_BLOCKS: List<String> = listOf("hot", "popular", "recommended")

        fun categoryBlocks(
            categories: List<RoomCategory>,
            byCategory: Map<Int, List<ListedRoom>>,
            perBlock: Int,
        ): List<CategoryBlock> =
            categories.filter { it.isVisible }.mapNotNull { category ->
                byCategory[category.id]?.takeIf { it.isNotEmpty() }?.let {
                    limited(CATEGORY_PREFIX + category.name, category.caption, it, perBlock)
                }
            }

        fun textSearchBlocks(
            matches: List<ListedRoom>,
            listed: Set<Int>,
            recommended: List<ListedRoom>?,
            categories: List<RoomCategory>,
            queryMax: Int,
            perBlock: Int,
        ): List<CategoryBlock> =
            textSearchBlocks(matches, listed, recommended, categories, queryMax, RECOMMENDED_BLOCK_SIZE, perBlock)

        @Suppress("LongParameterList") // the three block limits differ between the search and its "show more"
        private fun textSearchBlocks(
            matches: List<ListedRoom>,
            listed: Set<Int>,
            recommended: List<ListedRoom>?,
            categories: List<RoomCategory>,
            queryMax: Int,
            recommendedMax: Int,
            perBlock: Int,
        ): List<CategoryBlock> {
            if (matches.isEmpty()) return emptyList()
            val sorted = matches.sorted()
            val kept = sorted.map { it.id }.filterTo(HashSet()) { it in listed }
            val picks = recommended.orEmpty().filter { it.id in kept }.sorted()
            return buildList {
                add(limited("query", "", sorted, queryMax))
                if (picks.isNotEmpty()) add(limited("recommended", "", picks, recommendedMax))
                categories.filter { it.isVisible }.forEach { category ->
                    val inCategory = sorted.filter { it.category == category.id && it.id in kept }
                    if (inCategory.isNotEmpty()) {
                        add(limited(CATEGORY_PREFIX + category.name, category.caption, inCategory, perBlock))
                    }
                }
            }
        }

        fun expandsTextSearch(code: String): Boolean =
            code == "query" || code == "recommended" || code.startsWith(CATEGORY_PREFIX)

        fun expandedTextSearchBlock(
            code: String,
            matches: List<ListedRoom>,
            listed: Set<Int>,
            recommended: List<ListedRoom>?,
            categories: List<RoomCategory>,
        ): CategoryBlock? =
            textSearchBlocks(matches, listed, recommended, categories, Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
                .firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?.let { block ->
                    val title = categoryOfSearch(code, categories)?.name ?: block.text
                    CategoryBlock(code, title, SearchAction.BACK, block.rooms)
                }

        @Suppress("LongParameterList") // the block's fields, chosen by the caller
        fun categorySearch(
            view: String,
            category: RoomCategory,
            rooms: List<ListedRoom>,
            mode: BlockStyle,
            display: DisplayMode,
            showsHidden: Boolean,
        ): NavigatorBlock =
            NavigatorBlock(
                code = view,
                text = category.name,
                rooms = rooms,
                action = SearchAction.BACK,
                layout = mode,
                display = display,
                showsHidden = showsHidden,
            )

        fun categoryOfSearch(
            view: String?,
            categories: List<RoomCategory>,
        ): RoomCategory? {
            if (view == null || !view.startsWith(CATEGORY_PREFIX)) return null
            val name = view.removePrefix(CATEGORY_PREFIX)
            return categories.firstOrNull { it.name.equals(name, ignoreCase = true) }
        }

        private fun limited(
            code: String,
            text: String,
            rooms: List<ListedRoom>,
            max: Int,
        ): CategoryBlock {
            val more = rooms.size > max
            return CategoryBlock(
                code,
                text,
                if (more) SearchAction.MORE else SearchAction.NONE,
                if (more) rooms.take(max) else rooms,
            )
        }

        private fun loadedIds(matches: List<ListedRoom>): Set<Int> =
            matches.filter { it.isLoaded }.mapTo(HashSet()) { it.id }
    }
}
