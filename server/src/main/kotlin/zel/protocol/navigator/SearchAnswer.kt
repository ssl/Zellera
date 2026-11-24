package zel.protocol.navigator

import zel.game.navigation.NavigatorBlock

data class SearchAnswer(
    val code: String,
    val text: String,
    val blocks: List<NavigatorBlock>,
)
