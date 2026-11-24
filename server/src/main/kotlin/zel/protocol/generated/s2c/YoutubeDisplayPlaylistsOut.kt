package zel.protocol.generated

data class YoutubeDisplayPlaylistsOut(
    val int1: Int,
    val list1: List<List1Item>,
    val string1: String,
) {
    data class List1Item(
        val string1: String,
        val string2: String,
        val string3: String,
    )
}

object YoutubeDisplayPlaylistsOutCodec : OutgoingCodec<YoutubeDisplayPlaylistsOut> {
    override val name = "YoutubeDisplayPlaylistsOut"

    override fun read(r: PacketReader): YoutubeDisplayPlaylistsOut = readYoutubeDisplayPlaylistsOut(r)

    override fun write(w: PacketWriter, m: YoutubeDisplayPlaylistsOut) = writeYoutubeDisplayPlaylistsOut(w, m)

    private fun readYoutubeDisplayPlaylistsOutList1Item(r: PacketReader): YoutubeDisplayPlaylistsOut.List1Item {
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        return YoutubeDisplayPlaylistsOut.List1Item(vString1, vString2, vString3)
    }

    private fun readYoutubeDisplayPlaylistsOut(r: PacketReader): YoutubeDisplayPlaylistsOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(6)) { readYoutubeDisplayPlaylistsOutList1Item(r) }
        val vString1 = r.string()
        return YoutubeDisplayPlaylistsOut(vInt1, vList1, vString1)
    }

    private fun writeYoutubeDisplayPlaylistsOutList1Item(w: PacketWriter, m: YoutubeDisplayPlaylistsOut.List1Item) {
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
    }

    private fun writeYoutubeDisplayPlaylistsOut(w: PacketWriter, m: YoutubeDisplayPlaylistsOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeYoutubeDisplayPlaylistsOutList1Item(w, it) }
        w.string(m.string1)
    }
}
