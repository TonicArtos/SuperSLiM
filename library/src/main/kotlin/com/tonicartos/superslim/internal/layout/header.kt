package com.tonicartos.superslim.internal.layout

import android.util.Log
import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionConfig
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.internal.SectionState

private const val ABSENT = 1 shl 0
private const val ADDED = 1 shl 1
private const val FLOATING = 1 shl 2

internal object HeaderLayoutManager : SectionLayoutManager<SectionState> {
    override fun onLayout(helper: LayoutHelper, section: SectionState) {
        if (section.hasHeader) {
            selectHeaderLayout(section).onLayout(helper, section)
        } else {
            section.layoutContent(helper, leftGutter(section), 0, helper.layoutWidth - rightGutter(section))
        }
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState): Int {
        if (section.hasHeader) {
            return selectHeaderLayout(section).onFillTop(dy, helper, section)
        }
        return section.fillContentTop(dy, helper, leftGutter(section), helper.layoutWidth - rightGutter(section))
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState): Int {
        if (section.hasHeader) {
            return selectHeaderLayout(section).onFillBottom(dy, helper, section)
        }
        return section.fillContentBottom(dy, helper, leftGutter(section), helper.layoutWidth - rightGutter(section))
    }

    override fun onTrimTop(helper: LayoutHelper, section: SectionState) {
        if (section.hasHeader) {
            return selectHeaderLayout(section).onTrimTop(helper, section)
        }
        return section.trimContentTop(helper, leftGutter(section), 0, helper.layoutWidth - rightGutter(section))
    }

    override fun onTrimBottom(helper: LayoutHelper, section: SectionState) {
        if (section.hasHeader) {
            return selectHeaderLayout(section).onTrimBottom(helper, section)
        }
        return section.trimContentBottom(helper, leftGutter(section), 0, helper.layoutWidth - rightGutter(section))
    }

    private fun selectHeaderLayout(section: SectionState): SectionLayoutManager<SectionState> {
        return when (section.baseConfig.headerStyle) {
            SectionConfig.HEADER_EMBEDDED -> EmbeddedHlm
            SectionConfig.HEADER_START, SectionConfig.HEADER_END -> GutterHlm
            else -> InlineHlm
        }
    }

    private fun rightGutter(section: SectionState) = if (section.baseConfig.gutterRight == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterRight
    private fun leftGutter(section: SectionState) = if (section.baseConfig.gutterLeft == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterLeft
}

private object EmbeddedHlm : SectionLayoutManager<SectionState> {
//    override fun onLayout(helper: LayoutHelper, section: SectionState) {
//        // if the current position is the header
//        var filled = 0
//
//        section.headerLayout.state = ABSENT
//        if (section.headerLayout.headPosition == 0) {
//            helper.getHeader(section)?.apply {
//                addToRecyclerView()
//                measure()
//                layout(0, 0, measuredWidth, measuredHeight)
//                if (helper.isPreLayout && isRemoved) {
//                    helper.addIgnoredHeight(height)
//                }
//                filled += height
//                done()
//                section.headerLayout.state = ADDED
//            }
//        }
//
//        if (filled >= helper.layoutLimit) {
//            section.layout.height = filled
//            section.headerLayout.tailPosition = 0
//            return
//        }
//
//        section.headerLayout.tailPosition = 1
//
//        // The header had to be attached for some views to be correctly measured. Now it must be detached so it can be
//        // correctly placed after the section content.
//        val headerView = if (section.headerLayout.state == ADDED) helper.detachFirstView() else null
//
//        val leftGutter = if (section.baseConfig.gutterLeft == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterLeft
//        val rightGutter = if (section.baseConfig.gutterRight == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterRight
//        section.layoutContent(helper, leftGutter, filled, helper.layoutWidth - rightGutter)
//        section.headerLayout.numViews = section.layout.numViews
//
//        // Put the header after the section content. This is required in other header implementations to get the natural
//        // drawing order correct.
//        headerView?.let {
//            helper.attachViewToEnd(it)
//            section.headerLayout.numViews += 1
//        }
//
//        filled += section.layout.height
//        section.layout.height = filled
//    }
    override fun onLayout(helper: LayoutHelper, section: SectionState) {
        // if the current position is the header
        var y = 0
        if (section.headerLayout.headPosition <= 0) {
            val header = helper.getHeader(section)
            if (header != null) {
                header.addToRecyclerView()
                header.measure()
                header.layout(0, 0, header.measuredWidth, header.measuredHeight)
                if (helper.isPreLayout && header.isRemoved) {
                    helper.addIgnoredHeight(header.height)
                }
                y += header.height
                header.done()
            }
        }

        val left = if (section.baseConfig.gutterLeft == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterLeft
        val right = if (section.baseConfig.gutterRight == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterRight

        section.layoutContent(helper, left, y, helper.layoutWidth - right)

        y += section.layout.height
        section.layout.height = y
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState): Int {
        Log.d("HLM", "section = $section")
        var y = section.headerLayout.overdraw
        Log.d("HLM", "overdraw = $y")

        if (section.headerLayout.headPosition == 0) {
            if (y < 0) {
                section.headerLayout.overdraw = y - dy
                return -y
            }
            return 0
        }

        val leftGutter = if (section.baseConfig.gutterLeft == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterLeft
        val rightGutter = if (section.baseConfig.gutterRight == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterRight
        y = -section.fillContentTop(dy, helper, leftGutter, helper.layoutWidth - rightGutter)
        Log.d("HLM", "fillContentTop:: y = $y")

        if (y > dy) {
            // Not enough filled so add header.
            val headerBottom = y
            val headerHeight = helper.getHeader(section)?.let {
                it.addToRecyclerView()
                it.measure()
                it.layout(0, headerBottom - it.measuredHeight, it.measuredWidth, headerBottom)
                it.done()

                section.headerLayout.state = ADDED
                section.headerLayout.numViews += 1
                section.headerLayout.headPosition = 0

                return@let it.measuredHeight
            } ?: 0

            section.layout.height += headerHeight
            y -= headerHeight
        }

        section.headerLayout.overdraw = y - dy
        return -y
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState): Int {
        if (section.headerLayout.headPosition == 0) {
            val bottom = helper.getBottom(helper.getView(section.headerLayout.numViews - 1))
        }
        throw UnsupportedOperationException()
    }

    override fun onTrimTop(helper: LayoutHelper, section: SectionState) {
        throw UnsupportedOperationException()
    }

    override fun onTrimBottom(helper: LayoutHelper, section: SectionState) {
        throw UnsupportedOperationException()
    }
}

private object InlineHlm : SectionLayoutManager<SectionState> {
    override fun onLayout(helper: LayoutHelper, section: SectionState) {
        throw UnsupportedOperationException()
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState): Int {
        throw UnsupportedOperationException()
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState): Int {
        throw UnsupportedOperationException()
    }

    override fun onTrimTop(helper: LayoutHelper, section: SectionState) {
        throw UnsupportedOperationException()
    }

    override fun onTrimBottom(helper: LayoutHelper, section: SectionState) {
        throw UnsupportedOperationException()
    }
}

private object GutterHlm : SectionLayoutManager<SectionState> {
    override fun onLayout(helper: LayoutHelper, section: SectionState) {
        throw UnsupportedOperationException()
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState): Int {
        throw UnsupportedOperationException()
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState): Int {
        throw UnsupportedOperationException()
    }

    override fun onTrimTop(helper: LayoutHelper, section: SectionState) {
        throw UnsupportedOperationException()
    }

    override fun onTrimBottom(helper: LayoutHelper, section: SectionState) {
        throw UnsupportedOperationException()
    }
}