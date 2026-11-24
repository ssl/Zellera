package zel.core.console

interface ConsoleVerb {
    val verb: String

    val usage: String

    fun run(arguments: List<String>)
}
