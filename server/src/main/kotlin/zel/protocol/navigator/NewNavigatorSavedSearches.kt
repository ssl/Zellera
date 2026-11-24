package zel.protocol.navigator

import zel.game.navigation.NavigatorSavedSearch
import zel.protocol.generated.NavigatorSavedSearchesOut
import zel.protocol.generated.NavigatorSavedSearchesOutCodec
import zel.protocol.outgoing.OutgoingMessage

class NewNavigatorSavedSearches(
    private val searches: List<NavigatorSavedSearch>,
) : OutgoingMessage<NavigatorSavedSearchesOut>(NavigatorSavedSearchesOutCodec) {
    override fun message() = NavigatorMessages.savedSearches(searches)
}
