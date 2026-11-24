package zel.wired.effect.furni

import zel.wired.firing.Firing

interface TriesMoves {
    fun tryMoves(
        firing: Firing,
        trial: MoveTrial,
    )
}
