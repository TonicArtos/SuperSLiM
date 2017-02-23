package com.tonicartos.superslim.internal.layout

import com.tonicartos.superslim.BuildConfig
import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionConfig
import com.tonicartos.superslim.SectionConfig.Companion.GUTTER_AUTO
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.SectionState.HeaderLayoutState
import com.tonicartos.superslim.internal.SectionState.LayoutState

private const val ABSENT = 1 shl 0
private const val ADDED = 1 shl 1
private const val FLOATING = 1 shl 2

private inline fun rightGutter(section: SectionState, autoWidth: Int = 0)
        = if (section.baseConfig.gutterRight == GUTTER_AUTO) autoWidth else section.baseConfig.gutterRight

private inline fun leftGutter(section: SectionState, autoWidth: Int = 0)
        = if (section.baseConfig.gutterLeft == GUTTER_AUTO) autoWidth else section.baseConfig.gutterLeft

internal object HeaderLayoutManager : SectionLayoutManager<SectionState> {
    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState)
            = selectHeaderLayout(section).onLayout(helper, section, layoutState)

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState)
            = selectHeaderLayout(section).onFillTop(dy, helper, section, layoutState)

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState)
            = selectHeaderLayout(section).onFillBottom(dy, helper, section, layoutState)

    override fun onTrimTop(helper: LayoutHelper, section: SectionState, layoutState: LayoutState)
            = selectHeaderLayout(section).onTrimTop(helper, section, layoutState)

    override fun onTrimBottom(helper: LayoutHelper, section: SectionState, layoutState: LayoutState)
            = selectHeaderLayout(section).onTrimBottom(helper, section, layoutState)

    private fun selectHeaderLayout(section: SectionState): SectionLayoutManager<SectionState> {
        return NoHeaderHlm.takeUnless { section.hasHeader } ?: when (section.baseConfig.headerStyle) {
            SectionConfig.HEADER_INLINE -> InlineHlm
            SectionConfig.HEADER_START, SectionConfig.HEADER_END -> GutterHlm
            else -> StickyHlm
        }
    }
}

/**
 * A Hlm that handles layout when there is no header.
 */
private object NoHeaderHlm : SectionLayoutManager<SectionState> {
    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        val state = layoutState as HeaderLayoutState
        var y = state.top
        if (helper.moreToLayout(0, section)) {
            section.layoutContent(helper, leftGutter(section), y, helper.layoutWidth - rightGutter(section))
            y += section.height
        }
        state.tailPosition = 0
        state.tailPosition = 0
        state.bottom = y + helper.paddingBottom
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        val state = layoutState as HeaderLayoutState

        // How much distance left to fill.
        val padding = helper.paddingTop
        val dyRemaining = dy + padding - state.overdraw

        var filled: Int
        if (dyRemaining <= 0) {
            filled = Math.min(dy, state.overdraw)
        } else {
            // Fill space starting at overdraw.
            val slmY = state.top
            val filledContent = section.fillContentTop(dyRemaining, leftGutter(section), slmY,
                                                       helper.layoutWidth - rightGutter(section), helper)

            // Determine if actual filled area includes padding or not.
            val atTopOfContent = filledContent < dyRemaining
            val paddingAlreadyAdded = filledContent == 0 && state.overdraw >= padding
            val actualFilled = filledContent + (padding.takeIf { atTopOfContent && paddingAlreadyAdded } ?: 0)

            // Update partial state and compute dy filled.
            state.top -= filledContent
            state.overdraw += actualFilled
            filled = Math.min(dy, state.overdraw)
        }

        // Advance top by the filled area which will be scrolled. However, prevent scrolling past the top.
        state.top += filled
        if (state.top > 0) {
            filled -= state.top
            state.top = 0
        }

        // Update state
        state.overdraw -= filled
        state.bottom += filled
        state.headPosition = 0
        state.tailPosition = 0
        return filled
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        return 0
    }

    override fun onTrimTop(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
    }

    override fun onTrimBottom(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
    }

}

private object InlineHlm : SectionLayoutManager<SectionState> {
    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        var y = -layoutState.top - layoutState.overdraw

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
            val leftGutter = if (section.baseConfig.gutterLeft == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterLeft
            val rightGutter = if (section.baseConfig.gutterRight == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterRight
            section.layoutContent(helper, leftGutter, y, helper.layoutWidth - rightGutter)

            y += section.height
            state.tailPosition = 1
        } else {
            state.tailPosition = 0
        }

        state.bottom = y
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        Log.d("HeaderHlm", "section = $section, headPosition = ${layoutState.headPosition}, overdraw = ${layoutState.overdraw}")
        val state = layoutState as HeaderLayoutState

        // How much distance left to fill.
        var dyRemaining = dy - state.overdraw
        if (dyRemaining <= 0) {
            state.overdraw -= dy
            Log.d("InlineHlm", "Nothing todo: dyRemaining = $dyRemaining, overdraw = ${state.overdraw}")
            return dy
        }

        // Where we are filling at.
        var y = -state.top - state.overdraw
        Log.d("InlineHlm", "Before: y = $y, dyRemaining = $dyRemaining")

        var currentPos = state.headPosition
        if (currentPos == 2) currentPos -= 1
        if (currentPos == 1) {
            // Fill content
            val leftGutter = if (section.baseConfig.gutterLeft == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterLeft
            val rightGutter = if (section.baseConfig.gutterRight == SectionConfig.GUTTER_AUTO) 0 else section.baseConfig.gutterRight
            val contentFilled = section.fillContentTop(dyRemaining, leftGutter, y, helper.layoutWidth - rightGutter, helper)
            y -= contentFilled
            dyRemaining -= contentFilled
            Log.d("InlineHlm", "After content: filled = $contentFilled, y = $y, dyRemaining = $dyRemaining")

            if (dyRemaining > 0) {
                currentPos -= 1
                helper.getHeader(section)?.apply {
                    addToRecyclerView()
                    measure()
                    val headerFilled = fillTop(dyRemaining, 0, y - measuredHeight, measuredWidth, y)
                    Log.d("InlineHlm", "After header: filled = $headerFilled, height = $height, width = $width")
                    y -= headerFilled
                    dyRemaining -= headerFilled
                    done()
                    state.state = ADDED
                }
            }
        }

        Log.d("InlineHlm", "After inline: dyRemaining = $dyRemaining")
        val filled = Math.min(dy, dy - dyRemaining) // Cap filled distance at dy. Any left over is overdraw.
        state.overdraw = Math.max(0, -dyRemaining) // If dyRemaining is -ve, then overdraw happened.
        state.bottom += filled // Section got taller by the filled amount.
        state.headPosition = currentPos
        Log.d("InlineHlm", "filled = $filled, y = $y, dyRemaining = $dyRemaining, overdraw = ${state.overdraw}")
        return filled
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
    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
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
    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
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