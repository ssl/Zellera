package zel.game.guilds

import zel.core.Configuration
import zel.db.Database
import zel.db.Sql
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.floor
import kotlin.math.roundToInt

class BadgeImager(
    private val configuration: Configuration,
    private val database: Database,
    private val guilds: GuildDirectory,
) {
    private val parts = HashMap<String, BufferedImage>()

    init {
        if (configuration.getBoolean("groups.badges.are_drawn")) {
            if (reload()) {
                logger.info("badge imager ready")
                generateMissing()
            } else {
                logger.warn("badge imager off: its badge parts folder is missing or holds none of the parts")
            }
        }
    }

    /** Tinted with [rgb] unless that is negative. */
    internal data class Layer(
        val image: BufferedImage,
        val rgb: Int,
        val position: Int,
    )

    /** True when at least one badge part loaded; badges that need a missing part are skipped, not drawn partly. */
    @Synchronized
    fun reload(): Boolean {
        val folder = File(configuration.getValue("groups.badges.parts.folder"))
        if (!folder.exists()) {
            logger.error("badge imager: the badge parts folder {} is missing", folder)
            return false
        }

        parts.clear()
        return try {
            listOf(GuildPartType.BASE, GuildPartType.SYMBOL)
                .flatMap { guilds.parts.partsOf(it).values }
                .flatMap { listOf(it.asset, it.assetMask) }
                .filter { it.isNotEmpty() }
                .forEach { loadPart(folder, it) }
            parts.isNotEmpty()
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception, // a broken part list turns off the imager only
        ) {
            logger.error("badge imager: loading the badge parts failed", e)
            false
        }
    }

    private fun loadPart(
        folder: File,
        name: String,
    ) {
        val file = File(folder, "badgepart_" + name.replace(".gif", ".png"))
        val image =
            try {
                ImageIO.read(file)
            } catch (
                @Suppress("TooGenericExceptionCaught") _: Exception, // an unreadable part counts as missing
            ) {
                null
            }
        if (image == null) {
            logger.info("badge imager: missing badge part {}", file)
        } else {
            parts[name] = image
        }
    }

    fun generateMissing() {
        val out = File(outputDir())
        if (!out.isDirectory && !out.mkdirs()) {
            logger.warn("badge output folder {} is missing and cannot be made", out)
            return
        }
        val made =
            try {
                renderMissingBadges(out)
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception, // the backfill is best effort at startup
            ) {
                logger.error("group badge backfill failed", e)
                0
            }
        if (made > 0) {
            logger.info("rendered {} missing group badges", made)
        }
    }

    private fun renderMissingBadges(out: File): Int {
        val missing = allBadges().filter { GuildBadgeCode.isCode(it) && !File(out, "$it.png").exists() }
        return missing.count { generate(it) }
    }

    private fun allBadges(): List<String> =
        Sql(
            database.dataSource,
        ).select("SELECT DISTINCT badge_code FROM guilds").rows { it.getString("badge_code").orEmpty() }

    private fun outputDir(): String = configuration.getValue("groups.badges.output.folder")

    fun generate(guild: Guild) {
        generate(guild.badge)
    }

    /**
     * Writes the badge image unless it exists. Returns false without writing when a part it needs is missing, so a
     * later run can still make it once the part is there.
     */
    fun generate(badge: String): Boolean {
        val codes = GuildBadgeCode.parse(badge) ?: return false
        val outputFile = File(outputDir(), "$badge.png")
        if (outputFile.exists()) {
            return false
        }

        val layers = codes.map { code -> layersOf(code) }
        if (layers.any { it == null }) {
            logger.info("badge imager: skipped badge {}, a part it needs is missing", badge)
            return false
        }
        return try {
            ImageIO.write(compose(layers.flatMap { it.orEmpty() }), "PNG", outputFile)
        } catch (
            @Suppress("TooGenericExceptionCaught") _: Exception, // an unwritable folder must not stop the other badges
        ) {
            logger.error("badge imager: cannot write {}; make sure the output folder is writable", outputFile)
            false
        }
    }

    /** The layers of one badge part, or null when the part or one of its images is missing. */
    private fun layersOf(code: GuildBadgeCode.Part): List<Layer>? {
        val part =
            guilds.parts.part(if (code.base) GuildPartType.BASE else GuildPartType.SYMBOL, code.editorId)
                ?: return null
        val colour = guilds.parts.part(GuildPartType.BASE_TINT, code.colour)
        val tint = colour?.let { hex(it.asset) } ?: WHITE
        val tinted = if (part.asset.isEmpty()) null else parts[part.asset] ?: return null
        val plain = if (part.assetMask.isEmpty()) null else parts[part.assetMask] ?: return null
        return listOfNotNull(
            tinted?.let { Layer(it, tint, code.position) },
            plain?.let { Layer(it, -1, code.position) },
        )
    }

    internal companion object {
        private val logger = LoggerFactory.getLogger(BadgeImager::class.java)

        const val SIZE = 39
        private const val CELL = 13
        private const val GRID = 3
        private const val WHITE = 0xffffff
        private const val OPAQUE = 0xff000000.toInt()
        private const val CHANNEL_MASK = 0xff
        private const val ALPHA_MAX = 255
        private const val ALPHA_SHIFT = 24
        private const val RED_SHIFT = 16
        private const val GREEN_SHIFT = 8
        private const val TINT_SHIFT = 8
        private const val HEX_RADIX = 16

        fun point(
            width: Int,
            height: Int,
            position: Int,
        ): Pair<Int, Int> = place(position % GRID, width) to place(position / GRID, height)

        private fun place(
            cell: Int,
            size: Int,
        ): Int {
            val at = CELL * cell + CELL / 2.0 - size / 2.0
            return floor(minOf(maxOf(at, 0.0), (SIZE - size).toDouble())).toInt()
        }

        private fun channel(
            argb: Int,
            shift: Int,
        ): Int = (argb shr shift) and CHANNEL_MASK

        fun tint(
            argb: Int,
            rgb: Int,
        ): Int {
            val r = (channel(argb, RED_SHIFT) * channel(rgb, RED_SHIFT)) shr TINT_SHIFT
            val g = (channel(argb, GREEN_SHIFT) * channel(rgb, GREEN_SHIFT)) shr TINT_SHIFT
            val b = (channel(argb, 0) * channel(rgb, 0)) shr TINT_SHIFT
            return OPAQUE or (r shl RED_SHIFT) or (g shl GREEN_SHIFT) or b
        }

        fun over(
            src: Int,
            dst: Int,
        ): Int {
            val sa = src ushr ALPHA_SHIFT
            val da = dst ushr ALPHA_SHIFT
            val ra = sa + (da * (ALPHA_MAX - sa) / ALPHA_MAX.toDouble()).roundToInt()
            return when {
                sa == ALPHA_MAX -> {
                    src
                }

                ra == 0 -> {
                    0
                }

                else -> {
                    listOf(RED_SHIFT, GREEN_SHIFT, 0).fold(ra shl ALPHA_SHIFT) { out, shift ->
                        val s = channel(src, shift)
                        val d = channel(dst, shift)
                        val c = ((s * sa + d * da * (ALPHA_MAX - sa) / ALPHA_MAX.toDouble()) / ra).roundToInt()
                        out or (c shl shift)
                    }
                }
            }
        }

        fun compose(layers: List<Layer>): BufferedImage {
            val badge = BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB)
            for (layer in layers) {
                val image = layer.image
                val (atX, atY) = point(image.width, image.height, layer.position)
                for (y in 0 until image.height) {
                    val by = atY + y
                    if (by !in 0 until SIZE) continue
                    for (x in 0 until image.width) {
                        val bx = atX + x
                        var pixel = image.getRGB(x, y)
                        if (bx !in 0 until SIZE || (pixel ushr ALPHA_SHIFT) == 0) continue
                        if (layer.rgb >= 0) {
                            pixel = tint(pixel, layer.rgb)
                        }
                        badge.setRGB(bx, by, over(pixel, badge.getRGB(bx, by)))
                    }
                }
            }
            return badge
        }

        private fun hex(value: String): Int = value.toIntOrNull(HEX_RADIX)?.and(WHITE) ?: WHITE
    }
}
