package zel.protocol.generated

data class UpdateConditionIn(
    val id: Int,
    val intParams: List<Int>,
    val stringParam: String,
    val stuffIds: List<Int>,
    val quantifier: Int,
    val furniSources: List<Int>,
    val userSources: List<Int>,
    val variableIds: List<String>,
    val stuffIds2: List<Int>,
)

object UpdateConditionInCodec : IncomingCodec<UpdateConditionIn> {
    override val name = "UpdateConditionIn"

    override fun read(r: PacketReader): UpdateConditionIn = readUpdateConditionIn(r)

    override fun write(w: PacketWriter, m: UpdateConditionIn) = writeUpdateConditionIn(w, m)

    private fun readUpdateConditionIn(r: PacketReader): UpdateConditionIn {
        val vId = r.int()
        val vIntParams = List(r.countInt(4)) { r.int() }
        val vStringParam = r.string()
        val vStuffIds = List(r.countInt(4)) { r.int() }
        val vQuantifier = r.int()
        val vFurniSources = List(r.countInt(4)) { r.int() }
        val vUserSources = List(r.countInt(4)) { r.int() }
        val vVariableIds = List(r.countInt(2)) { r.string() }
        val vStuffIds2 = List(r.countInt(4)) { r.int() }
        return UpdateConditionIn(vId, vIntParams, vStringParam, vStuffIds, vQuantifier, vFurniSources, vUserSources, vVariableIds, vStuffIds2)
    }

    private fun writeUpdateConditionIn(w: PacketWriter, m: UpdateConditionIn) {
        w.int(m.id)
        w.countInt(m.intParams.size)
        m.intParams.forEach { w.int(it) }
        w.string(m.stringParam)
        w.countInt(m.stuffIds.size)
        m.stuffIds.forEach { w.int(it) }
        w.int(m.quantifier)
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
