package zel.protocol.generated

/** Low byte of [flags] is the format; bit 0x100 adds the limited edition numbers. */
data class StuffData(val flags: Int, val value: Value, val limited: Limited? = null) {
    val format: Int get() = flags and 0xFF

    data class Limited(val uniqueNumber: Int, val uniqueSeries: Int)

    sealed interface Value {
        data class Legacy(val state: String) : Value
        data class MapData(val entries: List<Pair<String, String>>) : Value
        data class StringArray(val values: List<String>) : Value
        data class Vote(val state: String, val result: Int) : Value
        data object Empty : Value
        data class IntArray(val values: List<Int>) : Value
        data class HighScore(val state: String, val scoreType: Int, val clearType: Int, val entries: List<HighScoreEntry>) : Value
        data class Crackable(val state: String, val hits: Int, val target: Int) : Value
    }

    data class HighScoreEntry(val score: Int, val users: List<String>)

    companion object {
        fun read(r: PacketReader): StuffData {
            val flags = r.int()
            val value = when (flags and 0xFF) {
                0 -> Value.Legacy(r.string())
                1 -> Value.MapData(List(r.countInt(4)) { r.string() to r.string() })
                2 -> Value.StringArray(List(r.countInt(2)) { r.string() })
                3 -> Value.Vote(r.string(), r.int())
                4 -> Value.Empty
                5 -> Value.IntArray(List(r.countInt(4)) { r.int() })
                6 -> Value.HighScore(
                    r.string(), r.int(), r.int(),
                    List(r.countInt(8)) { HighScoreEntry(r.int(), List(r.countInt(2)) { r.string() }) },
                )
                7 -> Value.Crackable(r.string(), r.int(), r.int())
                else -> throw IllegalArgumentException("stuff data format ${flags and 0xFF}")
            }
            val limited = if (flags and 0x100 != 0) Limited(r.int(), r.int()) else null
            return StuffData(flags, value, limited)
        }

        fun write(w: PacketWriter, s: StuffData) {
            w.int(s.flags)
            when (val v = s.value) {
                is Value.Legacy -> w.string(v.state)
                is Value.MapData -> { w.countInt(v.entries.size); v.entries.forEach { (k, x) -> w.string(k).string(x) } }
                is Value.StringArray -> { w.countInt(v.values.size); v.values.forEach { w.string(it) } }
                is Value.Vote -> w.string(v.state).int(v.result)
                Value.Empty -> {}
                is Value.IntArray -> { w.countInt(v.values.size); v.values.forEach { w.int(it) } }
                is Value.HighScore -> {
                    w.string(v.state).int(v.scoreType).int(v.clearType).countInt(v.entries.size)
                    v.entries.forEach { e -> w.int(e.score).countInt(e.users.size); e.users.forEach { w.string(it) } }
                }
                is Value.Crackable -> w.string(v.state).int(v.hits).int(v.target)
            }
            s.limited?.let { w.int(it.uniqueNumber).int(it.uniqueSeries) }
        }
    }
}
