package com.tonicartos.superslim.internal.layout

import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionConfig
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.adapter.HeaderStyle
import com.tonicartos.superslim.adapter.FooterStyle
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.SectionState.LayoutState

class FlexboxSectionConfig(gutterStart: Int = SectionConfig.DEFAULT_GUTTER,
                           gutterEnd: Int = SectionConfig.DEFAULT_GUTTER,
                           @HeaderStyle headerStyle: Int = SectionConfig.DEFAULT_HEADER_STYLE,
                           @FooterStyle footerStyle: Int = SectionConfig.DEFAULT_FOOTER_STYLE,
                           paddingStart: Int = 0, paddingTop: Int = 0, paddingEnd: Int = 0, paddingBottom: Int = 0) :
        SectionConfig(gutterStart, gutterEnd, headerStyle, footerStyle, paddingStart, paddingTop, paddingEnd, paddingBottom) {

    override fun onMakeSection(oldState: SectionState?): SectionState = FlexSectionState(this, oldState)

    override fun onCopy(): FlexboxSectionConfig {
        return FlexboxSectionConfig(gutterStart, gutterEnd, headerStyle)
    }
}

internal open class FlexSectionState(val configuration: FlexboxSectionConfig, oldState: SectionState? = null)
    : SectionState(configuration, oldState) {
    override fun doLayout(helper: LayoutHelper, layoutState: LayoutState)
            = FlexboxSlm.onLayout(helper, this, layoutState)

    override fun doFillTop(dy: Int, helper: LayoutHelper, layoutState: LayoutState)
            = FlexboxSlm.onFillTop(dy, helper, this, layoutState)

    override fun doFillBottom(dy: Int, helper: LayoutHelper, layoutState: LayoutState)
            = FlexboxSlm.onFillTop(dy, helper, this, layoutState)

    override fun doTrimTop(helper: LayoutHelper, layoutState: LayoutState)
            = FlexboxSlm.onTrimTop(helper, this, layoutState)

    override fun doTrimBottom(helper: LayoutHelper, layoutState: LayoutState)
            = FlexboxSlm.onTrimBottom(helper, this, layoutState)
}

private object FlexboxSlm : SectionLayoutManager<FlexSectionState> {
    override fun onLayout(helper: LayoutHelper, section: FlexSectionState, layoutState: LayoutState) {
        throw UnsupportedOperationException()
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: FlexSectionState, layoutState: LayoutState): Int {
        throw UnsupportedOperationException()
    }

    /**
     * Fill revealed area where content has been scrolled up the screen by dy.
     */
    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: FlexSectionState,
                              layoutState: LayoutState): Int {
        throw UnsupportedOperationException()
    }

    override fun onTrimTop(helper: LayoutHelper, section: FlexSectionState, layoutState: LayoutState) {
        throw UnsupportedOperationException()
    }

    override fun onTrimBottom(helper: LayoutHelper, section: FlexSectionState, layoutState: LayoutState) {
        throw UnsupportedOperationException()
    }
}
