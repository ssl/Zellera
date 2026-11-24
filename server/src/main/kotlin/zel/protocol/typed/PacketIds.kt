package zel.protocol.typed

/** An id a client sends names a row only when it is positive; 0 and -1 mean none. */
object PacketIds {
    fun isId(id: Int): Boolean = id > 0
}
