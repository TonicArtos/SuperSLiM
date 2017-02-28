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
        var y = -state.overdraw
        if (helper.moreToLayout(0, section)) {
            section.layoutContent(helper, section.leftGutter(), y, helper.layoutWidth - section.rightGutter())
            y += section.height
        }
        state.tailPosition = 0
        state.tailPosition = 0
        state.bottom = y
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        val state = layoutState as HeaderLayoutState

        // How much distance left to fill.
        val toFill = dy - state.overdraw

        if (toFill > 0) {
            // Fill space starting at overdraw.
            val y = -state.overdraw
            val filled = section.fillContentTop(toFill, section.leftGutter(), y,
                                                helper.layoutWidth - section.rightGutter(), helper)
            // Update partial state and compute dy filled.
            state.overdraw += filled
        }
        val filled = Math.min(dy, state.overdraw)

        // Update state
        state.overdraw -= filled
        state.bottom += filled
        state.headPosition = 0
        state.tailPosition = 0
        return filled
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        val state = layoutState as HeaderLayoutState

        var filled = (state.bottom - helper.layoutLimit).takeIf { it > 0} ?: 0
        var toFill = dy - filled
        if (toFill > 0) {
            section.fillContentBottom(toFill, section.leftGutter(), state.bottom,
                                      helper.layoutWidth - section.rightGutter(), helper).let {
                toFill -= it
                filled += it
                state.bottom += it
            }
        }

        state.bottom += filled
        state.headPosition = 0
        state.tailPosition = 0
        return Math.min(dy, filled)
    }

    override fun onTrimTop(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
    }

    override fun onTrimBottom(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
    }

}

private object InlineHlm : SectionLayoutManager<SectionState> {

    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        val state = layoutState as HeaderLayoutState
        var y = -state.overdraw
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
            section.layoutContent(helper, section.leftGutter(), y, helper.layoutWidth - section.rightGutter())

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
        var toFill = dy - state.overdraw

        if (toFill > 0 && state.headPosition > 0) {
            if (state.headPosition >= 1) {
                // Fill content
                section.fillContentTop(toFill, section.leftGutter(), -state.overdraw,
                                       helper.layoutWidth - section.rightGutter(), helper).let {
                    state.overdraw += it
                    toFill -= it
                    state.tailPosition = 1
                    state.headPosition = 1
                }

                // Fill header if there is space left.
                if (toFill > 0) {
                    helper.getHeader(section)?.use {
                        addToRecyclerView()
                        measure()
                        fillTop(toFill, 0, -state.overdraw - measuredHeight, measuredWidth, -state.overdraw)
                    }?.let {
                        state.overdraw += it
                        toFill -= it
                        state.state = ADDED
                        state.headPosition = 0
                    }
                }
            }
        }
        val filled = Math.min(dy, state.overdraw)

        // Update state
        state.overdraw -= filled
        state.bottom += filled
        return filled
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        val state = layoutState as HeaderLayoutState

        var filled = (state.bottom - helper.layoutLimit).takeIf { it > 0} ?: 0
        var toFill = dy - filled
        // Add header if room.
        if (toFill > 0 && state.tailPosition == 0) {
            helper.getHeader(section)?.use {
                addToRecyclerView()
                measure()
                fillTop(toFill, 0, state.bottom, measuredWidth, measuredHeight)
            }?.let {
                toFill -= it
                filled += it
                state.bottom += it
                state.state = ADDED
            }
        }

        // Fill content if room.
        if (toFill > 0) {
            /*
             * NOTE: Filled will always be equal to toFill, because fillContent is always tracking it's own overdraw.
             */
            section.fillContentBottom(toFill, section.leftGutter(), state.bottom,
                                      helper.layoutWidth - section.rightGutter(), helper).let {
                toFill -= it
                filled += it
                state.bottom += it
                state.tailPosition = 1
            }
        }

        return Math.min(dy, filled)
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