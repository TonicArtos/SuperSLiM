package com.tonicartos.superslim.internal.layout

import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionConfig
import com.tonicartos.superslim.SectionConfig.Companion.GUTTER_AUTO
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.SectionState.PaddingLayoutState
import com.tonicartos.superslim.internal.SectionState.LayoutState
import com.tonicartos.superslim.use

internal object PaddingLayoutManager : SectionLayoutManager<SectionState> {
    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        val state = layoutState as PaddingLayoutState
        var y = -state.overdraw
        if (helper.moreToLayout(0, section)) {
            section.layoutContent(helper, section.leftGutter(), y, helper.layoutWidth - section.rightGutter())
            y += section.height
        }
        state.tailPosition = 0
        state.tailPosition = 0
        state.bottom = y
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState) : Int {
        val state = layoutState as PaddingLayoutState

        // How much distance left to fill.
        val toFill = dy - state.overdraw

        if (toFill > 0) {
            // Fill space starting at overdraw.
            val y = -state.overdraw
            val filled = section.fillContentTop(toFill, section.leftGutter(), y,
                                                helper.layoutWidth - section.rightGutter(), helper)
            // Update partial state and compute dy filled.
            state.overdraw += filled
        }
        val filled = Math.min(dy, state.overdraw)

        // Update state
        state.overdraw -= filled
        state.bottom += filled
        state.headPosition = 0
        state.tailPosition = 0
        return filled
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        val state = layoutState as PaddingLayoutState

        var filled = (state.bottom - helper.layoutLimit).takeIf { it > 0} ?: 0
        var toFill = dy - filled
        if (toFill > 0) {
            section.fillContentBottom(toFill, section.leftGutter(), state.bottom,
                                      helper.layoutWidth - section.rightGutter(), helper).let {
                toFill -= it
                filled += it
                state.bottom += it
            }
        }

        state.bottom += filled
        state.headPosition = 0
        state.tailPosition = 0
        return Math.min(dy, filled)
    }

    override fun onTrimTop(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {

    }

    override fun onTrimBottom(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {

    }
}
