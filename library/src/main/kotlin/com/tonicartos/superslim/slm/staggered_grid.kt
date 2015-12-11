package com.tonicartos.superslim.slm

import com.tonicartos.superslim.*
import com.tonicartos.superslim.adapter.HeaderStyle
import com.tonicartos.superslim.adapter.Section

class StaggeredGridSectionConfig(gutterStart: Int = Section.Config.DEFAULT_GUTTER, gutterEnd: Int = Section.Config.DEFAULT_GUTTER,
                                 @HeaderStyle headerStyle: Int = Section.Config.DEFAULT_HEADER_STYLE) : Section.Config(gutterStart, gutterEnd, headerStyle),
        ColumnsSectionConfigurationMixin by ColumnsConfiguration() {

    override fun onMakeSection(oldState: SectionState?): SectionState = StaggeredGridSection(this, oldState)
}

class StaggeredGridSection(var configuration: StaggeredGridSectionConfig, oldState: SectionState? = null) : SectionState(configuration, oldState),
        ColumnsSectionStateMixin by ColumnsState(configuration) {
    override fun doLayout(helper: LayoutHelper) {
        resolveColumns(helper)

        StaggeredGridSlm.onLayout(helper, this)
    }
}

private object StaggeredGridSlm : SectionLayoutManager<StaggeredGridSection> {
    override fun onLayout(helper: LayoutHelper, section: StaggeredGridSection) {
        throw UnsupportedOperationException()
    }

    override fun fillTopScrolledArea(dy: Int, helper: LayoutHelper, section: StaggeredGridSection): Int {
        throw UnsupportedOperationException()
    }

    override fun fillBottomScrolledArea(dy: Int, helper: LayoutHelper, section: StaggeredGridSection): Int {
        throw UnsupportedOperationException()
    }
}


