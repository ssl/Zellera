package zel.protocol.generated

data class AccountSafetyLockStatusChangeOut(
    val int1: Int,
)

object AccountSafetyLockStatusChangeOutCodec : OutgoingCodec<AccountSafetyLockStatusChangeOut> {
    override val name = "AccountSafetyLockStatusChangeOut"

    override fun read(r: PacketReader): AccountSafetyLockStatusChangeOut = readAccountSafetyLockStatusChangeOut(r)

    override fun write(w: PacketWriter, m: AccountSafetyLockStatusChangeOut) = writeAccountSafetyLockStatusChangeOut(w, m)

    private fun readAccountSafetyLockStatusChangeOut(r: PacketReader): AccountSafetyLockStatusChangeOut {
        val vInt1 = r.int()
        return AccountSafetyLockStatusChangeOut(vInt1)
    }

    private fun writeAccountSafetyLockStatusChangeOut(w: PacketWriter, m: AccountSafetyLockStatusChangeOut) {
        w.int(m.int1)
    }
}
