package com.tonicartos.superslim.internal.layout

import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionConfig
import com.tonicartos.superslim.SectionLayoutManager
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
        return DoNothingSlm.takeUnless { section.hasFooter } ?: when (section.baseConfig.headerStyle) {
            SectionConfig.HEADER_INLINE                          -> inlineFlm
            SectionConfig.HEADER_START, SectionConfig.HEADER_END -> gutterFlm
            else                                                 -> stickyFlm
        }
    }
}

private interface BaseFlm : SectionLayoutManager<SectionState> {
    override fun isAtTop(section: SectionState, layoutState: LayoutState) = section.atTop
}

private object inlineFlm : BaseFlm {
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
            Math.max(0, state.bottom - helper.layoutLimit)
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
        var removedHeight = if (state.headPosition == 0) section.trimTop(scrolled, 0, helper, 0) else 0
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
        Log.d("STICKY FLM", "layout")
        val state = layoutState as FooterLayoutState
        var y = -state.overdraw

        helper.getFooter(section)?.use {
            addToRecyclerView()
            measure()
            helper.stickyEndInset += measuredHeight

            if (state.headPosition <= 0) {
                if (helper.moreToLayout(0, section)) {
                    section.layout(helper, section.leftGutter(), y, helper.layoutWidth - section.rightGutter())
                    y += section.height
                    helper.filledArea += section.height
                    state.headPosition = 0
                    state.tailPosition = 0
                }
            }

            helper.stickyEndInset -= measuredHeight
            // Detect and adjust positioning to sticky or not.
            var bottom = y + measuredHeight
            val limit = helper.layoutLimit - helper.stickyEndInset
            val floatOffset = if (bottom > limit) limit - bottom else 0
            bottom += floatOffset

            layout(0, bottom - measuredHeight, measuredWidth, bottom, helper.numViews)

            // 100% floating footer has 0 height.
            val floatAdjustedHeight = Math.max(0, height + floatOffset)
            if (helper.isPreLayout && isRemoved) helper.addIgnoredHeight(floatAdjustedHeight)
            helper.filledArea += floatAdjustedHeight
            state.state = FLOATING
            if (state.headPosition < 0) state.headPosition = 1
            state.tailPosition = 1
            y += height
            if (y < helper.layoutLimit) {
                state.state = ADDED
            }
        }

        state.bottom = y
    }

    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: SectionState, layoutState: LayoutState): Int {
        Log.d("FOOTER", "dy = $dy, state = $layoutState, helper = $helper")
        val state = layoutState as FooterLayoutState
        if (state.headPosition < 0) state.headPosition = 0


        if (state.state == ABSENT) {
            helper.getFooter(section)?.use {
                Log.d("FOOTER ADD", "add footer")
                addToRecyclerView()
                measure()
                fillBottom(dy, 0, state.bottom, measuredWidth, state.bottom + measuredHeight, helper.numViews)
                state.state = FLOATING
                state.tailPosition = 1
                state.bottom = measuredHeight
            }
        }

        val footer = helper.getAttachedViewAt(helper.numViews - 1)
//        if (state.state == ADDED && helper.getBottom(footer) == state.bottom) {
//            return 0
//        }

        var filled = helper.insetStickyEnd(footer.height) {
            val before = section.height
            val fill = section.fillBottom(dy, section.leftGutter(), 0, helper.layoutWidth - section.rightGutter(),
                                          helper)
            state.bottom += section.height - before
            fill
        }

        if (state.state != ADDED && filled < dy) {
            filled += footer.height
            state.tailPosition = 1
            state.state = ADDED
            Log.d("FOOTER", "Floating footer inserted.")
        }

        val offset = calculateStickyFooterOffset(footer, helper, state, dy)
        footer.offsetTopAndBottom(offset)
        return Math.min(dy, filled)
    }

    override fun onTrimBottom(scrolled: Int, helper: LayoutHelper, section: SectionState,
                              layoutState: LayoutState): Int {
        if (helper.numViews == 0) return 0
        val state = layoutState as FooterLayoutState
        if (state.state == ABSENT) throw IllegalStateException("Missing footer in trim operation.")
        val footer = helper.getAttachedViewAt(helper.numViews - 1)
        Log.d("FOOTER TRIM", "footer = ${((footer as LinearLayout).getChildAt(0) as TextView).text}")

        // Trim content.
        var removedHeight = helper.insetStickyEnd(footer.height) { section.trimBottom(scrolled, 0, helper) }
        if (section.numViews == 0) state.headPosition = 1

        // Adjust footer.
        val offset = calculateStickyFooterOffset(footer, helper, state)
        footer.offsetTopAndBottom(offset)

        // Trim and update state.
        if (helper.getTop(footer) > helper.layoutLimit) {
            removedHeight += footer.height
            helper.removeView(footer)
            state.state = ABSENT
        } else if (helper.getBottom(footer) <= state.bottom - footer.height) {
            state.state = FLOATING
        }

        if (helper.numViews == 0) {
            state.headPosition = -1
            state.headPosition = -1
        }
        state.bottom -= removedHeight
        return removedHeight
    }

    private fun calculateStickyFooterOffset(footer: View, helper: LayoutHelper, state: FooterLayoutState,
                                            stickyEdgeFudge: Int = 0): Int {
        val bottom = helper.getBottom(footer)
        val stickyEdge = helper.layoutLimit - helper.stickyEndInset + stickyEdgeFudge

        var offset = if (stickyEdge >= state.bottom) state.bottom - bottom else stickyEdge - bottom
        if (bottom + offset > state.bottom) offset -= bottom + offset - state.bottom

        val newTop = bottom - footer.height + offset
        if (newTop < 0) offset -= newTop

        Log.d("FOOTER",
              "offset = $offset, bottom = $bottom, dy = $stickyEdgeFudge, y = $state.bottom, stickyEdge = $stickyEdge, layoutLimit = ${helper.layoutLimit}, stickyEndInset = ${helper.stickyEndInset}")

        return offset
    }
}

private object gutterFlm : BaseFlm {
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