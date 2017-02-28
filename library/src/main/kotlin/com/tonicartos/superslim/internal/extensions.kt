package com.tonicartos.superslim.internal

import android.support.v7.widget.RecyclerView
import android.view.View
import java.util.*

internal val View.rvLayoutParams: RecyclerView.LayoutParams
    get() = layoutParams as RecyclerView.LayoutParams

inline fun <T : Stack<I>, I, R> T.babushka(block: T.(I) -> R): R {
    val item = pop()
    val r = block(item)
    push(item)
    return r
}

inline fun <T : Stack<SectionState.LayoutState>> T.plm(block: T.(SectionState.LayoutState) -> Unit): T {
    block(this[0])
    return this
}

inline fun <T : Stack<SectionState.LayoutState>> T.hlm(block: T.(SectionState.LayoutState) -> Unit): T {
    block(this[1])
    return this
}

inline fun <T : Stack<SectionState.LayoutState>> T.flm(block: T.(SectionState.LayoutState) -> Unit): T {
    block(this[2])
    return this
}

inline fun <T : Stack<SectionState.LayoutState>> T.slm(block: T.(SectionState.LayoutState) -> Unit): T {
    block(this[3])
    return this
}
