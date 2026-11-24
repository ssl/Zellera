package zel.protocol.generated

data class NewPlayerExperienceGetGiftsIn(
    val gifts: List<GiftsItem>,
) {
    data class GiftsItem(
        val key1: Int,
        val key2: Int,
        val index: Int,
    )
}

object NewPlayerExperienceGetGiftsInCodec : IncomingCodec<NewPlayerExperienceGetGiftsIn> {
    override val name = "NewPlayerExperienceGetGiftsIn"

    override fun read(r: PacketReader): NewPlayerExperienceGetGiftsIn = readNewPlayerExperienceGetGiftsIn(r)

    override fun write(w: PacketWriter, m: NewPlayerExperienceGetGiftsIn) = writeNewPlayerExperienceGetGiftsIn(w, m)

    private fun readNewPlayerExperienceGetGiftsInGiftsItem(r: PacketReader): NewPlayerExperienceGetGiftsIn.GiftsItem {
        val vKey1 = r.int()
        val vKey2 = r.int()
        val vIndex = r.int()
        return NewPlayerExperienceGetGiftsIn.GiftsItem(vKey1, vKey2, vIndex)
    }

    private fun readNewPlayerExperienceGetGiftsIn(r: PacketReader): NewPlayerExperienceGetGiftsIn {
        val vGifts = List(r.countInt(12)) { readNewPlayerExperienceGetGiftsInGiftsItem(r) }
        return NewPlayerExperienceGetGiftsIn(vGifts)
    }

    private fun writeNewPlayerExperienceGetGiftsInGiftsItem(w: PacketWriter, m: NewPlayerExperienceGetGiftsIn.GiftsItem) {
        w.int(m.key1)
        w.int(m.key2)
        w.int(m.index)
    }

    private fun writeNewPlayerExperienceGetGiftsIn(w: PacketWriter, m: NewPlayerExperienceGetGiftsIn) {
        w.countInt(m.gifts.size)
        m.gifts.forEach { writeNewPlayerExperienceGetGiftsInGiftsItem(w, it) }
    }
}
