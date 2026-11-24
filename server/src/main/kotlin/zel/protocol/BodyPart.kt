package zel.protocol

import zel.protocol.generated.PacketWriter

fun interface BodyPart {
    fun writeTo(body: PacketWriter)
}
