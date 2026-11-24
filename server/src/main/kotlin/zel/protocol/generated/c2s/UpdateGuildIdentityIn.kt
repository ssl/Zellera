package zel.protocol.generated

data class UpdateGuildIdentityIn(
    val int1: Int,
    val string1: String,
    val string2: String,
)

object UpdateGuildIdentityInCodec : IncomingCodec<UpdateGuildIdentityIn> {
    override val name = "UpdateGuildIdentityIn"

    override fun read(r: PacketReader): UpdateGuildIdentityIn = readUpdateGuildIdentityIn(r)

    override fun write(w: PacketWriter, m: UpdateGuildIdentityIn) = writeUpdateGuildIdentityIn(w, m)

    private fun readUpdateGuildIdentityIn(r: PacketReader): UpdateGuildIdentityIn {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        return UpdateGuildIdentityIn(vInt1, vString1, vString2)
    }

    private fun writeUpdateGuildIdentityIn(w: PacketWriter, m: UpdateGuildIdentityIn) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
    }
}
