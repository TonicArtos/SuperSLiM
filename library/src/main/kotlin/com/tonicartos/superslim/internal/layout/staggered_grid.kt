package com.tonicartos.superslim.internal.layout

import com.tonicartos.superslim.*
import com.tonicartos.superslim.adapter.HeaderStyle
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.SectionState.LayoutState

class StaggeredGridSectionConfig(gutterStart: Int = SectionConfig.DEFAULT_GUTTER, gutterEnd: Int = SectionConfig.DEFAULT_GUTTER,
                                 @HeaderStyle headerStyle: Int = SectionConfig.DEFAULT_HEADER_STYLE,
                                 paddingStart: Int = 0, paddingHead: Int = 0, paddingEnd: Int = 0, paddingTail: Int = 0) :
        SectionConfig(gutterStart, gutterEnd, headerStyle, paddingStart, paddingHead, paddingEnd, paddingTail),
        ColumnsSectionConfigurationMixin by ColumnsConfiguration() {
    override fun onMakeSection(oldState: SectionState?): SectionState = StaggeredGridSection(this, oldState)

    override fun onCopy(): StaggeredGridSectionConfig {
        val copy = StaggeredGridSectionConfig(gutterStart, gutterEnd, headerStyle)
        copy.numColumns = numColumns
        copy.columnWidth = columnWidth
        return copy
    }
}

class StaggeredGridSection(var configuration: StaggeredGridSectionConfig, oldState: SectionState? = null) : SectionState(configuration, oldState),
                                                                                                            ColumnsSectionStateMixin by ColumnsState(configuration) {
    override fun doLayout(helper: LayoutHelper, layoutState: LayoutState) {
        resolveColumns(helper)

        StaggeredGridSlm.onLayout(helper, this, layoutState)
    }

    override fun doFillTop(dy: Int, helper: LayoutHelper, layoutState: LayoutState): Int {
        throw UnsupportedOperationException()
    }

    override fun doFillBottom(dy: Int, helper: LayoutHelper, layoutState: LayoutState): Int {
        throw UnsupportedOperationException()
    }

    override fun doTrimTop(helper: LayoutHelper, layoutState: LayoutState) {
        throw UnsupportedOperationException()
    }

    override fun doTrimBottom(helper: LayoutHelper, layoutState: LayoutState) {
        throw UnsupportedOperationException()
    }
}

private object StaggeredGridSlm : SectionLayoutManager<StaggeredGridSection> {
    override fun onLayout(helper: LayoutHelper, section: StaggeredGridSection, layoutState: LayoutState) {
        throw UnsupportedOperationException()
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: StaggeredGridSection, layoutState: LayoutState): Int {
        throw UnsupportedOperationException()
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: StaggeredGridSection, layoutState: LayoutState): Int {
        throw UnsupportedOperationException()
    }

    override fun onTrimTop(helper: LayoutHelper, section: StaggeredGridSection, layoutState: LayoutState) {
        throw UnsupportedOperationException()
    }

    override fun onTrimBottom(helper: LayoutHelper, section: StaggeredGridSection, layoutState: LayoutState) {
        throw UnsupportedOperationException()
    }
}


