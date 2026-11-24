package zel.game.players.figure

import java.io.InputStream
import java.net.URI
import java.time.Duration
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants.END_ELEMENT
import javax.xml.stream.XMLStreamConstants.START_ELEMENT
import javax.xml.stream.XMLStreamReader

object FiguredataParser {
    private val CONNECT_TIMEOUT = Duration.ofSeconds(5)
    private val READ_TIMEOUT = Duration.ofSeconds(15)

    /** Reads without a DOCTYPE or external entities: the document comes from a configured URL. */
    private val XML: XMLInputFactory =
        XMLInputFactory.newDefaultFactory().apply {
            setProperty(XMLInputFactory.SUPPORT_DTD, false)
            setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
        }

    fun read(url: String): Figuredata = read(url, CONNECT_TIMEOUT, READ_TIMEOUT)

    /** A host that does not answer in time fails the read instead of holding up the start or a reload. */
    internal fun read(
        url: String,
        connectTimeout: Duration,
        readTimeout: Duration,
    ): Figuredata {
        val connection = URI(url).toURL().openConnection()
        connection.connectTimeout = connectTimeout.toMillis().toInt()
        connection.readTimeout = readTimeout.toMillis().toInt()
        val read = connection.getInputStream().use { input -> Section.rootOf(input.xml()) }
        val root = requireNotNull(read?.takeIf { it.tag == "figuredata" }) { "not a figuredata file" }
        return Figuredata(
            root["colors"].flatMap { it["palette"] }.map(::palette).associateBy { it.id },
            root["sets"].flatMap { it["settype"] }.map(::setType).associateBy { it.type },
        )
    }

    private fun palette(section: Section) =
        Figuredata.Palette(
            section.number("id"),
            section["color"].map {
                Figuredata.Color(it.number("id"), it.number("index"), it.flag("club"), it.flag("selectable"))
            },
        )

    private fun setType(section: Section): Figuredata.SetType {
        val mandatoryFor = HashSet<Figuredata.Requirement>()
        for (gender in Figuredata.Requirement.genders) {
            for (club in listOf(false, true)) {
                val key = "mand_${gender.lowercase()}_${if (club) 1 else 0}"
                if (section.flag(key)) mandatoryFor += Figuredata.Requirement(gender, club)
            }
        }
        return Figuredata.SetType(
            section.text("type"),
            section.number("paletteid"),
            mandatoryFor,
            section["set"].map(::clothingSet),
        )
    }

    private fun clothingSet(section: Section) =
        Figuredata.ClothingSet(
            section.number("id"),
            section.text("gender"),
            section.flag("club"),
            section.flag("colorable"),
            section.flag("selectable"),
            section.flag("sellable"),
            section["part"].maxOfOrNull { it.number("colorindex") } ?: 0,
        )

    private fun InputStream.xml(): XMLStreamReader = XML.createXMLStreamReader(this)

    private class Section(
        val tag: String,
        private val attributes: Map<String, String>,
        private val nested: List<Section>,
    ) {
        operator fun get(childTag: String): List<Section> = nested.filter { it.tag == childTag }

        fun text(key: String): String = attributes[key].orEmpty()

        fun number(key: String): Int = text(key).toIntOrNull() ?: 0

        fun flag(key: String): Boolean = number(key) > 0

        companion object {
            /** Null for a document without a root; closes the reader. */
            fun rootOf(xml: XMLStreamReader): Section? =
                try {
                    var root: Section? = null
                    while (root == null && xml.hasNext()) {
                        if (xml.next() == START_ELEMENT) root = sectionAt(xml)
                    }
                    root
                } finally {
                    xml.close()
                }

            private fun sectionAt(xml: XMLStreamReader): Section {
                val tag = xml.localName
                val attributes =
                    (0 until xml.attributeCount).associate {
                        xml.getAttributeLocalName(it) to xml.getAttributeValue(it)
                    }
                val nested = ArrayList<Section>()
                while (true) {
                    when (xml.next()) {
                        START_ELEMENT -> nested += sectionAt(xml)
                        END_ELEMENT -> return Section(tag, attributes, nested)
                    }
                }
            }
        }
    }
}
