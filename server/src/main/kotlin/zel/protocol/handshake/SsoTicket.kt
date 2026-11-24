package zel.protocol.handshake

object SsoTicket {
    /** The web login issues far shorter tickets. */
    const val MAX_LENGTH = 128

    fun accepted(raw: String?): String? = raw.orEmpty().filterNot { it == ' ' }.takeIf { it.length in 1..MAX_LENGTH }
}
