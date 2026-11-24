package zel.protocol.generated

data class LockAllChestsIn(
    val lock: Boolean,
    val all: Boolean,
)

object LockAllChestsInCodec : IncomingCodec<LockAllChestsIn> {
    override val name = "LockAllChestsIn"

    override fun read(r: PacketReader): LockAllChestsIn = readLockAllChestsIn(r)

    override fun write(w: PacketWriter, m: LockAllChestsIn) = writeLockAllChestsIn(w, m)

    private fun readLockAllChestsIn(r: PacketReader): LockAllChestsIn {
        val vLock = r.boolean()
        val vAll = r.boolean()
        return LockAllChestsIn(vLock, vAll)
    }

    private fun writeLockAllChestsIn(w: PacketWriter, m: LockAllChestsIn) {
        w.boolean(m.lock)
        w.boolean(m.all)
    }
}
