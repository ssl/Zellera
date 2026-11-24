@file:Suppress("MagicNumber") // fixed badge part tables and code layout

package zel.game.guilds

import java.security.MessageDigest
import java.util.HexFormat

object GuildBadgeCode {
    const val MAX_PARTS = 5
    private const val VALUES_PER_PART = 3
    const val MAX_POSITION = 8
    const val MAX_COLOUR = 99

    private const val SYMBOL_THRESHOLD = 100
    private const val NUMBER_DIGITS = 2

    private val BASES =
        intArrayOf(
            1,
            1,
            2,
            2,
            3,
            3,
            4,
            4,
            5,
            5,
            6,
            6,
            7,
            7,
            8,
            8,
            9,
            9,
            10,
            10,
            11,
            11,
            12,
            12,
            13,
            13,
            14,
            14,
            15,
            15,
            16,
            16,
            17,
            17,
            18,
            18,
            19,
            19,
            20,
            20,
            182,
            25,
            183,
            26,
            184,
            27,
            185,
            28,
            129,
            21,
            130,
            22,
            131,
            23,
            132,
            24,
        )

    private val SYMBOLS =
        intArrayOf(
            21,
            1,
            22,
            2,
            23,
            3,
            24,
            4,
            25,
            5,
            26,
            6,
            27,
            7,
            28,
            8,
            29,
            9,
            30,
            10,
            31,
            11,
            32,
            12,
            33,
            13,
            34,
            14,
            35,
            15,
            36,
            16,
            37,
            17,
            38,
            18,
            39,
            19,
            40,
            20,
            41,
            21,
            42,
            22,
            43,
            23,
            44,
            24,
            45,
            25,
            46,
            26,
            47,
            27,
            48,
            28,
            49,
            29,
            50,
            30,
            51,
            31,
            52,
            32,
            53,
            33,
            54,
            34,
            55,
            35,
            56,
            36,
            57,
            37,
            58,
            38,
            59,
            39,
            60,
            40,
            61,
            41,
            62,
            42,
            63,
            43,
            64,
            44,
            133,
            45,
            134,
            46,
            135,
            47,
            136,
            48,
            137,
            49,
            138,
            50,
            139,
            51,
            140,
            52,
            186,
            104,
            141,
            53,
            187,
            105,
            142,
            54,
            188,
            106,
            143,
            55,
            189,
            107,
            144,
            56,
            190,
            108,
            145,
            57,
            191,
            109,
            146,
            58,
            192,
            110,
            147,
            59,
            193,
            111,
            148,
            60,
            194,
            112,
            149,
            61,
            195,
            113,
            150,
            62,
            151,
            63,
            196,
            115,
            152,
            64,
            197,
            116,
            153,
            65,
            198,
            117,
            154,
            66,
            199,
            118,
            155,
            67,
            200,
            119,
            201,
            120,
            202,
            121,
            203,
            122,
            204,
            123,
            205,
            124,
            206,
            125,
            207,
            126,
            208,
            127,
            209,
            128,
            210,
            129,
            211,
            130,
            212,
            131,
            213,
            132,
            214,
            133,
            215,
            134,
            216,
            135,
            217,
            136,
            218,
            137,
            219,
            138,
            220,
            139,
            221,
            140,
            222,
            142,
            223,
            143,
            224,
            144,
            225,
            145,
            226,
            146,
            227,
            147,
            228,
            148,
            229,
            149,
            230,
            150,
            231,
            151,
            232,
            152,
            233,
            153,
            234,
            154,
            235,
            156,
            236,
            157,
            237,
            68,
            238,
            69,
            239,
            70,
            240,
            71,
            241,
            72,
            242,
            73,
            243,
            74,
            244,
            75,
            245,
            76,
            246,
            77,
            247,
            78,
            248,
            79,
            249,
            80,
            250,
            81,
            251,
            82,
            252,
            83,
            253,
            84,
            254,
            85,
            255,
            86,
            256,
            87,
            257,
            88,
            258,
            89,
            259,
            90,
            260,
            91,
            261,
            92,
            262,
            93,
            263,
            94,
            264,
            95,
            265,
            96,
            266,
            97,
            267,
            98,
            268,
            99,
            269,
            100,
            270,
            101,
            271,
            102,
            272,
            103,
            273,
            141,
            274,
            114,
            275,
            158,
            276,
            155,
        )

