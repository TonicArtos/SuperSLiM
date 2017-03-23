package com.tonicartos.superslim.internal.layout

import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.internal.SectionState

/**
 * A Slm that does nothing. Used as a Flm or a Hlm for when there is no footer or header.
 */
internal object DoNothingSlm : SectionLayoutManager<SectionState> {
    override fun isAtTop(section: SectionState, layoutState: SectionState.LayoutState) = section.atTop

    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: SectionState.LayoutState) {
        section.layout(helper, section.leftGutter { 0 }, 0, helper.layoutWidth - section.rightGutter { 0 })
        layoutState.disappearedOrRemovedHeight += section.disappearedHeight
        layoutState.bottom = section.height
        layoutState.headPosition = 0
        layoutState.tailPosition = 0
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState,
                           layoutState: SectionState.LayoutState): Int {
        val filled = Math.min(dy, section.fillTop(dy, section.leftGutter { 0 }, 0,
                                                  helper.layoutWidth - section.rightGutter { 0 }, helper))
        layoutState.bottom = section.height
        layoutState.headPosition = 0
        layoutState.tailPosition = 0
        return filled
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState,
                              layoutState: SectionState.LayoutState): Int {
        val filled = section.fillBottom(dy, section.leftGutter { 0 }, layoutState.bottom - section.height,
                                        helper.layoutWidth - section.rightGutter { 0 }, helper)
        layoutState.bottom = section.height
        layoutState.headPosition = 0
        layoutState.tailPosition = 0
        return filled
    }

    override fun onTrimTop(scrolled: Int, helper: LayoutHelper, section: SectionState,
                           layoutState: SectionState.LayoutState)
            = section.trimTop(scrolled, 0, helper).also { layoutState.bottom = section.height }

    override fun onTrimBottom(scrolled: Int, helper: LayoutHelper, section: SectionState,
                              layoutState: SectionState.LayoutState)
            = section.trimBottom(scrolled, 0, helper).also { layoutState.bottom = section.height }
}