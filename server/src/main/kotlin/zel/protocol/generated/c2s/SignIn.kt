package zel.protocol.generated

data class SignIn(
    val signId: Int,
)

object SignInCodec : IncomingCodec<SignIn> {
    override val name = "SignIn"

    override fun read(r: PacketReader): SignIn = readSignIn(r)

    override fun write(w: PacketWriter, m: SignIn) = writeSignIn(w, m)

    private fun readSignIn(r: PacketReader): SignIn {
        val vSignId = r.int()
        return SignIn(vSignId)
    }

    private fun writeSignIn(w: PacketWriter, m: SignIn) {
        w.int(m.signId)
    }
}
