package zel.protocol.generated

data class LoginFailedGameClosedOut(
    val int1: Int,
    val int2: Int,
)

object LoginFailedGameClosedOutCodec : OutgoingCodec<LoginFailedGameClosedOut> {
    override val name = "LoginFailedGameClosedOut"

    override fun read(r: PacketReader): LoginFailedGameClosedOut = readLoginFailedGameClosedOut(r)

    override fun write(w: PacketWriter, m: LoginFailedGameClosedOut) = writeLoginFailedGameClosedOut(w, m)

    private fun readLoginFailedGameClosedOut(r: PacketReader): LoginFailedGameClosedOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return LoginFailedGameClosedOut(vInt1, vInt2)
    }

    private fun writeLoginFailedGameClosedOut(w: PacketWriter, m: LoginFailedGameClosedOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
