package com.tonicartos.superslim.internal.layout

import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionConfig
import com.tonicartos.superslim.SectionConfig.Companion.GUTTER_AUTO
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.SectionState.HeaderLayoutState
import com.tonicartos.superslim.internal.SectionState.LayoutState
import com.tonicartos.superslim.use

private const val ABSENT = 1 shl 0
private const val ADDED = 1 shl 1
private const val FLOATING = 1 shl 2

@Suppress("nothing_to_inline")
private inline fun rightGutter(section: SectionState, autoWidth: Int = 0)
        = if (section.baseConfig.gutterRight == GUTTER_AUTO) autoWidth else section.baseConfig.gutterRight

@Suppress("nothing_to_inline")
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
        val toFill = dy + padding - state.overdrawTop

        if (toFill > 0) {
            // Fill space starting at overdrawTop.
            val y = state.top
            val filledContent = section.fillContentTop(toFill, leftGutter(section), y,
                                                       helper.layoutWidth - rightGutter(section), helper)

            // Determine if actual filled area includes padding or not.
            val atTopOfContent = filledContent < toFill
            val paddingAlreadyAdded = filledContent == 0 && state.overdrawTop >= padding
            val actualFilled = filledContent + (padding.takeIf { atTopOfContent && paddingAlreadyAdded } ?: 0)

            // Update partial state and compute dy filled.
            state.top -= filledContent
            state.overdrawTop += actualFilled
        }
        var filled = Math.min(dy, state.overdrawTop)

        // Advance top by the filled area which will be scrolled. However, prevent scrolling past the top.
        state.top += filled
        if (state.top > 0) {
            filled -= state.top
            state.top = 0
        }

        // Update state
        state.overdrawTop -= filled
        state.bottom += filled
        state.headPosition = 0
        state.tailPosition = 0
        return filled
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        val state = layoutState as HeaderLayoutState

        var toFill = dy - state.overdrawBottom
        if (toFill > 0 && state.headPosition < 0) {
            val filled = helper.paddingTop
            state.overdrawBottom += filled
            toFill -= filled
        }

        if (toFill > 0) {
            val filled = section.fillContentBottom(toFill, leftGutter(section), state.bottom + state.overdrawBottom,
                                                   helper.layoutWidth - rightGutter(section), helper)
            state.overdrawBottom += filled
            toFill -= filled
        }
        val filled = Math.min(dy, state.overdrawBottom)

        state.overdrawBottom -= filled
        state.bottom += filled
        state.headPosition = 0
        state.tailPosition = 0
        return filled
    }

    override fun onTrimTop(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
    }

    override fun onTrimBottom(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
    }

}

private object InlineHlm : SectionLayoutManager<SectionState> {

    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        val state = layoutState as HeaderLayoutState
        var y = state.top
        if (state.headPosition == 0) {
            helper.getHeader(section)?.use {
                addToRecyclerView()
                measure()
                layout(0, y, measuredWidth, y + measuredHeight)
                if (helper.isPreLayout && isRemoved) {
                    helper.addIgnoredHeight(height)
                }
                y += height
                state.state = ADDED
            }
        } else {
            state.state = ABSENT
        }

        if (helper.moreToLayout(0, section)) {
            section.layoutContent(helper, leftGutter(section), y, helper.layoutWidth - rightGutter(section))

            y += section.height
            state.tailPosition = 1
        } else {
            state.tailPosition = 0
        }

        state.bottom = y
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        val state = layoutState as HeaderLayoutState

        // How much distance left to fill.
        val padding = helper.paddingTop
        var toFill = dy + padding - state.overdrawTop

        if (toFill > 0 && state.headPosition > 0) {
            var currentPos = state.headPosition
            if (currentPos == 2) currentPos -= 1
            if (currentPos == 1) {
                // Fill content
                var filled = section.fillContentTop(toFill, leftGutter(section), state.top,
                                                    helper.layoutWidth - rightGutter(section), helper)
                state.top -= filled
                state.tailPosition = 1

                // Fill header and add padding if there is space left.
                if (filled < toFill) {
                    currentPos -= 1
                    helper.getHeader(section)?.use {
                        addToRecyclerView()
                        measure()
                        val filledHeader = fillTop(toFill, 0, state.top - measuredHeight, measuredWidth, state.top)
                        state.top -= filledHeader
                        filled += filledHeader + padding
                        state.state = ADDED
                    }
                }
                state.overdrawTop += filled
            }
            state.headPosition = currentPos
        }
        var filled = Math.min(dy, state.overdrawTop)

        // Scroll top
        state.top += filled
        if (state.top > 0) {
            filled -= state.top
            state.top = 0
        }

        // Update state
        state.overdrawTop -= filled
        state.bottom += filled
        return filled
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        val state = layoutState as HeaderLayoutState

        var toFill = dy - state.overdrawBottom

        // Add padding if not laid out yet.
        if (toFill > 0 && state.headPosition < 0) {
            val filled = helper.paddingTop
            state.overdrawBottom += filled
            toFill -= filled
            state.headPosition = 0
            state.tailPosition = 0
        }

        // Add header if room.
        if (toFill > 0 && state.tailPosition == 0) {
            helper.getHeader(section)?.use {
                addToRecyclerView()
                measure()
                val filled = fillTop(toFill, 0, state.bottom + state.overdrawBottom, measuredWidth, measuredHeight)
                state.overdrawBottom += filled
                toFill -= filled
                state.state = ADDED
            }
        }

        // Fill content if room.
        if (toFill > 0) {
            /*
             * NOTE: Filled will always be equal to toFill, because fillContent is always tracking it's own overdraw.
             */
            val filled = section.fillContentBottom(toFill, leftGutter(section),
                                                   state.bottom + state.overdrawBottom,
                                                   helper.layoutWidth - rightGutter(section),
                                                   helper)
            state.overdrawBottom += filled
            toFill -= filled
            state.tailPosition = 1
        }

        val filled = Math.min(dy, state.overdrawBottom)

        state.top -= filled

        state.overdrawBottom -= filled

        return filled
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