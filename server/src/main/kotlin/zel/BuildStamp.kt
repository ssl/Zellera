package zel

import zel.core.BuildInfo
import java.nio.file.Files
import java.nio.file.Path

data class BuildStamp(
    val version: String,
    val hash: String,
    val builtAtMillis: Long,
) {
    companion object {
        /** What is unknown stays empty or 0. */
        fun ofRunningCode(): BuildStamp = BuildStamp(BuildInfo.VERSION, BuildInfo.HASH, codeWrittenAt())

        /** The jar's modification time (the build is reproducible and records no time); a plain copy resets it. */
        private fun codeWrittenAt(): Long =
            runCatching {
                val code =
                    BuildStamp::class.java.protectionDomain.codeSource
                        ?.location
                        ?: return 0L
                Files.getLastModifiedTime(Path.of(code.toURI())).toMillis()
            }.getOrDefault(0L)
    }
}
