package com.tonicartos.superslim.internal.layout

import android.util.Log
import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionConfig
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.SectionState.HeaderLayoutState
import com.tonicartos.superslim.internal.SectionState.LayoutState
import com.tonicartos.superslim.use

private const val ABSENT = 1 shl 0
private const val ADDED = 1 shl 1
private const val FLOATING = 1 shl 2

internal object HeaderLayoutManager : SectionLayoutManager<SectionState> {
    override fun isAtTop(section: SectionState, layoutState: LayoutState)
            = selectHeaderLayout(section).isAtTop(section, layoutState)

    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState)
            = selectHeaderLayout(section).onLayout(helper, section, layoutState)

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState)
            = selectHeaderLayout(section).onFillTop(dy, helper, section, layoutState)

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState)
            = selectHeaderLayout(section).onFillBottom(dy, helper, section, layoutState)

    override fun onTrimTop(scrolled: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState)
            = selectHeaderLayout(section).onTrimTop(scrolled, helper, section, layoutState)

    override fun onTrimBottom(scrolled: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState)
            = selectHeaderLayout(section).onTrimBottom(scrolled, helper, section, layoutState)

    private fun selectHeaderLayout(section: SectionState): SectionLayoutManager<SectionState> {
        return DoNothingSlm.takeUnless { section.hasHeader } ?: when (section.baseConfig.headerStyle) {
            SectionConfig.HEADER_INLINE                          -> InlineHlm
            SectionConfig.HEADER_START, SectionConfig.HEADER_END -> GutterHlm
            else                                                 -> StickyHlm
        }
    }
}

private interface BaseHlm : SectionLayoutManager<SectionState> {
    override fun isAtTop(section: SectionState, layoutState: LayoutState)
            = layoutState.overdraw == 0 && layoutState.headPosition <= 0 && section.atTop
}

private object InlineHlm : BaseHlm {
    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        Log.d("INLINE HLM", "layout")
        val state = layoutState as HeaderLayoutState
        var y = -state.overdraw
        if (state.headPosition <= 0) {
            helper.getHeader(section)?.use { footer ->
                Log.d("Header", "add header")
                footer.addToRecyclerView()
                footer.measure()
                footer.layout(0, y, footer.measuredWidth, y + footer.measuredHeight)
                if (helper.isPreLayout && footer.isRemoved) {
                    helper.addIgnoredHeight(footer.height)
                }
                Log.d("Header", "numViews = $footer.numViews")
                y += footer.height
                helper.filledArea += footer.height
                state.state = ADDED
            }
        } else {
            state.state = ABSENT
        }

        section.layout(helper, section.leftGutter{0}, y, helper.layoutWidth - section.rightGutter{0},
                       if (state.state == ADDED) 1 else 0)
        y += section.height
        if (state.state == ABSENT) state.headPosition = 1
        state.tailPosition = if (section.height == 0) 0 else 1

        state.bottom = y
        Log.d("SADFASDF", "height = $y")
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        val state = layoutState as HeaderLayoutState

        // How much distance left to fill.
        var toFill = dy - state.overdraw

        if (toFill > 0 && state.headPosition > 0) {
            if (state.headPosition == 1 || state.headPosition < 0) {
                // Fill content
                section.fillTop(toFill, section.leftGutter{0}, -state.overdraw,
                                helper.layoutWidth - section.rightGutter{0}, helper).let {
                    state.overdraw += it
                    toFill -= it
                    state.tailPosition = 1
                    state.headPosition = 1
                }

                // Fill header if there is space left.
                if (toFill > 0) {
                    helper.getHeader(section)?.use { footer ->
                        footer.addToRecyclerView()
                        footer.measure()
                        footer.fillTop(toFill, 0, -state.overdraw - footer.measuredHeight, footer.measuredWidth, -state.overdraw)
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

        var filled = (state.bottom - helper.layoutLimit).takeIf { it > 0 } ?: 0
        var toFill = dy - filled
        // Add header if room.
        if (toFill > 0 && state.headPosition <= 0) {
            helper.getHeader(section)?.use { footer ->
                footer.addToRecyclerView()
                footer.measure()
                footer.fillBottom(toFill, 0, state.bottom, footer.measuredWidth, footer.measuredHeight)
            }?.let {
                toFill -= it
                filled += it
                state.headPosition = 0
                state.tailPosition = 0
                state.bottom += it
                state.state = ADDED
            }
        }

        // Fill content if room.
        if (toFill > 0) {
            section.fillBottom(toFill, section.leftGutter{0}, state.bottom, helper.layoutWidth - section.rightGutter{0},
                               helper, if (state.state == ADDED) 1 else 0).let {
                toFill -= it
                filled += it
                state.bottom += it
                if (state.state == ABSENT) state.headPosition = 1
                state.tailPosition = 1
            }
        }

        return filled
    }

    override fun onTrimTop(scrolled: Int, helper: LayoutHelper, section: SectionState,
                           layoutState: LayoutState): Int {
        Log.d("HEADER", "onTrimTop")
        Log.d("header", "layoutState = $layoutState")
        if (helper.numViews == 0) return 0

        val state = layoutState as HeaderLayoutState
        var removed = 0
        if (state.state == ADDED) {
            helper.getAttachedViewAt(0) { header ->
                Log.d("header", "top = ${header.top}, bottom = ${header.bottom}")
                if (header.bottom < 0) {
                    removed += header.height
                    header.remove()
                    state.overdraw = 0
                    state.headPosition = 1
                    state.state = ABSENT
                } else if (header.top < 0) {
                    Log.d("header", "top = ${header.top}")
                    val before = state.overdraw
                    state.overdraw = header.top
                    removed += before - state.overdraw
                    state.bottom -= removed
                    return removed
                }
            }
        }
        removed += section.trimTop(scrolled, 0, helper, if (state.state == ADDED) 1 else 0)
        if (helper.numViews == 0) {
            state.tailPosition = -1
            state.headPosition = -1
        }
        state.bottom -= removed
        return removed
    }

    override fun onTrimBottom(scrolled: Int, helper: LayoutHelper, section: SectionState,
                              layoutState: LayoutState): Int {
        Log.d("HEADER", "onTrimBottom")
        if (helper.numViews == 0) return 0

        val state = layoutState as HeaderLayoutState
        var removed = 0
        removed += section.trimBottom(scrolled, 0, helper, if (state.state == ADDED) 1 else 0)
        if (section.numViews == 0) {
            state.tailPosition = 0
        }
        if (state.state == ADDED) {
            helper.getAttachedViewAt(0) { header ->
                if (header.top > helper.layoutLimit) {
                    removed += header.height
                    header.remove()
                    state.headPosition = -1
                    state.tailPosition = -1
                    state.state = ABSENT
                }
            }
        }
        if (helper.numViews == 0) {
            state.headPosition = -1
            state.tailPosition = -1
        }
        state.bottom -= removed
        return removed
    }
}

private object StickyHlm : BaseHlm {
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

private object GutterHlm : BaseHlm {
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