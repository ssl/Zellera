package zel.protocol.generated

data class SetNewNavigatorWindowPreferencesIn(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val openSearches: Boolean,
    val resultsMode: Int,
)

object SetNewNavigatorWindowPreferencesInCodec : IncomingCodec<SetNewNavigatorWindowPreferencesIn> {
    override val name = "SetNewNavigatorWindowPreferencesIn"

    override fun read(r: PacketReader): SetNewNavigatorWindowPreferencesIn = readSetNewNavigatorWindowPreferencesIn(r)

    override fun write(w: PacketWriter, m: SetNewNavigatorWindowPreferencesIn) = writeSetNewNavigatorWindowPreferencesIn(w, m)

    private fun readSetNewNavigatorWindowPreferencesIn(r: PacketReader): SetNewNavigatorWindowPreferencesIn {
        val vX = r.int()
        val vY = r.int()
        val vWidth = r.int()
        val vHeight = r.int()
        val vOpenSearches = r.boolean()
        val vResultsMode = r.int()
        return SetNewNavigatorWindowPreferencesIn(vX, vY, vWidth, vHeight, vOpenSearches, vResultsMode)
    }

    private fun writeSetNewNavigatorWindowPreferencesIn(w: PacketWriter, m: SetNewNavigatorWindowPreferencesIn) {
        w.int(m.x)
        w.int(m.y)
        w.int(m.width)
        w.int(m.height)
        w.boolean(m.openSearches)
        w.int(m.resultsMode)
    }
}
