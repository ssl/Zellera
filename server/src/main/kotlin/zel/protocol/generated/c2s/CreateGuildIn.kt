package zel.protocol.generated

data class CreateGuildIn(
    val name: String,
    val description: String,
    val roomId: Int,
    val colorOne: Int,
    val colorTwo: Int,
    val parts: List<Int>,
)

object CreateGuildInCodec : IncomingCodec<CreateGuildIn> {
    override val name = "CreateGuildIn"

    override fun read(r: PacketReader): CreateGuildIn = readCreateGuildIn(r)

    override fun write(w: PacketWriter, m: CreateGuildIn) = writeCreateGuildIn(w, m)

    private fun readCreateGuildIn(r: PacketReader): CreateGuildIn {
        val vName = r.string()
        val vDescription = r.string()
        val vRoomId = r.int()
        val vColorOne = r.int()
        val vColorTwo = r.int()
        val vParts = List(r.countInt(4)) { r.int() }
        return CreateGuildIn(vName, vDescription, vRoomId, vColorOne, vColorTwo, vParts)
    }

    private fun writeCreateGuildIn(w: PacketWriter, m: CreateGuildIn) {
        w.string(m.name)
        w.string(m.description)
        w.int(m.roomId)
        w.int(m.colorOne)
        w.int(m.colorTwo)
        w.countInt(m.parts.size)
        m.parts.forEach { w.int(it) }
    }
}
