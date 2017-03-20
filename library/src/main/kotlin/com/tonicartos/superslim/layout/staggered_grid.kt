package com.tonicartos.superslim.layout

import com.tonicartos.superslim.*
import com.tonicartos.superslim.adapter.FooterStyle
import com.tonicartos.superslim.adapter.HeaderStyle
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.SectionState.LayoutState

class StaggeredGridSectionConfig(gutterStart: Int = SectionConfig.DEFAULT_GUTTER,
                                 gutterEnd: Int = SectionConfig.DEFAULT_GUTTER,
                                 @HeaderStyle headerStyle: Int = SectionConfig.DEFAULT_HEADER_STYLE,
                                 @FooterStyle footerStyle: Int = SectionConfig.DEFAULT_FOOTER_STYLE,
                                 paddingStart: Int = 0, paddingTop: Int = 0, paddingEnd: Int = 0,
                                 paddingBottom: Int = 0) :
        SectionConfig(gutterStart, gutterEnd, headerStyle, footerStyle, paddingStart, paddingTop, paddingEnd,
                      paddingBottom),
        ColumnsSectionConfigurationMixin by ColumnsConfiguration() {
    override fun onMakeSection(oldState: SectionState?): SectionState = StaggeredGridSection(this, oldState)

    override fun onCopy(): StaggeredGridSectionConfig {
        val copy = StaggeredGridSectionConfig(gutterStart, gutterEnd, headerStyle, footerStyle)
        copy.numColumns = numColumns
        copy.columnWidth = columnWidth
        return copy
    }
}

private class StaggeredGridSection(var configuration: StaggeredGridSectionConfig, oldState: SectionState? = null) :
        SectionState(configuration, oldState), ColumnsSectionStateMixin by ColumnsState(configuration) {
    override fun isAtTop(layoutState: LayoutState): Boolean {
        return StaggeredGridSlm.isAtTop(this, layoutState)
    }

    override fun doLayout(helper: LayoutHelper, layoutState: LayoutState) {
        resolveColumns(helper)

        StaggeredGridSlm.onLayout(helper, this, layoutState)
    }

    override fun doFillTop(dy: Int, helper: LayoutHelper, layoutState: LayoutState): Int {
        TODO("not implemented")
    }

    override fun doFillBottom(dy: Int, helper: LayoutHelper, layoutState: LayoutState): Int {
        TODO("not implemented")
    }

    override fun doTrimTop(scrolled: Int, helper: LayoutHelper,
                           layoutState: LayoutState): Int {
        TODO("not implemented")
    }

    override fun doTrimBottom(scrolled: Int, helper: LayoutHelper,
                              layoutState: LayoutState): Int {
        TODO("not implemented")
    }
}

private object StaggeredGridSlm : SectionLayoutManager<StaggeredGridSection> {
    override fun isAtTop(section: StaggeredGridSection, layoutState: LayoutState): Boolean {
        TODO("not implemented")
    }

    override fun onLayout(helper: LayoutHelper, section: StaggeredGridSection, layoutState: LayoutState) {
        TODO("not implemented")
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: StaggeredGridSection,
                           layoutState: LayoutState): Int {
        TODO("not implemented")
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: StaggeredGridSection,
                              layoutState: LayoutState): Int {
        TODO("not implemented")
    }

    override fun onTrimTop(scrolled: Int, helper: LayoutHelper,
                           section: StaggeredGridSection,
                           layoutState: LayoutState): Int {
        TODO("not implemented")
    }

    override fun onTrimBottom(scrolled: Int, helper: LayoutHelper,
                              section: StaggeredGridSection,
                              layoutState: LayoutState): Int {
        TODO("not implemented")
    }
}


