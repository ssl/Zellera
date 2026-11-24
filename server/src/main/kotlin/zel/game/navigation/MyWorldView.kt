package zel.game.navigation

import zel.game.players.Player

class MyWorldView(
    private val navigator: Navigator,
) : NavigatorView {
    override val name = NAME

    override fun blocks(player: Player): List<NavigatorBlock> {
        val settings = player.stats.navigatorLayouts
        return BLOCKS
            .mapNotNull { (code, always) ->
                navigator
                    .blockRooms(code, player)
                    .orEmpty()
                    .takeIf { always || it.isNotEmpty() }
                    ?.let { code to it }
            }.mapIndexed { rank, (code, rooms) ->
                NavigatorBlock(
                    code = code,
                    text = "",
                    rooms = rooms,
                    layout = settings.styleOf(code),
                    display = settings.displayOf(code),
                    narrowsOnText = true,
                    rank = rank,
                )
            }
    }

    companion object {
        const val NAME = "myworld_view"

        private val BLOCKS =
            listOf(
                "my" to true,
                "favorites" to true,
                "history" to false,
                "history_freq" to false,
                "my_groups" to false,
                "with_friends" to false,
                "with_rights" to false,
            )
    }
}
