package com.tonicartos.superslim.internal.layout

import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionConfig
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.internal.AttachedView
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.SectionState.FooterLayoutState
import com.tonicartos.superslim.internal.SectionState.LayoutState
import com.tonicartos.superslim.internal.insetStickyEnd
import com.tonicartos.superslim.use

/**
 * The footer is not a child view yet.
 */
private const val ABSENT = 0
/**
 * The footer is a child view and also has grown the section height.
 */
private const val ADDED = 1 shl 0
/**
 * The footer is a child view, but has not grown the section height.
 */
private const val FLOATING = 1 shl 1

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
        return DoNothingSlm.takeUnless { section.hasFooter } ?: when (section.baseConfig.footerStyle) {
            SectionConfig.FOOTER_INLINE                          -> inlineFlm
            SectionConfig.FOOTER_START, SectionConfig.FOOTER_END -> gutterFlm
            else                                                 -> stickyFlm
        }
    }
}

private interface BaseFlm : SectionLayoutManager<SectionState> {
    override fun isAtTop(section: SectionState, layoutState: LayoutState) = section.atTop
}

private object inlineFlm : BaseFlm {
    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        val state = layoutState as FooterLayoutState
        var y = -state.overdraw

        if (state.headPosition <= 0) {
            if (helper.moreToLayout(0, section)) {
                section.layout(helper, section.leftGutter { 0 }, y, helper.layoutWidth - section.rightGutter { 0 })
                state.disappearedOrRemovedHeight += section.disappearedHeight
                y += section.height
                helper.filledArea += section.height
                state.headPosition = 0
                state.tailPosition = 0
            }
        }

        if (y < helper.layoutLimit) {
            helper.getFooter(section)?.use { footer ->
                footer.addToRecyclerView()
                footer.measure()
                footer.layout(0, y, footer.measuredWidth, y + footer.measuredHeight, helper.numViews)
                if (helper.isPreLayout && footer.isRemoved) helper.addIgnoredHeight(footer.height)
                state.disappearedOrRemovedHeight += footer.disappearedHeight
                y += footer.height
                helper.filledArea += footer.height
                state.mode = ADDED
                if (state.headPosition < 0) state.headPosition = 1
                state.tailPosition = 1
            }
        } else {
            state.mode = ABSENT
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
            helper.getFooter(section)?.use { footer ->
                footer.addToRecyclerView()
                footer.measure()
                val filled = footer.fillTop(toFill, 0, -state.overdraw - footer.measuredHeight, footer.measuredWidth,
                                            -state.overdraw)
                state.overdraw += filled
                toFill -= filled
                state.mode = ADDED
                state.headPosition = 1
                state.tailPosition = 1
            }
        }

        state.overdraw += section.fillTop(Math.max(0, toFill), section.leftGutter { 0 }, -state.overdraw,
                                          helper.layoutWidth - section.rightGutter { 0 }, helper)
        if (state.mode == ADDED &&helper.numViews > 1) state.headPosition = 0
        val filled = Math.min(dy, state.overdraw)
        state.overdraw -= filled
        state.bottom += filled
        return filled
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        val state = layoutState as FooterLayoutState
        if (state.headPosition < 0) state.headPosition = 0

        // Must fill section children first.
        val before = section.height
        var filled = section.fillBottom(dy, section.leftGutter { 0 }, 0,
                                        helper.layoutWidth - section.rightGutter { 0 }, helper)
        state.bottom += section.height - before
        state.tailPosition = 0

        if (state.mode == ABSENT && filled < dy) {
            helper.getFooter(section)?.use { footer ->
                footer.addToRecyclerView()
                footer.measure()
                filled += footer.fillBottom(dy - filled, 0, state.bottom, footer.measuredWidth,
                                            state.bottom + footer.measuredHeight, helper.numViews)
                state.bottom += footer.height
                state.mode = ADDED
                state.tailPosition = 1
            }
        } else if (state.mode == ADDED) {
            filled += Math.max(0, state.bottom - helper.layoutLimit)
        }

