package zel.content

import java.nio.file.Files
import java.nio.file.Path

internal data class ContentDirs(
    val gamedata: Path?,
    val data: Path?,
    val assets: Path?,
    val client: Path? = null,
) {
    val figuredata: Path? get() = gamedata?.resolve(FIGUREDATA_FILE)?.takeIf(Files::isRegularFile)

    companion object {
        const val FIGUREDATA_FILE = "figurepartlist"

        fun fromEnvironment(env: Map<String, String> = System.getenv()) =
            ContentDirs(
                path(env, "ZEL_GAMEDATA_DIR"),
                path(env, "ZEL_DATA_DIR"),
                path(env, "ZEL_ASSET_DIR"),
                path(env, "ZEL_CLIENT_DIR"),
            )

        private fun path(
            env: Map<String, String>,
            key: String,
        ) = env[key]?.takeIf { it.isNotBlank() }?.let(Path::of)
    }
}
