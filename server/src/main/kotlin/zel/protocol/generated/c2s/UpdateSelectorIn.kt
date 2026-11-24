package zel.protocol.generated

data class UpdateSelectorIn(
    val id: Int,
    val intParams: List<Int>,
    val stringParam: String,
    val stuffIds: List<Int>,
    val filter: Boolean,
    val invert: Boolean,
    val furniSources: List<Int>,
    val userSources: List<Int>,
    val variableIds: List<String>,
    val stuffIds2: List<Int>,
)

object UpdateSelectorInCodec : IncomingCodec<UpdateSelectorIn> {
    override val name = "UpdateSelectorIn"

    override fun read(r: PacketReader): UpdateSelectorIn = readUpdateSelectorIn(r)

    override fun write(w: PacketWriter, m: UpdateSelectorIn) = writeUpdateSelectorIn(w, m)

    private fun readUpdateSelectorIn(r: PacketReader): UpdateSelectorIn {
        val vId = r.int()
        val vIntParams = List(r.countInt(4)) { r.int() }
        val vStringParam = r.string()
        val vStuffIds = List(r.countInt(4)) { r.int() }
        val vFilter = r.boolean()
        val vInvert = r.boolean()
        val vFurniSources = List(r.countInt(4)) { r.int() }
        val vUserSources = List(r.countInt(4)) { r.int() }
        val vVariableIds = List(r.countInt(2)) { r.string() }
        val vStuffIds2 = List(r.countInt(4)) { r.int() }
        return UpdateSelectorIn(vId, vIntParams, vStringParam, vStuffIds, vFilter, vInvert, vFurniSources, vUserSources, vVariableIds, vStuffIds2)
    }

    private fun writeUpdateSelectorIn(w: PacketWriter, m: UpdateSelectorIn) {
        w.int(m.id)
        w.countInt(m.intParams.size)
        m.intParams.forEach { w.int(it) }
        w.string(m.stringParam)
        w.countInt(m.stuffIds.size)
        m.stuffIds.forEach { w.int(it) }
        w.boolean(m.filter)
        w.boolean(m.invert)
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
