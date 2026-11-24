package zel.protocol.generated

data class NavigatorSavedSearchesOut(
    val searches: List<SearchesItem>,
) {
    data class SearchesItem(
        val searchId: Int,
        val searchCode: String,
        val filter: String,
        val localization: String,
    )
}

object NavigatorSavedSearchesOutCodec : OutgoingCodec<NavigatorSavedSearchesOut> {
    override val name = "NavigatorSavedSearchesOut"

    override fun read(r: PacketReader): NavigatorSavedSearchesOut = readNavigatorSavedSearchesOut(r)

    override fun write(w: PacketWriter, m: NavigatorSavedSearchesOut) = writeNavigatorSavedSearchesOut(w, m)

    private fun readNavigatorSavedSearchesOutSearchesItem(r: PacketReader): NavigatorSavedSearchesOut.SearchesItem {
        val vSearchId = r.int()
        val vSearchCode = r.string()
        val vFilter = r.string()
        val vLocalization = r.string()
        return NavigatorSavedSearchesOut.SearchesItem(vSearchId, vSearchCode, vFilter, vLocalization)
    }

    private fun readNavigatorSavedSearchesOut(r: PacketReader): NavigatorSavedSearchesOut {
        val vSearches = List(r.countInt(10)) { readNavigatorSavedSearchesOutSearchesItem(r) }
        return NavigatorSavedSearchesOut(vSearches)
    }

    private fun writeNavigatorSavedSearchesOutSearchesItem(w: PacketWriter, m: NavigatorSavedSearchesOut.SearchesItem) {
        w.int(m.searchId)
        w.string(m.searchCode)
        w.string(m.filter)
        w.string(m.localization)
    }

    private fun writeNavigatorSavedSearchesOut(w: PacketWriter, m: NavigatorSavedSearchesOut) {
        w.countInt(m.searches.size)
        m.searches.forEach { writeNavigatorSavedSearchesOutSearchesItem(w, it) }
    }
}
