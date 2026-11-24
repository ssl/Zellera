package zel.game.navigation

import zel.game.players.Player

class OfficialView(
    private val navigator: Navigator,
) : NavigatorView {
    override val name = NAME

    override fun blocks(player: Player): List<NavigatorBlock> {
        val settings = player.stats.navigatorLayouts
        val showsHidden = seesHiddenRooms(player)
        val legacy =
            navigator.blockRooms(LEGACY_BLOCK, player).orEmpty().takeIf { it.isNotEmpty() }?.let { rooms ->
                NavigatorBlock(
                    code = LEGACY_BLOCK,
                    text = "",
                    rooms = rooms,
                    layout = settings.styleOf(LEGACY_BLOCK, BlockStyle.TILES),
                    display = settings.displayOf(LEGACY_BLOCK),
                    showsHidden = showsHidden,
                    rank = -1,
                )
            }
        val categories =
            navigator.publicFolders.values.mapNotNull { category ->
                val rooms = navigator.roomsOf(category).takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val name = category.name
                NavigatorBlock(
                    code = "official_${category.id}",
                    text = name,
                    rooms = rooms,
                    layout = settings.styleOf(name, category.listMode),
                    display = settings.displayOf(name),
                    narrowsOnText = true,
                    showsHidden = showsHidden,
                    rank = category.order,
                )
            }
        return (listOfNotNull(legacy) + categories).sortedBy { it.rank }
    }

    companion object {
        const val NAME = "official_view"
        private const val LEGACY_BLOCK = "official-root"
    }
}
