package zel.protocol.generated

data object MyGuildBasesSearchIn

object MyGuildBasesSearchInCodec : IncomingCodec<MyGuildBasesSearchIn> {
    override val name = "MyGuildBasesSearchIn"

    override fun read(r: PacketReader): MyGuildBasesSearchIn = MyGuildBasesSearchIn

    override fun write(w: PacketWriter, m: MyGuildBasesSearchIn) = writeMyGuildBasesSearchIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeMyGuildBasesSearchIn(w: PacketWriter, m: MyGuildBasesSearchIn) {
    }
}