        return Math.min(dy, filled)
    }

    override fun onTrimTop(scrolled: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        val state = layoutState as FooterLayoutState
        var removedHeight = if (state.headPosition == 0) section.trimTop(scrolled, 0, helper, 0) else 0
        if (helper.numViews == 1 && state.mode == ADDED) {
            helper.getAttachedViewAt(helper.numViews - 1) { footer ->
                state.headPosition = 1
                if (footer.bottom < 0) {
                    footer.remove()
                    val height = footer.height
                    removedHeight += Math.max(0, height - state.overdraw)
                    layoutState.overdraw = Math.max(0, layoutState.overdraw - height)
                    state.tailPosition = 0
                    state.mode = ABSENT
                } else if (footer.top < 0) {
                    val before = state.overdraw
                    state.overdraw = -footer.top
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
        val state = layoutState as FooterLayoutState
        var removedHeight = 0
        if (state.mode == ADDED) {
            helper.getAttachedViewAt(helper.numViews - 1) { footer ->
                if (footer.top > helper.layoutLimit) {
                    removedHeight += footer.height
                    footer.remove()
                    state.tailPosition = 0
                    state.mode = ABSENT
                }
            }
        }
        removedHeight += section.trimBottom(scrolled - removedHeight, 0, helper, 0)
        if (helper.numViews == 0) {
            state.headPosition = -1
            state.headPosition = -1
        }
        state.bottom -= removedHeight
        return removedHeight
    }
}

private object stickyFlm : BaseFlm by inlineFlm {
    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        val state = layoutState as FooterLayoutState
        var y = -state.overdraw

        helper.getFooter(section)?.use { footer ->
            footer.addToRecyclerView()
            footer.measure()

            helper.insetStickyEnd(footer.measuredHeight) {
                if (state.headPosition <= 0) {
                    if (helper.moreToLayout(0, section)) {
                        section.layout(helper, section.leftGutter { 0 }, y,
                                       helper.layoutWidth - section.rightGutter { 0 })
                        state.disappearedOrRemovedHeight += section.disappearedHeight
                        y += section.height
                        helper.filledArea += section.height
                        state.headPosition = 0
                        state.tailPosition = 0
                    }
                }
            }

            // Detect and adjust positioning to sticky or not.
            var bottom = y + footer.measuredHeight
            val limit = helper.layoutLimit - helper.stickyEndInset
            val floatOffset = if (bottom > limit) limit - bottom else 0
            bottom += floatOffset

            footer.layout(0, bottom - footer.measuredHeight, footer.measuredWidth, bottom, helper.numViews)

            // 100% floating footer has 0 height.
//            val floatAdjustedHeight = Math.max(0, footer.height + floatOffset)
            if (helper.isPreLayout && footer.isRemoved) helper.addIgnoredHeight(footer.height)
            state.disappearedOrRemovedHeight += footer.disappearedHeight
            helper.filledArea += footer.height
            state.mode = FLOATING
            if (state.headPosition < 0) state.headPosition = 1
            state.tailPosition = 1
            if (y < helper.layoutLimit) state.mode = ADDED
            y += footer.height
        }

        state.bottom = y
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        val state = layoutState as FooterLayoutState
        if (state.headPosition < 0) state.headPosition = 0

        if (state.mode == ABSENT) {
            helper.getFooter(section)?.use { footer ->
                footer.addToRecyclerView()
                footer.measure()
                footer.fillBottom(dy, 0, state.bottom, footer.measuredWidth, state.bottom + footer.measuredHeight,
                                  helper.numViews)
                state.mode = FLOATING
                state.tailPosition = 1
                state.bottom = footer.measuredHeight
            }
        }

        return Math.min(dy, helper.getAttachedViewAt(helper.numViews - 1) { footer ->
            var filled = helper.insetStickyEnd(footer.height) {
                val before = section.height
                val fill = section.fillBottom(dy, section.leftGutter { 0 }, 0,
                                              helper.layoutWidth - section.rightGutter { 0 },
                                              helper)
                state.bottom += section.height - before
                fill
            }

            if (state.mode != ADDED && filled < dy) {
                filled += footer.height
                state.tailPosition = 1
                state.mode = ADDED
            }

            val offset = calculateStickyFooterOffset(footer, helper, state, dy)
            footer.offsetTopAndBottom(offset)
            filled
        })
    }

    override fun onTrimBottom(scrolled: Int, helper: LayoutHelper, section: SectionState,
                              layoutState: LayoutState): Int {
        val state = layoutState as FooterLayoutState
        if (state.mode == ABSENT) throw IllegalStateException("Missing footer in trim operation.")
        val removedHeight = helper.getAttachedViewAt(helper.numViews - 1) { footer ->
            // Trim content.
            var removedHeight = helper.insetStickyEnd(footer.height) { section.trimBottom(scrolled, 0, helper) }
            if (section.numViews == 0) state.headPosition = 1

            // Adjust footer.
            val offset = calculateStickyFooterOffset(footer, helper, state)
            footer.offsetTopAndBottom(offset)

            // Trim and update mode.
            if (footer.top > helper.layoutLimit) {
                removedHeight += footer.height
                footer.remove()
                state.mode = ABSENT
            } else if (footer.bottom <= state.bottom - footer.height) {
                state.mode = FLOATING
            }
            removedHeight
        }

        if (helper.numViews == 0) {
            state.headPosition = -1
            state.headPosition = -1
        }
        state.bottom -= removedHeight
        return removedHeight
    }

    private fun calculateStickyFooterOffset(footer: AttachedView, helper: LayoutHelper, state: FooterLayoutState,
                                            stickyEdgeFudge: Int = 0): Int {
        val bottom = footer.bottom
        val stickyEdge = helper.layoutLimit - helper.stickyEndInset + stickyEdgeFudge

        var offset = if (stickyEdge >= state.bottom) state.bottom - bottom else stickyEdge - bottom
        if (bottom + offset > state.bottom) offset -= bottom + offset - state.bottom

        val newTop = bottom - footer.height + offset
        if (newTop < 0) offset -= newTop

        return offset
    }
}

// TODO: Gutter integration between footers and headers. Gutter needs to be applied by Hlms and Flms, however Flm needs to know how much has already been applied. Effect of all applications is max of all, not sum. So for pass do max calculation and save to section state. Inset only by abs delta. Saved gutter gets reset for each entry into section for Plm.

// TODO: Gutter footers and headers need to not have space in the section for them, unless the section is smaller than them.

// TODO: Support footers with layout_height = match_parent.

private object gutterFlm : BaseFlm {
    override fun isAtTop(section: SectionState, layoutState: LayoutState) = section.atTop

    override fun onLayout(helper: LayoutHelper, section: SectionState, layoutState: LayoutState) {
        val state = layoutState as FooterLayoutState
        var y = -state.overdraw

        helper.getFooter(section)?.use { footer ->
            footer.addToRecyclerView()
            val isLeft = section.baseConfig.footerStyle == SectionConfig.FOOTER_START
            val gutterWidth = if (isLeft) section.leftGutter { 0 } else section.rightGutter { 0 }
            footer.measure(if (gutterWidth == 0) 0 else helper.layoutWidth - gutterWidth)

            if (state.headPosition <= 0) {
                if (helper.moreToLayout(0, section)) {
                    section.layout(helper, section.leftGutter { footer.measuredWidth }, y,
                                   helper.layoutWidth - section.rightGutter { footer.measuredWidth })
                    y += section.height
                    helper.filledArea += section.height
                    state.headPosition = 0
                    state.tailPosition = 0
                }
            }

            // Detect and adjust positioning to sticky or not.
            var bottom = y
            val limit = helper.layoutLimit - helper.stickyEndInset
            val floatOffset = if (bottom > limit) limit - bottom else 0
            bottom += floatOffset

            footer.layout(0, bottom - footer.measuredHeight, footer.measuredWidth, bottom, helper.numViews)

            state.mode = FLOATING
            if (state.headPosition < 0) state.headPosition = 1
            state.tailPosition = 1

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