package zel.util

import io.netty.buffer.ByteBuf
import java.nio.charset.StandardCharsets

object PacketUtils {
    private const val FIRST_PRINTABLE = ' '

    /** Control characters show as `[code]`; the reader index stays. */
    fun formatPacket(buffer: ByteBuf): String =
        buffer.toString(buffer.readerIndex(), buffer.readableBytes(), StandardCharsets.UTF_8).let { text ->
            buildString(text.length) {
                text.forEach { char ->
                    if (char <
                        FIRST_PRINTABLE
                    ) {
                        append('[').append(char.code).append(']')
                    } else {
                        append(char)
                    }
                }
            }
        }
}
