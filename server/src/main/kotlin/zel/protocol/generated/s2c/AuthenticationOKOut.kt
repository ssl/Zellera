package zel.protocol.generated

data class AuthenticationOKOut(
    val int1: Int,
    val list1: List<Short>,
    val int2: Int,
)

object AuthenticationOKOutCodec : OutgoingCodec<AuthenticationOKOut> {
    override val name = "AuthenticationOKOut"

    override fun read(r: PacketReader): AuthenticationOKOut = readAuthenticationOKOut(r)

    override fun write(w: PacketWriter, m: AuthenticationOKOut) = writeAuthenticationOKOut(w, m)

    private fun readAuthenticationOKOut(r: PacketReader): AuthenticationOKOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(2)) { r.short() }
        val vInt2 = r.int()
        return AuthenticationOKOut(vInt1, vList1, vInt2)
    }

    private fun writeAuthenticationOKOut(w: PacketWriter, m: AuthenticationOKOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { w.short(it) }
        w.int(m.int2)
    }
}
