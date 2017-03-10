package com.tonicartos.superslim.internal.layout

import android.util.Log
import android.view.View
import com.tonicartos.superslim.*
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.SectionState.FooterLayoutState
import com.tonicartos.superslim.internal.SectionState.LayoutState

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

        if (state.headPosition == 0) {
            if (helper.moreToLayout(0, section)) {
                section.layout(helper, section.leftGutter(), y, helper.layoutWidth - section.rightGutter())

                y += section.height
                helper.filledArea += section.height
                state.tailPosition = 1
            } else {
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
            }
        } else {
            state.state = ABSENT
        }

        state.bottom = y
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        val state = layoutState as FooterLayoutState

        var toFill = dy - state.overdraw

        if (state.headPosition < 0) {
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
        val state = layoutState as FooterLayoutState

        var filled = (state.bottom - helper.layoutLimit).takeIf { it > 0 } ?: 0
        var toFill = dy - filled

        if (toFill > 0 && state.headPosition < 1) {
            section.fillBottom(toFill, section.leftGutter(), state.bottom,
                               helper.layoutWidth - section.rightGutter(), helper).let {
                toFill -= it
                filled += it
                state.bottom += it
            }
            state.headPosition = 0
            state.tailPosition = 0
        }
        if (toFill > 0) {
            helper.getFooter(section)?.use {
                addToRecyclerView()
                measure()
                fillBottom(toFill, 0, state.bottom, measuredWidth, measuredHeight, helper.numViews).let {
                    toFill -= it
                    filled += it
                    state.bottom += it
                }
                state.state = ADDED
                state.tailPosition = 1
            }
        }

        return filled
    }

    override fun onTrimTop(scrolled: Int, helper: LayoutHelper,
                           section: SectionState,
                           layoutState: LayoutState): Int {
        if (helper.numViews == 0) return 0

        val state = layoutState as FooterLayoutState
        var removed = section.trimTop(scrolled, helper, 0)
        if (section.numViews == 0) {
            state.headPosition = 1
        }
        if (state.state == ADDED) {
            val footer = helper.getAttachedViewAt(helper.numViews - 1)
            if (helper.getBottom(footer) < 0) {
                removed += footer.height
                helper.removeView(footer)
                state.overdraw = 0
                state.tailPosition = 0
                state.state = ABSENT
            } else if (helper.getTop(footer) < 0) {
                val before = state.overdraw
                state.overdraw = helper.getTop(footer)
                removed += before - state.overdraw
                state.bottom -= removed
                return removed
            }
        }
        if (helper.numViews == 0) {
            state.headPosition = -1
            state.tailPosition = -1
        }
        state.bottom -= removed
        return removed
    }

    override fun onTrimBottom(scrolled: Int, helper: LayoutHelper,
                              section: SectionState,
                              layoutState: LayoutState): Int {
        if (helper.numViews == 0) return 0

        val state = layoutState as FooterLayoutState
        var removed = 0
        if (state.state == ADDED) {
            val footer = helper.getAttachedViewAt(helper.numViews - 1)
            if (helper.getTop(footer) > helper.layoutLimit) {
                removed += footer.height
                helper.removeView(footer)
                state.tailPosition = 0
                state.state = ABSENT
            }
        }
        removed += section.trimBottom(scrolled - removed, helper, if (state.state == ADDED) 1 else 0)
        if (helper.numViews == 0) {
            state.headPosition = -1
            state.headPosition = -1
        }
        state.bottom -= removed
        return removed
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