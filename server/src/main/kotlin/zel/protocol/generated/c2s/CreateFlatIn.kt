package zel.protocol.generated

data class CreateFlatIn(
    val name: String,
    val description: String,
    val modelName: String,
    val categoryId: Int,
    val maxUsers: Int,
    val tradeType: Int,
)

object CreateFlatInCodec : IncomingCodec<CreateFlatIn> {
    override val name = "CreateFlatIn"

    override fun read(r: PacketReader): CreateFlatIn = readCreateFlatIn(r)

    override fun write(w: PacketWriter, m: CreateFlatIn) = writeCreateFlatIn(w, m)

    private fun readCreateFlatIn(r: PacketReader): CreateFlatIn {
        val vName = r.string()
        val vDescription = r.string()
        val vModelName = r.string()
        val vCategoryId = r.int()
        val vMaxUsers = r.int()
        val vTradeType = r.int()
        return CreateFlatIn(vName, vDescription, vModelName, vCategoryId, vMaxUsers, vTradeType)
    }

    private fun writeCreateFlatIn(w: PacketWriter, m: CreateFlatIn) {
        w.string(m.name)
        w.string(m.description)
        w.string(m.modelName)
        w.int(m.categoryId)
        w.int(m.maxUsers)
        w.int(m.tradeType)
    }
}
