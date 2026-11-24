package zel.protocol.generated

data class Game2QuickJoinGameIn(
    val selection: Selection?,
) {
    data class Selection(
        val teams: Int,
        val modeChoice: Int?,
    )
}

object Game2QuickJoinGameInCodec : IncomingCodec<Game2QuickJoinGameIn> {
    override val name = "Game2QuickJoinGameIn"

    override fun read(r: PacketReader): Game2QuickJoinGameIn = readGame2QuickJoinGameIn(r)

    override fun write(w: PacketWriter, m: Game2QuickJoinGameIn) = writeGame2QuickJoinGameIn(w, m)

    private fun readGame2QuickJoinGameInSelection(r: PacketReader): Game2QuickJoinGameIn.Selection {
        val vTeams = r.int()
        val vModeChoice = if (r.remaining > 0) r.int() else null
        return Game2QuickJoinGameIn.Selection(vTeams, vModeChoice)
    }

    private fun readGame2QuickJoinGameIn(r: PacketReader): Game2QuickJoinGameIn {
        val vSelection = if (r.remaining > 0) readGame2QuickJoinGameInSelection(r) else null
        return Game2QuickJoinGameIn(vSelection)
    }

    private fun writeGame2QuickJoinGameInSelection(w: PacketWriter, m: Game2QuickJoinGameIn.Selection) {
        w.int(m.teams)
        m.modeChoice?.let { w.int(it) }
    }

    private fun writeGame2QuickJoinGameIn(w: PacketWriter, m: Game2QuickJoinGameIn) {
        m.selection?.let { writeGame2QuickJoinGameInSelection(w, it) }
    }
}
