package zel.protocol.typed

fun interface PacketHandler<T> {
    fun handle(
        session: PacketSession,
        message: T,
    )
}
