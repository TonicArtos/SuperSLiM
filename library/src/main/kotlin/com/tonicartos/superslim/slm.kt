package com.tonicartos.superslim

import com.tonicartos.superslim.adapter.Section

interface SectionLayoutManager<T : SectionState> {
    fun onLayout(helper: LayoutHelper, section: T)
    fun fillTopScrolledArea(dy: Int, helper: LayoutHelper, section: T): Int
    fun fillBottomScrolledArea(dy: Int, helper: LayoutHelper, section: T): Int
}

internal object HeaderLayoutManager : SectionLayoutManager<SectionState> {
    override fun onLayout(helper: LayoutHelper, section: SectionState) {
        if (section.hasHeader) {
            selectHeaderLayout(section).onLayout(helper, section)
        } else {
            val left = if (section.baseConfig.gutterLeft == Section.Config.GUTTER_AUTO) 0 else section.baseConfig.gutterLeft
            val right = if (section.baseConfig.gutterRight == Section.Config.GUTTER_AUTO) 0 else section.baseConfig.gutterRight
            section.layoutContent(helper, left, 0, helper.layoutWidth - right)
        }
    }

    override fun fillTopScrolledArea(dy: Int, helper: LayoutHelper, section: SectionState): Int {
        if (section.hasHeader) {
            return selectHeaderLayout(section).fillTopScrolledArea(dy, helper, section)
        }
        return 0
    }

    override fun fillBottomScrolledArea(dy: Int, helper: LayoutHelper, section: SectionState): Int {
        if (section.hasHeader) {
            return selectHeaderLayout(section).fillBottomScrolledArea(dy, helper, section)
        }
        return 0
    }

    private fun selectHeaderLayout(section: SectionState): SectionLayoutManager<SectionState> {
        return when (section.headerStyle) {
            Section.Config.HEADER_EMBEDDED -> EmbeddedHlm
            Section.Config.HEADER_START, Section.Config.HEADER_END -> GutterHlm
            else -> InlineHlm
        }
    }
}

private object EmbeddedHlm : SectionLayoutManager<SectionState> {
    override fun onLayout(helper: LayoutHelper, section: SectionState) {
        // if the current position is the header
        var y = 0
        if (section.headPosition == 0) {
            val child = section.getChildAt(helper, 0)
            child.addToRecyclerView()
            child.measure()
            child.layout(0, 0, child.measuredWidth, child.measuredHeight)
            y += child.height
            child.done()
        }

        val left = if (section.baseConfig.gutterLeft == Section.Config.GUTTER_AUTO) 0 else section.baseConfig.gutterLeft
        val right = if (section.baseConfig.gutterRight == Section.Config.GUTTER_AUTO) 0 else section.baseConfig.gutterRight

        section.layoutContent(helper, left, y, right)

        y += section.height
        section.height = y
    }

    override fun fillTopScrolledArea(dy: Int, helper: LayoutHelper, section: SectionState): Int {
        throw UnsupportedOperationException()
    }

    override fun fillBottomScrolledArea(dy: Int, helper: LayoutHelper, section: SectionState): Int {
        throw UnsupportedOperationException()
    }
}

private object InlineHlm : SectionLayoutManager<SectionState> {
    override fun onLayout(helper: LayoutHelper, section: SectionState) {
        throw UnsupportedOperationException()
    }

    override fun fillTopScrolledArea(dy: Int, helper: LayoutHelper, section: SectionState): Int {
        throw UnsupportedOperationException()
    }

    override fun fillBottomScrolledArea(dy: Int, helper: LayoutHelper, section: SectionState): Int {
        throw UnsupportedOperationException()
    }
}

private object GutterHlm : SectionLayoutManager<SectionState> {
    override fun onLayout(helper: LayoutHelper, section: SectionState) {
        throw UnsupportedOperationException()
    }

    override fun fillTopScrolledArea(dy: Int, helper: LayoutHelper, section: SectionState): Int {
        throw UnsupportedOperationException()
    }

    override fun fillBottomScrolledArea(dy: Int, helper: LayoutHelper, section: SectionState): Int {
        throw UnsupportedOperationException()
    }
}