package zel.protocol.generated

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/** Reads a message body (big-endian ints, strings as unsigned short length + UTF-8). */
class PacketReader(body: ByteArray) {
    private val buf = ByteBuffer.wrap(body)

    val remaining: Int get() = buf.remaining()

    fun int(): Int = buf.getInt()
    fun short(): Short = buf.getShort()
    fun byte(): Byte = buf.get()
    fun boolean(): Boolean = buf.get().toInt() != 0
    fun long(): Long = buf.getLong()
    fun double(): Double = buf.getDouble()
    fun float(): Float = buf.getFloat()
    fun string(): String {
        val length = buf.getShort().toInt() and 0xFFFF
        if (length > buf.remaining()) malformed("string of $length bytes, ${buf.remaining()} left")
        val bytes = ByteArray(length)
        buf.get(bytes)
        return bytes.toString(Charsets.UTF_8)
    }
    fun rest(): ByteArray = ByteArray(buf.remaining()).also { buf.get(it) }

    /** A negative count reads as 0; one the rest of the frame cannot hold rejects the frame before allocating. */
    fun countInt(minItemBytes: Int): Int = count(int(), minItemBytes)
    fun countShort(minItemBytes: Int): Int = count(short().toInt(), minItemBytes)
    fun countByte(minItemBytes: Int): Int = count(byte().toInt() and 0xFF, minItemBytes)

    /** A list count read earlier in the body (see [countInt]). */
    fun count(declared: Int, minItemBytes: Int): Int {
        if (declared <= 0) return 0
        if (declared > MAX_LIST_COUNT) malformed("list of $declared items")
        if (declared.toLong() * minItemBytes > buf.remaining()) {
            malformed("list of $declared items of at least $minItemBytes bytes, ${buf.remaining()} left")
        }
        return declared
    }

    /** Items up to the end of the body; an item that reads no bytes rejects the frame. */
    fun <T> untilEnd(item: () -> T): List<T> = buildList {
        while (buf.remaining() > 0) {
            val before = buf.remaining()
            add(item())
            if (buf.remaining() == before || size > MAX_LIST_COUNT) malformed("list to the end of the body does not end")
        }
    }

    private fun malformed(what: String): Nothing = throw MalformedPacketException(what)

    companion object {
        /** A frame declaring a longer list is rejected whatever its size. */
        const val MAX_LIST_COUNT = 65_536
    }
}

/** A body whose declared lengths cannot be right; the frame is dropped like one that ends early. */
class MalformedPacketException(message: String) : RuntimeException(message)

/** Writes a message body in the layout PacketReader reads. */
class PacketWriter {
    private val out = ByteArrayOutputStream()

    fun int(v: Int) = apply { out.write(ByteBuffer.allocate(4).putInt(v).array()) }
    fun short(v: Short) = apply { out.write(ByteBuffer.allocate(2).putShort(v).array()) }
    fun byte(v: Byte) = apply { out.write(v.toInt()) }
    fun boolean(v: Boolean) = apply { out.write(if (v) 1 else 0) }
    fun long(v: Long) = apply { out.write(ByteBuffer.allocate(8).putLong(v).array()) }
    fun double(v: Double) = apply { out.write(ByteBuffer.allocate(8).putDouble(v).array()) }
    fun float(v: Float) = apply { out.write(ByteBuffer.allocate(4).putFloat(v).array()) }
    fun string(v: String) = apply {
        val bytes = v.toByteArray(Charsets.UTF_8)
        require(bytes.size <= 0xFFFF) { "string too long: ${bytes.size} bytes" }
        short(bytes.size.toShort())
        out.write(bytes)
    }
    fun bytes(v: ByteArray) = apply { out.write(v) }

    fun countInt(n: Int) = int(n)
    fun countShort(n: Int) = short(n.toShort())
    fun countByte(n: Int) = byte(n.toByte())

    fun toByteArray(): ByteArray = out.toByteArray()
}

/** One message: its name, typed reader and writer of its body. */
interface MessageCodec<T> {
    val name: String
    fun read(r: PacketReader): T
    fun write(w: PacketWriter, m: T)
}

/** A message the server reads. */
interface IncomingCodec<T> : MessageCodec<T>

/** A message the server writes. */
interface OutgoingCodec<T> : MessageCodec<T>
