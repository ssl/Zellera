package zel.wired.variable.box

import java.math.BigInteger
import kotlin.random.Random

object ValueOps {
    const val SET = 0
    const val ADD = 1
    const val SUBTRACT = 2
    const val MULTIPLY = 3
    const val DIVIDE = 4
    const val POWER = 5
    const val MODULO = 6
    const val MIN = 40 // a minimum: the value stays at least the operand
    const val MAX = 41 // a maximum: at most the operand
    const val RANDOM = 50
    const val ABS = 60
    const val AND = 100
    const val OR = 101
    const val XOR = 102
    const val NOT = 103
    const val SHIFT_LEFT = 104
    const val SHIFT_RIGHT = 105
    const val BIT_COUNT = 110
    const val NEXT_CLEAR_INCLUSIVE = 111
    const val NEXT_SET_INCLUSIVE = 112
    const val PREVIOUS_CLEAR_INCLUSIVE = 113
    const val PREVIOUS_SET_INCLUSIVE = 114
    const val GET_BIT = 115
    const val SET_BIT = 116
    const val CLEAR_BIT = 117
    const val TOGGLE_BIT = 118
    const val NEXT_CLEAR_EXCLUSIVE = 119
    const val NEXT_SET_EXCLUSIVE = 120
    const val PREVIOUS_CLEAR_EXCLUSIVE = 121
    const val PREVIOUS_SET_EXCLUSIVE = 122

    private val WITHOUT_OPERAND = setOf(ABS, NOT, BIT_COUNT)

    fun takesOperand(operation: Int): Boolean = operation !in WITHOUT_OPERAND

    fun apply(
        operation: Int,
        value: Long,
        operand: Long,
        random: Random = Random.Default,
    ): Long =
        arithmetic(operation, value, operand, random)
            ?: bits(operation, value, operand)
            ?: value

    private fun arithmetic(
        operation: Int,
        value: Long,
        operand: Long,
        random: Random,
    ): Long? =
        when (operation) {
            SET -> operand
            ADD -> value + operand
            SUBTRACT -> value - operand
            MULTIPLY -> value * operand
            DIVIDE -> if (operand == 0L) byZero(value) else value / operand
            MODULO -> if (operand == 0L) 0 else value % operand
            POWER -> power(value, operand)
            MIN -> maxOf(value, operand)
            MAX -> minOf(value, operand)
            RANDOM -> if (operand <= 0) 0 else random.nextLong(0, randomBound(operand))
            ABS -> if (value == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(value)
            else -> null
        }

    /** As a floating division: the end of the range by sign, 0 for 0. */
    private fun byZero(value: Long): Long =
        when {
            value > 0 -> Long.MAX_VALUE
            value < 0 -> Long.MIN_VALUE
            else -> 0
        }

    private fun randomBound(operand: Long): Long = if (operand == Long.MAX_VALUE) operand else operand + 1

    private fun power(
        value: Long,
        exponent: Long,
    ): Long =
        when {
            exponent < 0 -> 0
            value == 0L -> if (exponent == 0L) 1 else 0
            value == 1L -> 1
            value == -1L -> if (exponent % 2 == 0L) 1 else -1
            exponent >= MAX_EXPONENT -> if (value > 0 || exponent % 2 == 0L) Long.MAX_VALUE else Long.MIN_VALUE
            else -> held(BigInteger.valueOf(value).pow(exponent.toInt()))
        }

    private fun held(value: BigInteger): Long =
        value.max(BigInteger.valueOf(Long.MIN_VALUE)).min(BigInteger.valueOf(Long.MAX_VALUE)).toLong()

    private fun bits(
        operation: Int,
        value: Long,
        operand: Long,
    ): Long? =
        when (operation) {
            AND -> {
                value and operand
            }

            OR -> {
                value or operand
            }

            XOR -> {
                value xor operand
            }

            NOT -> {
                value.inv()
            }

            SHIFT_LEFT -> {
                value shl operand.toInt()
            }

            SHIFT_RIGHT -> {
                value shr operand.toInt()
            }

            BIT_COUNT -> {
                java.lang.Long
                    .bitCount(value)
                    .toLong()
            }

            else -> {
                singleBit(operation, value, operand) ?: scan(operation, value, operand)
            }
        }

    private fun singleBit(
        operation: Int,
        value: Long,
        position: Long,
    ): Long? {
        val inside = position in 0 until Long.SIZE_BITS
        val mask = if (inside) 1L shl position.toInt() else 0
        return when (operation) {
            GET_BIT -> if (inside && value and mask != 0L) 1 else 0
            SET_BIT -> value or mask
            CLEAR_BIT -> value and mask.inv()
            TOGGLE_BIT -> value xor mask
            else -> null
        }
    }

    private fun scan(
        operation: Int,
        value: Long,
        position: Long,
    ): Long? {
        val start = position.coerceIn(-1, Long.SIZE_BITS.toLong()).toInt()

        fun clear(i: Int) = value and (1L shl i) == 0L

        fun set(i: Int) = !clear(i)
        return when (operation) {
            NEXT_CLEAR_INCLUSIVE -> up(start, ::clear)
            NEXT_CLEAR_EXCLUSIVE -> up(start + 1, ::clear)
            NEXT_SET_INCLUSIVE -> up(start, ::set)
            NEXT_SET_EXCLUSIVE -> up(start + 1, ::set)
            PREVIOUS_CLEAR_INCLUSIVE -> down(start, ::clear)
            PREVIOUS_CLEAR_EXCLUSIVE -> down(start - 1, ::clear)
            PREVIOUS_SET_INCLUSIVE -> down(start, ::set)
            PREVIOUS_SET_EXCLUSIVE -> down(start - 1, ::set)
            else -> null
        }
    }

    private fun up(
        from: Int,
        matches: (Int) -> Boolean,
    ): Long = (from.coerceAtLeast(0) until Long.SIZE_BITS).firstOrNull(matches)?.toLong() ?: -1

    private fun down(
        from: Int,
        matches: (Int) -> Boolean,
    ): Long = (from.coerceAtMost(Long.SIZE_BITS - 1) downTo 0).firstOrNull(matches)?.toLong() ?: -1

    private const val MAX_EXPONENT = 64L
}
