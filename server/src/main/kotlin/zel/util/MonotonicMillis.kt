package zel.util

/** Never jumps with the wall clock: for gaps and windows only. */
fun monotonicMillis(): Long = System.nanoTime() / NANOS_PER_MILLI