    private val PART_COLOURS =
        intArrayOf(
            1,
            11,
            2,
            3,
            4,
            18,
            5,
            19,
            6,
            16,
            7,
            8,
            9,
            20,
            21,
            10,
            22,
            17,
            14,
            23,
            15,
            12,
            24,
            13,
        )

    private val PART_COLOUR_ORDER: Map<Int, Int> = PART_COLOURS.withIndex().associate { (index, id) -> id to index }

    private val BASE_NUMBERING = Numbering(BASES)
    private val SYMBOL_NUMBERING = Numbering(SYMBOLS)

    private val CODE = Regex("^((?:[bst][0-9]{5}){1,$MAX_PARTS})([0-9a-f]{32})$")
    private val PART = Regex("([bst])([0-9]{2})([0-9]{2})([0-9])")

    data class Part(
        val base: Boolean,
        val editorId: Int,
        val colour: Int,
        val position: Int,
    )

    fun fromEditor(values: List<Int>): String? {
        if (values.size % VALUES_PER_PART != 0) return null
        return encode(
            values.chunked(VALUES_PER_PART).mapIndexed { index, (id, colour, position) ->
                Part(index == 0, id, colour, position)
            },
        )
    }

    fun encode(parts: List<Part>): String? {
        if (parts.isEmpty() || parts.size > MAX_PARTS) return null
        val body = parts.map { encode(it) ?: return null }.joinToString("")
        return body + hash(body)
    }

    private fun encode(part: Part): String? {
        val number = (if (part.base) BASE_NUMBERING else SYMBOL_NUMBERING).code(part.editorId)
        if (number < 0 || part.colour !in 0..MAX_COLOUR || part.position !in 0..MAX_POSITION) return null
        val letter =
            when {
                part.base -> 'b'
                number >= SYMBOL_THRESHOLD -> 't'
                else -> 's'
            }
        return "$letter${twoDigits(number % SYMBOL_THRESHOLD)}${twoDigits(part.colour)}${part.position}"
    }

    fun parse(code: String): List<Part>? {
        val match = CODE.matchEntire(code) ?: return null
        return PART
            .findAll(match.groupValues[1])
            .mapNotNull { part ->
                val letter = part.groupValues[1][0]
                val number = part.groupValues[2].toInt() + (if (letter == 't') SYMBOL_THRESHOLD else 0)
                val editorId = (if (letter == 'b') BASE_NUMBERING else SYMBOL_NUMBERING).editor(number)
                if (editorId >= 0) {
                    Part(letter == 'b', editorId, part.groupValues[3].toInt(), part.groupValues[4].toInt())
                } else {
                    null
                }
            }.toList()
    }

    fun isCode(code: String): Boolean = CODE.matches(code)

    fun editorOrder(
        base: Boolean,
        editorId: Int,
    ): Int {
        val at = (if (base) BASE_NUMBERING else SYMBOL_NUMBERING).order(editorId)
        return if (at >= 0) at else Int.MAX_VALUE
    }

    fun colourOrder(colourId: Int): Int = PART_COLOUR_ORDER[colourId] ?: Int.MAX_VALUE

    internal fun hash(body: String): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(body.toByteArray(Charsets.US_ASCII)))

    private fun twoDigits(value: Int): String = value.toString().padStart(NUMBER_DIGITS, '0')

    private class Numbering(
        pairs: IntArray,
    ) {
        private val entries = pairs.toList().chunked(2) { (editor, code) -> editor to code }
        private val codeByEditor = entries.toMap()
        private val editorByCode = entries.associate { (editor, code) -> code to editor }
        private val orderByEditor = entries.withIndex().associate { (index, entry) -> entry.first to index }

        fun code(editorId: Int): Int = codeByEditor[editorId] ?: -1

        fun editor(code: Int): Int = editorByCode[code] ?: -1

        fun order(editorId: Int): Int = orderByEditor[editorId] ?: -1
    }
}
