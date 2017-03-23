package com.tonicartos.superslim.internal.layout

import android.util.Log
import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.SectionState.LayoutState
import com.tonicartos.superslim.internal.SectionState.PaddingLayoutState

internal object PaddingLayoutManager : SectionLayoutManager<SectionState> {
    override fun isAtTop(section: SectionState, layoutState: LayoutState): Boolean {
        val state = layoutState as PaddingLayoutState
        return state.overdraw == 0 && state.paddingTop > 0 || section.atTop
    }

    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        val state = layoutState as PaddingLayoutState
        state.paddingTop = helper.paddingTop

        if (section.atTop) {
            state.overdraw = 0
            Log.d("PADDING", "at top")
        } else {
            Log.d("PADDING", "not at top")
            state.overdraw = state.paddingTop
        }
        var y = state.paddingTop - state.overdraw

        section.layout(helper, section.leftGutter{0}, y, helper.layoutWidth - section.rightGutter{0})
        y += section.height + helper.paddingBottom
        state.disappearedOrRemovedHeight += section.disappearedHeight

        state.tailPosition = 0
        state.tailPosition = 0
        state.bottom = y
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        val state = layoutState as PaddingLayoutState
        state.paddingTop = helper.paddingTop

        var toFill = dy
        // How much distance left to fill.
        var filled = section.fillTop(toFill, section.leftGutter{0}, state.paddingTop - state.overdraw,
                                     helper.layoutWidth - section.rightGutter{0}, helper)
        Log.d("PADDING", "filled = $filled, state = $state")
        toFill -= filled
        filled += Math.min(toFill, state.overdraw)
        if (state.overdraw > 0) state.overdraw -= Math.min(toFill, state.overdraw)

        // Update state
        state.headPosition = 0
        state.tailPosition = 0
        state.bottom += filled
        return filled
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        // Always add paddingBottom to bottom of section. We never scroll through it unlike paddingTop so this is a
        // simple and easy way to manage it. The padding just has to be removed when calling through to the section to
        // continue filling content, and then re-added once done.
        Log.d("onFillTop", "limit = ${helper.layoutLimit}, state = $layoutState")
        val state = layoutState as PaddingLayoutState
        state.paddingTop = helper.paddingTop

        val paddingBottom = helper.paddingBottom
        val actualBottom = state.bottom - paddingBottom
        var filled = (actualBottom - helper.layoutLimit).takeIf { it > 0 } ?: 0
        var toFill = dy - filled
        if (toFill > 0) {
            section.fillBottom(toFill, section.leftGutter{0}, actualBottom,
                               helper.layoutWidth - section.rightGutter{0}, helper).let {
                toFill -= it
                filled += it
                state.bottom += it
            }
        }

        state.headPosition = 0
        state.tailPosition = 0
        return Math.min(dy, filled)
    }

    override fun onTrimTop(scrolled: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        Log.d("PADDING", "onTrimTop")
        val state = layoutState as PaddingLayoutState
        var filled = 0
        if (state.overdraw < state.paddingTop) {
            filled = Math.min(scrolled, state.paddingTop - state.overdraw)
            state.overdraw += filled
        }

        val removed = section.trimTop(scrolled - filled, 0, helper, 0)
        if (helper.numViews == 0) {
            state.headPosition = -1
            state.tailPosition = -1
        }
        state.bottom -= removed
        return removed
    }

    override fun onTrimBottom(scrolled: Int, helper: LayoutHelper, section: SectionState,
                              layoutState: LayoutState): Int {
        Log.d("PADDING", "onTrimBottom")
        val removed = section.trimBottom(scrolled, 0, helper, 0)
        if (helper.numViews == 0) {
            layoutState.headPosition = -1
            layoutState.tailPosition = -1
        }
        return removed
    }
}
