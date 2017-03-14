package com.tonicartos.superslim.internal.layout

import android.util.Log
import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionConfig
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.SectionState.FooterLayoutState
import com.tonicartos.superslim.internal.SectionState.LayoutState
import com.tonicartos.superslim.use

private const val ABSENT = 1 shl 0
private const val ADDED = 1 shl 1
private const val FLOATING = 1 shl 2

internal object FooterLayoutManager : SectionLayoutManager<SectionState> {
    override fun isAtTop(section: SectionState, layoutState: LayoutState) = selectFooterLayout((section)).isAtTop(
            section, layoutState)

    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState)
            = selectFooterLayout(section).onLayout(helper, section, layoutState)

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState)
            = selectFooterLayout(section).onFillTop(dy, helper, section, layoutState)

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState)
            = selectFooterLayout(section).onFillBottom(dy, helper, section, layoutState)

    override fun onTrimTop(scrolled: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState)
            = selectFooterLayout(section).onTrimTop(scrolled, helper, section, layoutState)

    override fun onTrimBottom(scrolled: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState)
            = selectFooterLayout(section).onTrimBottom(scrolled, helper, section, layoutState)

    private fun selectFooterLayout(section: SectionState): SectionLayoutManager<SectionState> {
        return DoNothingSlm.takeUnless { section.hasFooter } ?: when (section.baseConfig.headerStyle) {
            SectionConfig.HEADER_INLINE                          -> InlineFlm
            SectionConfig.HEADER_START, SectionConfig.HEADER_END -> GutterFlm
            else                                                 -> StickyFlm
        }
    }
}

private interface BaseFlm : SectionLayoutManager<SectionState> {
    override fun isAtTop(section: SectionState, layoutState: LayoutState) = section.atTop
}

private object InlineFlm : BaseFlm {
    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        Log.d("INLINE FLM", "layout")
        val state = layoutState as FooterLayoutState
        var y = -state.overdraw

        if (state.headPosition <= 0) {
            if (helper.moreToLayout(0, section)) {
                section.layout(helper, section.leftGutter(), y, helper.layoutWidth - section.rightGutter())
                y += section.height
                helper.filledArea += section.height
                state.headPosition = 0
                state.tailPosition = 0
            }
        }

        if (y < helper.layoutLimit) {
            helper.getFooter(section)?.use {
                addToRecyclerView()
                measure()
                layout(0, y, measuredWidth, y + measuredHeight, helper.numViews)
                if (helper.isPreLayout && isRemoved) {
                    helper.addIgnoredHeight(height)
                }
                y += height
                helper.filledArea += height
                state.state = ADDED
                if (state.headPosition < 0) state.headPosition = 1
                state.tailPosition = 1
            }
        } else {
            state.state = ABSENT
        }

        state.bottom = y
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        if (layoutState.headPosition < 0) {
            layoutState.headPosition = 2
        }
        val state = layoutState as FooterLayoutState

        var toFill = dy - state.overdraw

        if (state.headPosition > 1) {
            helper.getFooter(section)?.use {
                addToRecyclerView()
                measure()
                val filled = fillTop(toFill, 0, -state.overdraw - measuredHeight, measuredWidth, -state.overdraw)
                state.overdraw += filled
                toFill -= filled
                state.state = ADDED
                state.headPosition = 1
                state.tailPosition = 1
            }
        }

        if (toFill > 0) {
            val filled = section.fillTop(toFill, section.leftGutter(), -state.overdraw,
                                         helper.layoutWidth - section.rightGutter(), helper)
            state.overdraw += filled
            toFill -= filled
            state.headPosition = 0
        }
        val filled = Math.min(dy, state.overdraw)
        state.overdraw -= filled
        state.bottom += filled
        return filled
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        Log.d("FOOTER", "dy = $dy, state = $layoutState")
        val state = layoutState as FooterLayoutState
        if (state.headPosition < 0) state.headPosition = 0

