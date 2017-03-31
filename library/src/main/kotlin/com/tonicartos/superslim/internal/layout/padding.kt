package com.tonicartos.superslim.internal.layout

import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.SectionState.LayoutState
import com.tonicartos.superslim.internal.SectionState.PaddingLayoutState
import com.tonicartos.superslim.internal.SectionState.PaddingLayoutState.Companion.BOTTOM_ADDED
import com.tonicartos.superslim.internal.SectionState.PaddingLayoutState.Companion.TOP_ADDED

internal object PaddingLayoutManager : SectionLayoutManager<SectionState> {
    override fun isAtTop(section: SectionState, layoutState: LayoutState): Boolean {
        val state = layoutState as PaddingLayoutState
        return state.overdraw == 0 && state.paddingTop > 0 || section.atTop
    }

    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        val state = layoutState as PaddingLayoutState
        state.paddingTop = helper.paddingTop
        state.paddingBottom = helper.paddingBottom

        if (state.paddingTop > 0) {
            if (state.onScreen && state flagUnset TOP_ADDED && state.overdraw > 0) {
                // Must be in a layout pass with requested position.
                state set TOP_ADDED
            } else if (!state.onScreen) {
                state.overdraw = 0
                state set TOP_ADDED
            }
        }
        state.onScreen = true

        var y = if (state flagSet TOP_ADDED) state.paddingTop - state.overdraw else 0

        section.layout(helper, section.leftGutter { 0 }, y, helper.layoutWidth - section.rightGutter { 0 })
        state.disappearedOrRemovedHeight += section.disappearedHeight
        y += section.height
        helper.filledArea += section.height

        if (state.paddingBottom > 0 && y < helper.layoutLimit) {
            state set BOTTOM_ADDED
            y += helper.paddingBottom
        }

        state.bottom = y
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        val state = layoutState as PaddingLayoutState

        var toFill = dy - state.overdraw

        if (state.paddingBottom > 0 && !state.onScreen) {
            state.paddingTop = helper.paddingTop
            state.paddingBottom = helper.paddingBottom

            // Add bottom padding.
            val filled = state.paddingBottom
            state.overdraw += filled
            toFill -= filled
            state set BOTTOM_ADDED
        }
        state.onScreen = true

        // Add content.
        state.overdraw += section.fillTop(Math.max(0, toFill), section.leftGutter { 0 }, -state.overdraw,
                                          helper.layoutWidth - section.rightGutter { 0 }, helper)

        if (state.paddingTop > 0 && state flagUnset TOP_ADDED && state.overdraw < dy) {
            // Add top padding.
            state.overdraw += state.paddingTop
            state set TOP_ADDED
        }

        val filled = Math.min(dy, state.overdraw)
        state.overdraw -= filled
        state.bottom += filled
        return filled
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        val state = layoutState as PaddingLayoutState
        var filled = 0

        if (state.paddingTop > 0 && !state.onScreen) {
            state.paddingBottom = helper.paddingBottom
            state.paddingTop = helper.paddingTop

            // Add top padding.
            filled += state.paddingTop
            state.bottom = state.paddingTop
            state.overdraw = 0
            state set TOP_ADDED
        }
        state.onScreen = true

        val y = if (state flagSet TOP_ADDED) state.paddingTop - state.overdraw else 0

        // Add content
        val before = section.height
        filled += section.fillBottom(dy, section.leftGutter { 0 }, y, helper.layoutWidth - section.rightGutter { 0 },
                                     helper)
        state.bottom += section.height - before

        if (state.paddingBottom > 0 && filled < dy) {
            if (state flagUnset BOTTOM_ADDED) {
                // Add bottom padding.
                filled += state.paddingBottom
                state.bottom += state.paddingBottom
                state set BOTTOM_ADDED
            } else {
                filled += Math.max(0, state.bottom - helper.layoutLimit)
            }
        }

        return Math.min(dy, filled)
    }

    override fun onTrimTop(scrolled: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        val state = layoutState as PaddingLayoutState

        var removedHeight = 0
        var contentTop = 0

        if (state flagSet TOP_ADDED) {
            val before = state.overdraw
            state.overdraw = Math.min(state.paddingTop, state.overdraw + scrolled)
            removedHeight += state.overdraw - before

            // Do padding top.
            if (state.overdraw >= state.paddingTop) {
                state.overdraw = 0
                state unset TOP_ADDED
            } else {
                contentTop = state.paddingTop - state.overdraw
            }
        }

        removedHeight += section.trimTop(scrolled, contentTop, helper)

        if (helper.numViews == 0 && state flagSet BOTTOM_ADDED) {
            val before = state.overdraw
            state.overdraw = Math.min(state.paddingBottom, state.overdraw + (scrolled - removedHeight))
            removedHeight += state.overdraw - before

            // Do padding bottom.
            if (state.bottom < 0) {
                state.overdraw = 0
                state unset BOTTOM_ADDED
                state.onScreen = false
            }
        }

        state.bottom -= removedHeight
        return removedHeight
    }

    override fun onTrimBottom(scrolled: Int, helper: LayoutHelper, section: SectionState,
                              layoutState: LayoutState): Int {
        val state = layoutState as PaddingLayoutState
        var removedHeight = 0
        if (state flagSet BOTTOM_ADDED) {
            // Do padding bottom.
            if (state.bottom - state.paddingBottom > helper.layoutLimit) {
                removedHeight += state.paddingBottom
                state unset BOTTOM_ADDED
            }
        }

        val contentTop = if (state flagSet TOP_ADDED) state.paddingTop - state.overdraw else 0

        // Do content.
        removedHeight += section.trimBottom(scrolled - removedHeight, contentTop, helper)

        if (state flagSet TOP_ADDED) {
            // Do padding top.
            if (helper.layoutLimit < 0) {
                removedHeight += state.paddingBottom
                state unset TOP_ADDED
                state.onScreen = false
            }
        }
        state.bottom -= removedHeight
        return removedHeight
    }
}
