package zel.protocol.generated

data class WiredGetAllVariablesDiffsIn(
    val hashes: List<HashesItem>,
) {
    data class HashesItem(
        val variableId: String,
        val hash: Int,
    )
}

object WiredGetAllVariablesDiffsInCodec : IncomingCodec<WiredGetAllVariablesDiffsIn> {
    override val name = "WiredGetAllVariablesDiffsIn"

    override fun read(r: PacketReader): WiredGetAllVariablesDiffsIn = readWiredGetAllVariablesDiffsIn(r)

    override fun write(w: PacketWriter, m: WiredGetAllVariablesDiffsIn) = writeWiredGetAllVariablesDiffsIn(w, m)

    private fun readWiredGetAllVariablesDiffsInHashesItem(r: PacketReader): WiredGetAllVariablesDiffsIn.HashesItem {
        val vVariableId = r.string()
        val vHash = r.int()
        return WiredGetAllVariablesDiffsIn.HashesItem(vVariableId, vHash)
    }

    private fun readWiredGetAllVariablesDiffsIn(r: PacketReader): WiredGetAllVariablesDiffsIn {
        val vHashes = List(r.countInt(6)) { readWiredGetAllVariablesDiffsInHashesItem(r) }
        return WiredGetAllVariablesDiffsIn(vHashes)
    }

    private fun writeWiredGetAllVariablesDiffsInHashesItem(w: PacketWriter, m: WiredGetAllVariablesDiffsIn.HashesItem) {
        w.string(m.variableId)
        w.int(m.hash)
    }

    private fun writeWiredGetAllVariablesDiffsIn(w: PacketWriter, m: WiredGetAllVariablesDiffsIn) {
        w.countInt(m.hashes.size)
        m.hashes.forEach { writeWiredGetAllVariablesDiffsInHashesItem(w, it) }
    }
}
