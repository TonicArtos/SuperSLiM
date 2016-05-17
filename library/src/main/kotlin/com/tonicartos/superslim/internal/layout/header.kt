package com.tonicartos.superslim.internal.layout

import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionConfig
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.SectionState.HeaderLayoutState
import com.tonicartos.superslim.internal.SectionState.LayoutState

private const val ABSENT = 1 shl 0
private const val ADDED = 1 shl 1
private const val FLOATING = 1 shl 2

internal object HeaderLayoutManager : SectionLayoutManager<SectionState> {
    override fun onLayout(helper: LayoutHelper, section: SectionState, state: LayoutState) {
        if (section.hasHeader) {
            selectHeaderLayout(section).onLayout(helper, section, state)
        } else {
            section.layoutContent(helper, leftGutter(section), 0, helper.layoutWidth - rightGutter(section))
            state.bottom = section.height
        }
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        if (section.hasHeader) {
            return selectHeaderLayout(section).onFillTop(dy, helper, section, layoutState)
        }
        return section.fillContentTop(dy, helper, leftGutter(section), helper.layoutWidth - rightGutter(section))
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        if (section.hasHeader) {
            return selectHeaderLayout(section).onFillBottom(dy, helper, section, layoutState)
        }
        return section.fillContentBottom(dy, helper, leftGutter(section), helper.layoutWidth - rightGutter(section))
    }

    override fun onTrimTop(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        if (section.hasHeader) {
            return selectHeaderLayout(section).onTrimTop(helper, section, layoutState)
        }
        return section.trimContentTop(helper, leftGutter(section), 0, helper.layoutWidth - rightGutter(section))
    }

    override fun onTrimBottom(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        if (section.hasHeader) {
            return selectHeaderLayout(section).onTrimBottom(helper, section, layoutState)
        }
        return section.trimContentBottom(helper, leftGutter(section), 0, helper.layoutWidth - rightGutter(section))
    }

    private fun selectHeaderLayout(section: SectionState): SectionLayoutManager<SectionState> {
        return when (section.baseConfig.headerStyle) {
            SectionConfig.HEADER_INLINE -> InlineHlm
            SectionConfig.HEADER_START, SectionConfig.HEADER_END -> GutterHlm
            else -> StickyHlm
        }
    }

    private fun rightGutter(section: SectionState) = if (section.baseConfig.gutterRight == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterRight
    private fun leftGutter(section: SectionState) = if (section.baseConfig.gutterLeft == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterLeft
}

private object InlineHlm : SectionLayoutManager<SectionState> {
    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        // if the current position is the header
        var y = 0

        val state = layoutState as HeaderLayoutState
        if (state.headPosition == 0) {
            helper.getHeader(section)?.apply {
                addToRecyclerView()
                measure()
                layout(0, 0, measuredWidth, measuredHeight)
                if (helper.isPreLayout && isRemoved) {
                    helper.addIgnoredHeight(height)
                }
                y += height
                done()
                state.state = ADDED
            }
        } else {
            state.state = ABSENT
        }

        if (helper.moreToLayout(0, section)) {
            // The header had to be attached for some views to be correctly measured. Now it must be detached so it can be
            // correctly placed after the section content.
            // TODO: fix whatever this is :D
            //            val headerView = if (state.state == ADDED) helper.detachFirstView() else null

            val leftGutter = if (section.baseConfig.gutterLeft == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterLeft
            val rightGutter = if (section.baseConfig.gutterRight == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterRight
            section.layoutContent(helper, leftGutter, y, helper.layoutWidth - rightGutter)
            state.numViews = section.numViews

            // Put the header after the section content. This is required in other header implementations to get the natural
            // drawing order correct.
            //            headerView?.let {
            //                helper.attachViewToEnd(it)
            //                state.numViews += 1
            //            }

            y += section.height
            state.tailPosition = 1
        } else {
            state.tailPosition = 0
        }

        state.bottom = y
    }

    //    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
    //        Log.d("header", "Laying out section ${section.positionInAdapter} with height limit ${helper.layoutLimit}")
    //        // if the current position is the header
    //        var y = 0
    //        if (layoutState.headPosition <= 0) {
    //            val header = helper.getHeader(section)
    //            if (header != null) {
    //                header.addToRecyclerView()
    //                header.measure()
    //                header.layout(0, 0, header.measuredWidth, header.measuredHeight)
    //                if (helper.isPreLayout && header.isRemoved) {
    //                    helper.addIgnoredHeight(header.height)
    //                }
    //                y += header.height
    //                header.done()
    //            }
    //        }
    //
    //        val left = if (section.baseConfig.gutterLeft == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterLeft
    //        val right = if (section.baseConfig.gutterRight == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterRight
    //
    //        section.layoutContent(helper, left, y, helper.layoutWidth - right)
    //        Log.d("linear", "Laid out content with height ${section.height}")
    //
    //        layoutState.bottom = y + section.height
    //    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        //        var y = section.headerLayout.overdraw
        //
        //        if (section.headerLayout.headPosition == 0) {
        //            if (y < 0) {
        //                section.headerLayout.overdraw = y - dy
        //                return -y
        //            }
        //            return 0
        //        }
        //
        //        val leftGutter = if (section.baseConfig.gutterLeft == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterLeft
        //        val rightGutter = if (section.baseConfig.gutterRight == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterRight
        //        y = -section.fillContentTop(dy, helper, leftGutter, helper.layoutWidth - rightGutter)
        //
        //        if (y > dy) {
        //            // Not enough filled so add header.
        //            val headerBottom = y
        //            val headerHeight = helper.getHeader(section)?.let {
        //                it.addToRecyclerView()
        //                it.measure()
        //                it.layout(0, headerBottom - it.measuredHeight, it.measuredWidth, headerBottom)
        //                it.done()
        //
        //                section.headerLayout.state = ADDED
        //                section.headerLayout.numViews += 1
        //                section.headerLayout.headPosition = 0
        //
        //                return@let it.measuredHeight
        //            } ?: 0
        //
        //            section.layout.height += headerHeight
        //            y -= headerHeight
        //        }
        //
        //        section.headerLayout.overdraw = y - dy
        //        return -y
        throw UnsupportedOperationException()
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        //        if (section.headerLayout.headPosition == 0) {
        //            val bottom = helper.getBottom(helper.getView(section.headerLayout.numViews - 1))
        //        }
        throw UnsupportedOperationException()
    }

    override fun onTrimTop(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        throw UnsupportedOperationException()
    }

    override fun onTrimBottom(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        throw UnsupportedOperationException()
    }
}

private object StickyHlm : SectionLayoutManager<SectionState> {
    override fun onLayout(helper: LayoutHelper, section: SectionState, state: LayoutState) {
        throw UnsupportedOperationException()
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        throw UnsupportedOperationException()
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        throw UnsupportedOperationException()
    }

    override fun onTrimTop(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        throw UnsupportedOperationException()
    }

    override fun onTrimBottom(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        throw UnsupportedOperationException()
    }
}

private object GutterHlm : SectionLayoutManager<SectionState> {
    override fun onLayout(helper: LayoutHelper, section: SectionState, state: LayoutState) {
        throw UnsupportedOperationException()
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        throw UnsupportedOperationException()
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        throw UnsupportedOperationException()
    }

    override fun onTrimTop(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        throw UnsupportedOperationException()
    }

    override fun onTrimBottom(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        throw UnsupportedOperationException()
    }
}