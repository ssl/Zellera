package zel.room.furni.behaviour.state

internal data class RandomDrawParams(
    val states: Int,
    val delay: Int,
) {
    companion object {
        fun parse(params: String): RandomDrawParams {
            val values =
                params
                    .split(",")
                    .map { it.split("=") }
                    .filter { it.size == 2 }
                    .associate { (key, value) -> key to value }
            val states = requireNotNull(values["states"]?.toInt()) { "no states in '$params'" }
            val delay = requireNotNull(values["delay"]?.toInt()) { "no delay in '$params'" }
            require(states >= 0 && delay >= 0) { "negative value in '$params'" }
            return RandomDrawParams(states, delay)
        }
    }
}
