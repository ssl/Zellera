package zel.protocol.generated

data class UpdateVariableIn(
    val id: Int,
    val intParams: List<Int>,
    val stringParam: String,
    val stuffIds: List<Int>,
    val furniSources: List<Int>,
    val userSources: List<Int>,
    val variableIds: List<String>,
    val stuffIds2: List<Int>,
)

object UpdateVariableInCodec : IncomingCodec<UpdateVariableIn> {
    override val name = "UpdateVariableIn"

    override fun read(r: PacketReader): UpdateVariableIn = readUpdateVariableIn(r)

    override fun write(w: PacketWriter, m: UpdateVariableIn) = writeUpdateVariableIn(w, m)

    private fun readUpdateVariableIn(r: PacketReader): UpdateVariableIn {
        val vId = r.int()
        val vIntParams = List(r.countInt(4)) { r.int() }
        val vStringParam = r.string()
        val vStuffIds = List(r.countInt(4)) { r.int() }
        val vFurniSources = List(r.countInt(4)) { r.int() }
        val vUserSources = List(r.countInt(4)) { r.int() }
        val vVariableIds = List(r.countInt(2)) { r.string() }
        val vStuffIds2 = List(r.countInt(4)) { r.int() }
        return UpdateVariableIn(vId, vIntParams, vStringParam, vStuffIds, vFurniSources, vUserSources, vVariableIds, vStuffIds2)
    }

    private fun writeUpdateVariableIn(w: PacketWriter, m: UpdateVariableIn) {
        w.int(m.id)
        w.countInt(m.intParams.size)
        m.intParams.forEach { w.int(it) }
        w.string(m.stringParam)
        w.countInt(m.stuffIds.size)
        m.stuffIds.forEach { w.int(it) }
        w.countInt(m.furniSources.size)
        m.furniSources.forEach { w.int(it) }
        w.countInt(m.userSources.size)
        m.userSources.forEach { w.int(it) }
        w.countInt(m.variableIds.size)
        m.variableIds.forEach { w.string(it) }
        w.countInt(m.stuffIds2.size)
        m.stuffIds2.forEach { w.int(it) }
    }
}
