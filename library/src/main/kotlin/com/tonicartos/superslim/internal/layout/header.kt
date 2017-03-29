package com.tonicartos.superslim.internal.layout

import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionConfig
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.SectionState.HeaderLayoutState
import com.tonicartos.superslim.internal.SectionState.LayoutState
import com.tonicartos.superslim.internal.insetStickyStart
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
        val state = layoutState as HeaderLayoutState
        var y = -state.overdraw
        if (state.headPosition <= 0) {
            helper.getHeader(section)?.use { header ->
                header.addToRecyclerView()
                header.measure()
                header.layout(0, y, header.measuredWidth, y + header.measuredHeight)
                if (helper.isPreLayout && header.isRemoved) helper.addIgnoredHeight(header.height)
                state.disappearedOrRemovedHeight += header.disappearedHeight
                y += header.height
                helper.filledArea += header.height
                state.mode = ADDED
                state.headPosition = 0
                state.tailPosition = 0
            }
        } else {
            state.mode = ABSENT
            state.headPosition = 1
        }

        section.layout(helper, section.leftGutter { 0 }, y, helper.layoutWidth - section.rightGutter { 0 },
                       if (state.mode == ADDED) 1 else 0)
        state.disappearedOrRemovedHeight += section.disappearedHeight
        y += section.height
        helper.filledArea += section.height
        if (section.numViews > 0) state.tailPosition = 1

        state.bottom = y
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        val state = layoutState as HeaderLayoutState
        if (state.headPosition < 0) state.headPosition = 1

        if (state.headPosition == 1) {
            // Must fill section children first.
            state.overdraw += section.fillTop(Math.max(0, dy - state.overdraw), section.leftGutter { 0 },
                                              0, helper.layoutWidth - section.rightGutter { 0 }, helper)
            state.tailPosition = 1

            // Fill header if there is space left.
            if (dy - state.overdraw > 0) {
                helper.getHeader(section)?.use { header ->
                    header.addToRecyclerView(0)
                    header.measure()
                    state.overdraw += header.fillTop(dy - state.overdraw, 0,
                                                     -state.overdraw - header.measuredHeight,
                                                     header.measuredWidth,
                                                     -state.overdraw)
                    state.mode = ADDED
                    state.headPosition = 0
                }
            }
        }
        val filled = Math.min(dy, state.overdraw)
        state.overdraw -= filled
        state.bottom += filled
        return filled
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        val state = layoutState as HeaderLayoutState

        var filled = if (state.tailPosition == 0) Math.max(0, state.bottom - helper.layoutLimit) else 0
        if (state.headPosition < 0) {
            helper.getHeader(section)?.use { header ->
                header.addToRecyclerView()
                header.measure()
                filled += header.fillBottom(dy, 0, -state.overdraw, header.measuredWidth,
                                            -state.overdraw + header.measuredHeight)
                state.bottom += header.height
                state.mode = ADDED
                state.headPosition = 0
            }
        }

        val before = section.height
        filled += section.fillBottom(dy - filled, section.leftGutter { 0 }, state.bottom - section.height,
                                     helper.layoutWidth - section.rightGutter { 0 }, helper,
                                     if (state.mode == ADDED) 1 else 0)
        state.bottom += section.height - before
        state.tailPosition = 1
        return Math.min(dy, filled)
    }

    override fun onTrimTop(scrolled: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        val state = layoutState as HeaderLayoutState
        var removedHeight = 0
        var contentTop = 0
        if (state.mode == ADDED) {
            helper.getAttachedViewAt(0) { header ->
                if (header.bottom < 0) {
                    header.remove()
                    removedHeight += Math.max(0, header.height - state.overdraw)
                    state.overdraw = Math.max(0, state.overdraw - header.height)
                    state.headPosition = 1
                    state.mode = ABSENT
                } else if (header.top < 0) {
                    val before = state.overdraw
                    state.overdraw = -header.top
                    removedHeight += state.overdraw - before
                    contentTop = header.bottom
                }
            }
        }
        removedHeight += section.trimTop(scrolled, contentTop, helper, if (state.mode == ADDED) 1 else 0)
        if (helper.numViews == 0) {
            state.headPosition = -1
            state.headPosition = -1
        }
        state.bottom -= removedHeight
        return removedHeight
    }

    override fun onTrimBottom(scrolled: Int, helper: LayoutHelper, section: SectionState,
                              layoutState: LayoutState): Int {
        val state = layoutState as HeaderLayoutState
        var removed = 0
        removed += section.trimBottom(scrolled, state.bottom - section.height, helper,
                                      if (state.mode == ADDED) 1 else 0)
        if (section.numViews == 0) {
            state.tailPosition = 0
        }
        if (state.mode == ADDED) {
            helper.getAttachedViewAt(0) { header ->
                if (header.top >= helper.layoutLimit) {
                    removed += header.height
                    header.remove()
                    state.mode = ABSENT
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

// Sticky headers are always attached after content.
private object StickyHlm : BaseHlm {
    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        val state = layoutState as HeaderLayoutState
        var y = -state.overdraw

        helper.getHeader(section)?.use { header ->
            header.addToRecyclerView()
            header.measure()
            header.layout(0, y, header.measuredWidth, y + header.measuredHeight)

            if (state.headPosition <= 0) {
                if (helper.isPreLayout && header.isRemoved) helper.addIgnoredHeight(header.height)
                state.disappearedOrRemovedHeight += header.disappearedHeight
                y += header.height
                helper.filledArea += header.height
                state.mode = ADDED
                state.headPosition = 0
                state.tailPosition = 0
            }

            helper.insetStickyStart(header.measuredHeight) {
                section.layout(helper, section.leftGutter { 0 }, y,
                               helper.layoutWidth - section.rightGutter { 0 })
                state.disappearedOrRemovedHeight += section.disappearedHeight
                y += section.height
                helper.filledArea += section.height
                state.headPosition = 0
                state.tailPosition = 0
            }

            if (state.mode == ABSENT) {

            }

            // Detect and adjust positioning to sticky or not.
            var bottom = y + header.measuredHeight
            val limit = helper.layoutLimit - helper.stickyStartInset
            val floatOffset = if (bottom > limit) limit - bottom else 0
            bottom += floatOffset

            header.layout(0, bottom - header.measuredHeight, header.measuredWidth, bottom, helper.numViews)

            // 100% floating header has 0 height.
//            val floatAdjustedHeight = Math.max(0, header.height + floatOffset)
            if (helper.isPreLayout && header.isRemoved) helper.addIgnoredHeight(header.height)
            helper.filledArea += header.height
            state.mode = FLOATING
            if (state.headPosition < 0) state.headPosition = 1
            state.tailPosition = 1
            y += header.height
            if (y < helper.layoutLimit) {
                state.mode = ADDED
            }
        }

        state.bottom = y
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