        return if (state.state == ADDED) {
            (state.bottom - helper.layoutLimit).takeIf { it > 0 } ?: 0
        } else {
            // Must fill section children first.
            val before = section.height
            var filled = section.fillBottom(dy, section.leftGutter(), state.bottom - section.height,
                                            helper.layoutWidth - section.rightGutter(), helper)
            Log.d("FOOTER", "filled = $filled")
            state.bottom += section.height - before
            state.tailPosition = 0

            // Add footer
            if (filled < dy) {
                helper.getFooter(section)?.use {
                    Log.d("FOOTER ADD", "add footer")
                    addToRecyclerView()
                    measure()
                    filled += fillBottom(dy - filled, 0, state.bottom, measuredWidth, state.bottom + measuredHeight,
                                         helper.numViews)
                    state.bottom += height
                    state.state = ADDED
                    state.tailPosition = 1
                }
            }

            filled
        }
    }

    override fun onTrimTop(scrolled: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        Log.d("FOOTER TRIM", "state = ${layoutState}")
        if (helper.numViews == 0) return 0

        val state = layoutState as FooterLayoutState
        var removedHeight = if (state.headPosition == 0) section.trimTop(scrolled, helper, 0) else 0
        if (section.numViews == 0) {
            state.headPosition = 1
        }
        if (helper.numViews == 1 && state.state == ADDED) {
            helper.getAttachedViewAt(helper.numViews - 1).let {
                if (helper.getBottom(it) < 0) {
                    helper.removeView(it)
                    removedHeight += Math.max(0, it.height - state.overdraw)
                    layoutState.overdraw = Math.max(0, layoutState.overdraw - it.height)
                    state.tailPosition = 0
                    state.state = ABSENT
                } else if (helper.getTop(it) < 0) {
                    val before = state.overdraw
                    state.overdraw = -helper.getTop(it)
                    removedHeight += state.overdraw - before
                }
            }
        }
        if (helper.numViews == 0) {
            state.headPosition = -1
            state.tailPosition = -1
        }
        state.bottom -= removedHeight
        return removedHeight
    }

    override fun onTrimBottom(scrolled: Int, helper: LayoutHelper, section: SectionState,
                              layoutState: LayoutState): Int {
        if (helper.numViews == 0) return 0

        val state = layoutState as FooterLayoutState
        var removedHeight = 0
        if (state.state == ADDED) {
            helper.getAttachedViewAt(helper.numViews - 1).let {
                if (helper.getTop(it) > helper.layoutLimit) {
                    removedHeight += it.height
                    helper.removeView(it)
                    state.tailPosition = 0
                    state.state = ABSENT
                }
            }
        }
        removedHeight += section.trimBottom(scrolled - removedHeight, helper, 0)
        if (helper.numViews == 0) {
            state.headPosition = -1
            state.headPosition = -1
        }
        state.bottom -= removedHeight
        return removedHeight
    }
}

private object StickyFlm : BaseFlm {
    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        TODO("not implemented")
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        TODO("not implemented")
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        TODO("not implemented")
    }

    override fun onTrimTop(scrolled: Int, helper: LayoutHelper,
                           section: SectionState,
                           layoutState: LayoutState): Int {
        TODO("not implemented")
    }

    override fun onTrimBottom(scrolled: Int, helper: LayoutHelper,
                              section: SectionState,
                              layoutState: LayoutState): Int {
        TODO("not implemented")
    }
}

private object GutterFlm : BaseFlm {
    override fun isAtTop(section: SectionState, layoutState: LayoutState) = section.atTop

    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        TODO("not implemented")
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        TODO("not implemented")
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        TODO("not implemented")
    }

    override fun onTrimTop(scrolled: Int, helper: LayoutHelper,
                           section: SectionState,
                           layoutState: LayoutState): Int {
        TODO("not implemented")
    }

    override fun onTrimBottom(scrolled: Int, helper: LayoutHelper,
                              section: SectionState,
                              layoutState: LayoutState): Int {
        TODO("not implemented")
    }
}