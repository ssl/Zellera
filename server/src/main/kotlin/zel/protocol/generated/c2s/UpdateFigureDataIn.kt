package zel.protocol.generated

data class UpdateFigureDataIn(
    val genderCode: String,
    val look: String,
)

object UpdateFigureDataInCodec : IncomingCodec<UpdateFigureDataIn> {
    override val name = "UpdateFigureDataIn"

    override fun read(r: PacketReader): UpdateFigureDataIn = readUpdateFigureDataIn(r)

    override fun write(w: PacketWriter, m: UpdateFigureDataIn) = writeUpdateFigureDataIn(w, m)

    private fun readUpdateFigureDataIn(r: PacketReader): UpdateFigureDataIn {
        val vGenderCode = r.string()
        val vLook = r.string()
        return UpdateFigureDataIn(vGenderCode, vLook)
    }

    private fun writeUpdateFigureDataIn(w: PacketWriter, m: UpdateFigureDataIn) {
        w.string(m.genderCode)
        w.string(m.look)
    }
}
