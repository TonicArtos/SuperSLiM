package com.tonicartos.superslim

import com.tonicartos.superslim.internal.SectionState

interface SectionLayoutManager<T : SectionState> {
    fun onLayout(helper: LayoutHelper, section: T)
    fun fillTopScrolledArea(dy: Int, helper: LayoutHelper, section: T): Int
    fun fillBottomScrolledArea(dy: Int, helper: LayoutHelper, section: T): Int
}

interface Child {
    companion object {
        const val INVALID = -1
        const val ANIM_NONE = 0
        const val ANIM_APPEARING = 1
        const val ANIM_DISAPPEARING = 2
    }

    fun done()

    /**
     * True if the child is being removed in this layout.
     */
    val isRemoved: Boolean

    val measuredWidth: Int
    val measuredHeight: Int
    fun measure(usedWidth: Int = 0, usedHeight: Int = 0)

    val left: Int
    val top: Int
    val right: Int
    val bottom: Int
    fun layout(left: Int = 0, top: Int = 0) = layout(left, top, 0, 0)
    fun layout(left: Int, top: Int, right: Int, bottom: Int)

    val width: Int
    val height: Int

    /**
     * The animation state for the child in this layout pass. An appearing child is one that will start offscreen and animate
     * onscreen. A disappearing child is the opposite. A normal child does neither. Valid values are per the
     * [AnimationState] annotation.
     */
    @AnimationState var animationState: Int

    /**
     * Adds child to the recycler view. Handles disappearing or appearing state per value set in [animationState].
     */
    fun addToRecyclerView() = addToRecyclerView(-1)

    /**
     * Adds child to the recycler view.
     */
    fun addToRecyclerView(i: Int)
}
