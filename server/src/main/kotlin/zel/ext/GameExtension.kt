package zel.ext

interface GameExtension {
    fun start(host: ExtensionHost)

    fun stop() {}
}
