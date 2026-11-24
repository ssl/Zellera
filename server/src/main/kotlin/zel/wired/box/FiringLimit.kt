package zel.wired.box

import zel.wired.firing.Firing

/**
 * [admits] only checks; [record] counts the firing once every limit and the usage gate let it run. A refused
 * firing runs nothing and blinks nothing, except a says firing, which blinks its trigger and the add-ons that work
 * before the condition check.
 */
interface FiringLimit : AddonBox {
    fun admits(firing: Firing): Boolean

    fun record(firing: Firing)
}
