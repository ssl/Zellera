package zel.net.server.proxy

import zel.core.Configuration

class CloudflareEdge(
    private val blocks: List<AddressBlock>,
) {
    fun contains(ip: String?): Boolean {
        val address = IpLiteral.bytes(ip) ?: return false
        return blocks.any { it.contains(address) }
    }

    companion object {
        val NONE = CloudflareEdge(emptyList())

        private val PUBLISHED =
            listOf(
                "173.245.48.0/20",
                "103.21.244.0/22",
                "103.22.200.0/22",
                "103.31.4.0/22",
                "141.101.64.0/18",
                "108.162.192.0/18",
                "190.93.240.0/20",
                "188.114.96.0/20",
                "197.234.240.0/22",
                "198.41.128.0/17",
                "162.158.0.0/15",
                "104.16.0.0/13",
                "104.24.0.0/14",
                "172.64.0.0/13",
                "131.0.72.0/22",
                "2400:cb00::/32",
                "2606:4700::/32",
                "2803:f800::/32",
                "2405:b500::/32",
                "2405:8100::/32",
                "2a06:98c0::/29",
                "2c0f:f248::/32",
            )

        fun published(): CloudflareEdge = CloudflareEdge(PUBLISHED.mapNotNull(AddressBlock::parse))

        fun configured(configuration: Configuration): CloudflareEdge =
            if (configuration.getBoolean("network.client_address.trusts_cloudflare", false)) published() else NONE
    }
}
