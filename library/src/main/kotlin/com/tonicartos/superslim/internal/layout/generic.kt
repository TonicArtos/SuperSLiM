package com.tonicartos.superslim.internal.layout

import android.util.Log
import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.internal.SectionState

/**
 * A Slm that does nothing. Used as a Flm or a Hlm for when there is no footer or header.
 */
internal object DoNothingSlm : SectionLayoutManager<SectionState> {
    override fun isAtTop(section: SectionState, layoutState: SectionState.LayoutState) = section.atTop

    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: SectionState.LayoutState) {
        section.layout(helper, section.leftGutter(), 0, helper.layoutWidth - section.rightGutter())
        layoutState.bottom = section.height
        layoutState.headPosition = 0
        layoutState.tailPosition = 0
        Log.d("NO OP SLM", "height = ${section.height}")
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState,
                           layoutState: SectionState.LayoutState): Int {
        val filled = Math.min(dy, section.fillTop(dy, section.leftGutter(), 0,
                                                  helper.layoutWidth - section.rightGutter(), helper))
        layoutState.bottom = section.height
        layoutState.headPosition = 0
        layoutState.tailPosition = 0
        return filled
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState,
                              layoutState: SectionState.LayoutState): Int {
        val filled = section.fillBottom(0, section.leftGutter(), layoutState.bottom,
                                        helper.layoutWidth - section.rightGutter(), helper)
        layoutState.bottom = section.height
        layoutState.headPosition = 0
        layoutState.tailPosition = 0
        return Math.min(dy, filled)
    }

    override fun onTrimTop(scrolled: Int, helper: LayoutHelper, section: SectionState,
                           layoutState: SectionState.LayoutState)
            = section.trimTop(scrolled, helper, 0).also { layoutState.bottom = section.height }

    override fun onTrimBottom(scrolled: Int, helper: LayoutHelper, section: SectionState,
                              layoutState: SectionState.LayoutState)
            = section.trimBottom(scrolled, helper, 0).also { layoutState.bottom = section.height }
}