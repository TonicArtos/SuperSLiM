package com.tonicartos.superslim.layout

import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionConfig
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.adapter.FooterStyle
import com.tonicartos.superslim.adapter.HeaderStyle
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.SectionState.LayoutState

class FlexboxSectionConfig(gutterStart: Int = SectionConfig.DEFAULT_GUTTER,
                           gutterEnd: Int = SectionConfig.DEFAULT_GUTTER,
                           @HeaderStyle headerStyle: Int = SectionConfig.DEFAULT_HEADER_STYLE,
                           @FooterStyle footerStyle: Int = SectionConfig.DEFAULT_FOOTER_STYLE,
                           paddingStart: Int = 0, paddingTop: Int = 0, paddingEnd: Int = 0, paddingBottom: Int = 0) :
        SectionConfig(gutterStart, gutterEnd, headerStyle, footerStyle, paddingStart, paddingTop, paddingEnd,
                      paddingBottom) {

    override fun onMakeSection(oldState: SectionState?): SectionState = FlexboxSectionState(this, oldState)

    override fun onCopy(): FlexboxSectionConfig {
        return FlexboxSectionConfig(gutterStart, gutterEnd, headerStyle, footerStyle)
    }
}

private class FlexboxSectionState(configuration: FlexboxSectionConfig, oldState: SectionState? = null)
    : SectionState(configuration, oldState) {
    override fun isAtTop(layoutState: LayoutState) = FlexboxSlm.isAtTop(this, layoutState)

    override fun doLayout(helper: LayoutHelper, layoutState: LayoutState)
            = FlexboxSlm.onLayout(helper, this, layoutState)

    override fun doFillTop(dy: Int, helper: LayoutHelper, layoutState: LayoutState)
            = FlexboxSlm.onFillTop(dy, helper, this, layoutState)

    override fun doFillBottom(dy: Int, helper: LayoutHelper, layoutState: LayoutState)
            = FlexboxSlm.onFillTop(dy, helper, this, layoutState)

    override fun doTrimTop(scrolled: Int, helper: LayoutHelper, layoutState: LayoutState): Int
            = FlexboxSlm.onTrimTop(scrolled, helper, this, layoutState)

    override fun doTrimBottom(scrolled: Int, helper: LayoutHelper, layoutState: LayoutState): Int
            = FlexboxSlm.onTrimBottom(scrolled, helper, this, layoutState)
}

private object FlexboxSlm : SectionLayoutManager<FlexboxSectionState> {
    override fun isAtTop(section: FlexboxSectionState, layoutState: LayoutState): Boolean {
        TODO("not implemented")
    }

    override fun onLayout(helper: LayoutHelper, section: FlexboxSectionState, layoutState: LayoutState) {
        TODO("not implemented")
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: FlexboxSectionState, layoutState: LayoutState): Int {
        TODO("not implemented")
    }

    /**
     * Fill revealed area where content has been scrolled up the screen by dy.
     */
    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: FlexboxSectionState,
                              layoutState: LayoutState): Int {
        TODO("not implemented")
    }

    override fun onTrimTop(scrolled: Int, helper: LayoutHelper,
                           section: FlexboxSectionState,
                           layoutState: LayoutState): Int {
        TODO("not implemented")
    }

    override fun onTrimBottom(scrolled: Int, helper: LayoutHelper,
                              section: FlexboxSectionState,
                              layoutState: LayoutState): Int {
        TODO("not implemented")
    }
}
