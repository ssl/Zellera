package zel.net.server

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise

class Rc4Stream(
    key: ByteArray,
) : ChannelDuplexHandler() {
    private val fromClient = Keystream(key)
    private val toClient = Keystream(key)

    override fun channelRead(
        ctx: ChannelHandlerContext,
        msg: Any,
    ) {
        ctx.fireChannelRead(if (msg is ByteBuf) Unpooled.wrappedBuffer(fromClient.applyTo(msg)) else msg)
    }

    override fun write(
        ctx: ChannelHandlerContext,
        msg: Any,
        promise: ChannelPromise,
    ) {
        ctx.write(if (msg is ByteBuf) ctx.alloc().buffer().writeBytes(toClient.applyTo(msg)) else msg, promise)
    }

    /** Owned by the channel's event loop. */
    internal class Keystream(
        key: ByteArray,
    ) {
        private val state = IntArray(STATE_SIZE) { it }
        private var x = 0
        private var y = 0

        init {
            var mix = 0
            for (at in state.indices) {
                mix = (mix + state[at] + key[at % key.size].toUByte().toInt()) and LAST
                swap(at, mix)
            }
        }

        fun applyTo(bytes: ByteArray) {
            for (at in bytes.indices) bytes[at] = (bytes[at].toInt() xor next()).toByte()
        }

        /** Releases [frame] and returns a crypted copy: a broadcast frame must stay readable for its other readers. */
        fun applyTo(frame: ByteBuf): ByteArray {
            val copy = ByteBufUtil.getBytes(frame)
            frame.release()
            applyTo(copy)
            return copy
        }

        private fun next(): Int {
            x = (x + 1) and LAST
            y = (y + state[x]) and LAST
            swap(x, y)
            return state[(state[x] + state[y]) and LAST]
        }

        private fun swap(
            a: Int,
            b: Int,
        ) {
            state[a] = state[b].also { state[b] = state[a] }
        }

        private companion object {
            const val STATE_SIZE = 256
            const val LAST = STATE_SIZE - 1
        }
    }
}
