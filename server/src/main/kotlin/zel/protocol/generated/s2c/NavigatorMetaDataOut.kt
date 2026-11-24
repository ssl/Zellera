package zel.protocol.generated

data class NavigatorMetaDataOut(
    val topLevelContexts: List<TopLevelContextsItem>,
) {
    data class TopLevelContextsItem(
        val searchCode: String,
        val savedSearches: List<SavedSearchesItem>,
    ) {
        data class SavedSearchesItem(
            val searchId: Int,
            val searchCode: String,
            val filter: String,
            val localization: String,
        )
    }
}

object NavigatorMetaDataOutCodec : OutgoingCodec<NavigatorMetaDataOut> {
    override val name = "NavigatorMetaDataOut"

    override fun read(r: PacketReader): NavigatorMetaDataOut = readNavigatorMetaDataOut(r)

    override fun write(w: PacketWriter, m: NavigatorMetaDataOut) = writeNavigatorMetaDataOut(w, m)

    private fun readNavigatorMetaDataOutTopLevelContextsItemSavedSearchesItem(r: PacketReader): NavigatorMetaDataOut.TopLevelContextsItem.SavedSearchesItem {
        val vSearchId = r.int()
        val vSearchCode = r.string()
        val vFilter = r.string()
        val vLocalization = r.string()
        return NavigatorMetaDataOut.TopLevelContextsItem.SavedSearchesItem(vSearchId, vSearchCode, vFilter, vLocalization)
    }

    private fun readNavigatorMetaDataOutTopLevelContextsItem(r: PacketReader): NavigatorMetaDataOut.TopLevelContextsItem {
        val vSearchCode = r.string()
        val vSavedSearches = List(r.countInt(10)) { readNavigatorMetaDataOutTopLevelContextsItemSavedSearchesItem(r) }
        return NavigatorMetaDataOut.TopLevelContextsItem(vSearchCode, vSavedSearches)
    }

    private fun readNavigatorMetaDataOut(r: PacketReader): NavigatorMetaDataOut {
        val vTopLevelContexts = List(r.countInt(6)) { readNavigatorMetaDataOutTopLevelContextsItem(r) }
        return NavigatorMetaDataOut(vTopLevelContexts)
    }

    private fun writeNavigatorMetaDataOutTopLevelContextsItemSavedSearchesItem(w: PacketWriter, m: NavigatorMetaDataOut.TopLevelContextsItem.SavedSearchesItem) {
        w.int(m.searchId)
        w.string(m.searchCode)
        w.string(m.filter)
        w.string(m.localization)
    }

    private fun writeNavigatorMetaDataOutTopLevelContextsItem(w: PacketWriter, m: NavigatorMetaDataOut.TopLevelContextsItem) {
        w.string(m.searchCode)
        w.countInt(m.savedSearches.size)
        m.savedSearches.forEach { writeNavigatorMetaDataOutTopLevelContextsItemSavedSearchesItem(w, it) }
    }

    private fun writeNavigatorMetaDataOut(w: PacketWriter, m: NavigatorMetaDataOut) {
        w.countInt(m.topLevelContexts.size)
        m.topLevelContexts.forEach { writeNavigatorMetaDataOutTopLevelContextsItem(w, it) }
    }
}
