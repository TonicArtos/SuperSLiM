package com.tonicartos.superslim.slm

import com.tonicartos.superslim.*
import com.tonicartos.superslim.adapter.HeaderStyle
import com.tonicartos.superslim.adapter.Section

class GridSectionConfig(gutterStart: Int = Section.Config.DEFAULT_GUTTER, gutterEnd: Int = Section.Config.DEFAULT_GUTTER,
                        @HeaderStyle headerStyle: Int = Section.Config.DEFAULT_HEADER_STYLE) : Section.Config(gutterStart, gutterEnd, headerStyle),
        ColumnsSectionConfigurationMixin by ColumnsConfiguration() {

    override fun onMakeSection(oldState: SectionState?): SectionState = GridSectionState(this, oldState)
}

class GridSectionState(var configuration: GridSectionConfig, oldState: SectionState? = null) : SectionState(configuration, oldState),
        ColumnsSectionStateMixin by ColumnsState(configuration) {
    override fun doLayout(helper: LayoutHelper) {
        resolveColumns(helper)

        GridSlm.onLayout(helper, this)
    }
}

private object GridSlm : SectionLayoutManager<GridSectionState> {
    override fun onLayout(helper: LayoutHelper, section: GridSectionState) {
        throw UnsupportedOperationException()
    }

    override fun fillTopScrolledArea(dy: Int, helper: LayoutHelper, section: GridSectionState): Int {
        throw UnsupportedOperationException()
    }

    override fun fillBottomScrolledArea(dy: Int, helper: LayoutHelper, section: GridSectionState): Int {
        throw UnsupportedOperationException()
    }
}

