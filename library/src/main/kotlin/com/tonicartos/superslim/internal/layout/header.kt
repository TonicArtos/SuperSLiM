package com.tonicartos.superslim.internal.layout

import android.util.Log
import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionConfig
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.internal.SectionState

internal object HeaderLayoutManager : SectionLayoutManager<SectionState> {
    override fun onLayout(helper: LayoutHelper, section: SectionState) {
        if (section.hasHeader) {
            selectHeaderLayout(section).onLayout(helper, section)
        } else {
            val left = if (section.baseConfig.gutterLeft == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterLeft
            val right = if (section.baseConfig.gutterRight == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterRight
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
        return when (section.baseConfig.headerStyle) {
            SectionConfig.HEADER_EMBEDDED                        -> EmbeddedHlm
            SectionConfig.HEADER_START, SectionConfig.HEADER_END -> GutterHlm
            else                                                 -> InlineHlm
        }
    }
}

private object EmbeddedHlm : SectionLayoutManager<SectionState> {
    override fun onLayout(helper: LayoutHelper, section: SectionState) {
        // if the current position is the header
        var y = 0
        if (section.headPosition == 0) {
            val header = helper.getHeader(section)
            if (header != null) {
                header.addToRecyclerView()
                header.measure()
                header.layout(0, 0, header.measuredWidth, header.measuredHeight)
                y += header.height
                header.done()
            }
        }

        val left = if (section.baseConfig.gutterLeft == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterLeft
        val right = if (section.baseConfig.gutterRight == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterRight

        section.layoutContent(helper, left, y, helper.layoutWidth - right)

        y += section.height
        Log.d("", "y = $y, section height = ${section.height}")
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