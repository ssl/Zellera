package zel.protocol.typed

interface PacketPackage {
    val name: String
    val routes: List<PacketRoute<*>>
}
