package zel.wired.box

import zel.wired.firing.Firing

interface ConditionBox : WiredBox {
    fun passes(firing: Firing): Boolean
}
