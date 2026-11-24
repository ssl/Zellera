package zel.game.navigation

import zel.game.players.Player

class SingleBlockView private constructor(
    override val name: String,
    private val navigator: Navigator,
    private val block: String,
    private val displayKey: String,
    private val narrowsOnText: Boolean,
    private val showsHidden: (Player) -> Boolean,
) : NavigatorView {
    override fun blocks(player: Player): List<NavigatorBlock> {
        val settings = player.stats.navigatorLayouts
        return listOf(
            NavigatorBlock(
                code = block,
                text = "",
                rooms = navigator.blockRooms(block, player).orEmpty(),
                layout = settings.styleOf(block, BlockStyle.ROWS),
                display = settings.displayOf(displayKey, DisplayMode.VISIBLE),
                narrowsOnText = narrowsOnText,
                showsHidden = showsHidden(player),
            ),
        )
    }

    companion object {
        const val FAVOURITES = "favorites"
        const val ROOM_EVENTS = "roomads_view"

        fun favourites(navigator: Navigator) =
            SingleBlockView(FAVOURITES, navigator, FAVOURITES, "popular", narrowsOnText = true) { true }

        fun roomEvents(navigator: Navigator) =
            SingleBlockView(
                ROOM_EVENTS,
                navigator,
                "categories",
                "official-root",
                narrowsOnText = false,
                ::seesHiddenRooms,
            )
    }
}
