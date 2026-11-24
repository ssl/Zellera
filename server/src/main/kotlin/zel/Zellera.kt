package zel

import zel.content.ContentCommand
import zel.core.BuildInfo
import kotlin.system.exitProcess

object Zellera {
    @JvmStatic
    fun main(args: Array<String>) {
        when {
            args.contentEquals(arrayOf("version")) -> println(BuildInfo.VERSION)
            KeysCommand.handles(args) -> exitProcess(KeysCommand.run(args, System.getenv(), System.err))
            ContentCommand.handles(args) -> ContentCommand.main(args)
            else -> ServerProcess().run(args)
        }
    }
}
