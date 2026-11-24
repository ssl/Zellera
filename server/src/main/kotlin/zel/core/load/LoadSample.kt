package zel.core.load

/** How full each meter is, 0 to 1, at one moment; a meter that could not be read is left out. */
data class LoadSample(
    val shares: Map<ServerGauges.Meter, Double>,
    val databaseDown: Boolean = false,
) {
    init {
        shares.forEach { (meter, share) ->
            require(share.isFinite() && share in 0.0..1.0) { "$meter reads $share, not a share between 0 and 1" }
        }
    }
}
