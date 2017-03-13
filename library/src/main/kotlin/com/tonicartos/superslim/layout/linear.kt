package com.tonicartos.superslim.layout

import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionConfig
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.adapter.FooterStyle
import com.tonicartos.superslim.adapter.HeaderStyle
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.SectionState.LayoutState
import com.tonicartos.superslim.use

class LinearSectionConfig(gutterStart: Int = SectionConfig.DEFAULT_GUTTER,
                          gutterEnd: Int = SectionConfig.DEFAULT_GUTTER,
                          @HeaderStyle headerStyle: Int = SectionConfig.DEFAULT_HEADER_STYLE,
                          @FooterStyle footerStyle: Int = SectionConfig.DEFAULT_FOOTER_STYLE,
                          paddingStart: Int = 0, paddingTop: Int = 0, paddingEnd: Int = 0, paddingBottom: Int = 0) :
        SectionConfig(gutterStart, gutterEnd, headerStyle, footerStyle, paddingStart, paddingTop, paddingEnd,
                      paddingBottom) {
    override fun onMakeSection(oldState: SectionState?): SectionState = LinearSectionState(this, oldState)

    override fun onCopy(): LinearSectionConfig {
        return LinearSectionConfig(gutterStart, gutterEnd, headerStyle)
    }
}

internal class LinearSectionState(val configuration: LinearSectionConfig, oldState: SectionState? = null)
    : SectionState(configuration, oldState) {
    override fun isAtTop(layoutState: LayoutState)
            = LinearSlm.isAtTop(this, layoutState)

    override fun doLayout(helper: LayoutHelper, layoutState: LayoutState)
            = LinearSlm.onLayout(helper, this, layoutState)

    override fun doFillTop(dy: Int, helper: LayoutHelper, layoutState: LayoutState)
            = LinearSlm.onFillTop(dy, helper, this, layoutState)

    override fun doFillBottom(dy: Int, helper: LayoutHelper, layoutState: LayoutState)
            = LinearSlm.onFillBottom(dy, helper, this, layoutState)

    override fun doTrimTop(scrolled: Int, helper: LayoutHelper, layoutState: LayoutState)
            = LinearSlm.onTrimTop(scrolled, helper, this, layoutState)

    override fun doTrimBottom(scrolled: Int, helper: LayoutHelper, layoutState: LayoutState)
            = LinearSlm.onTrimBottom(scrolled, helper, this, layoutState)
}

internal object LinearSlm : SectionLayoutManager<LinearSectionState> {
    override fun isAtTop(section: LinearSectionState, layoutState: LayoutState)
            = layoutState.overdraw == 0 && layoutState.headPosition <= 0 &&
            section.isChildAtTop(layoutState.headPosition)

    override fun onLayout(helper: LayoutHelper, section: LinearSectionState, layoutState: LayoutState) {
        if (layoutState.headPosition < 0) layoutState.headPosition = 0
        var currentPosition = layoutState.headPosition
        var y = -layoutState.overdraw

        while (helper.moreToLayout(currentPosition, section)) {
            helper.getChild(currentPosition, section)?.use {
                addToRecyclerView()
                measure()
                layout(0, y, measuredWidth, y + measuredHeight, helper.numViews)
                if (helper.isPreLayout && isRemoved) {
                    helper.addIgnoredHeight(height)
                }
                y += height
                helper.filledArea += height
                currentPosition += 1
            } ?: break
        }
        layoutState.bottom = y
        layoutState.tailPosition = currentPosition - 1
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: LinearSectionState, layoutState: LayoutState): Int {
        if (layoutState.headPosition < 0) {
            layoutState.headPosition = section.numChildren
            layoutState.tailPosition = section.numChildren - 1
        }

        var dyRemaining = dy
        var currentPos = layoutState.headPosition
        var y = -layoutState.overdraw

        // Fill leading children
        if (0 <= currentPos && currentPos < section.numChildren) {
            helper.getUnfinishedChild(currentPos, section)?.use {
                measure()
                // Different from fillBottom because overscroll hides the excess from section height.
                fillTop(dyRemaining, 0, y - measuredHeight, measuredWidth, y).let {
                    y -= it
                    dyRemaining -= it
                }
            }
        }

        // Fill using overdraw
        dyRemaining -= layoutState.overdraw

        // Fill remaining dy with remaining content.
        while (dyRemaining > 0 && currentPos > 0) {
            currentPos -= 1
            helper.getChild(currentPos, section)?.use {
                addToRecyclerView(0)
                measure()
                fillTop(dyRemaining, 0, y - measuredHeight, measuredWidth, y).let {
                    y -= it
                    dyRemaining -= it
                }
            }
        }

        val filled = Math.min(dy, dy - dyRemaining) // Cap filled distance at dy. Any left over is overdraw.
        layoutState.overdraw = Math.max(0, -dyRemaining) // If dyRemaining is -ve, then overdraw happened.
        layoutState.bottom += filled // Section got taller by the filled amount.
        layoutState.headPosition = currentPos
        return filled
    }

    /**
     * Fill revealed area where content has been scrolled up the screen by dy.
     */
    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: LinearSectionState,
                              layoutState: LayoutState): Int {
        if (layoutState.headPosition < 0) {
            layoutState.headPosition = 0
        }

        // Must fill section children first.
        var filled = 0
        if (layoutState.tailPosition >= 0) {
            helper.getUnfinishedChild(layoutState.tailPosition, section)?.use {
                measure()
                val before = height
                filled += fillBottom(dy, 0, layoutState.bottom - height, measuredWidth, measuredHeight,
                                     helper.numViews - numViews)
                layoutState.bottom += height - before
            }
        }

        // Check to see if we have to handle excess.
        var excess =0
        if (filled == 0) {
            excess = layoutState.bottom - helper.layoutLimit
            excess = if (excess > 0) excess else 0
        }

        // Fill in remaining space
        while (filled + excess < dy && layoutState.tailPosition + 1 < section.numChildren) {
            helper.getChild(layoutState.tailPosition + 1, section)?.use {
                addToRecyclerView()
                measure()
                filled += fillBottom(dy - filled, 0, layoutState.bottom, measuredWidth,
                                     layoutState.bottom + measuredHeight, helper.numViews)
                layoutState.tailPosition += 1
                layoutState.bottom += height
            }
        }

        filled += excess

        return filled
    }

    override fun onTrimTop(scrolled: Int, helper: LayoutHelper, section: LinearSectionState,
                           layoutState: LayoutState): Int {
        var removedHeight = 0
        while (layoutState.headPosition <= layoutState.tailPosition) {
            var childRemoved = false
            helper.getUnfinishedChild(layoutState.headPosition, section)?.use {
                removedHeight += trimTop(scrolled, helper, 0)
                childRemoved = numViews == 0
                // Don't adjust overdraw because section children don't report drawing into the area.
            } ?: helper.getAttachedViewAt(0).let {
                if (helper.getBottom(it) < 0) {
                    helper.removeView(it)
                    removedHeight += Math.max(0, it.height - layoutState.overdraw)
                    layoutState.overdraw = Math.max(0, layoutState.overdraw - it.height)
                    childRemoved = true
                } else if (helper.getTop(it) < 0) {
                    val before = layoutState.overdraw
                    layoutState.overdraw = -helper.getTop(it)
                    removedHeight += layoutState.overdraw - before
                }
            }

            if (childRemoved) {
                layoutState.headPosition += 1
            } else {
                break
            }
        }

        if (helper.numViews == 0) {
            layoutState.headPosition = -1
            layoutState.tailPosition = -1
        }
        layoutState.bottom -= removedHeight
        return removedHeight
    }

    override fun onTrimBottom(scrolled: Int, helper: LayoutHelper, section: LinearSectionState,
                              layoutState: LayoutState): Int {
        var removedHeight = 0
        while (layoutState.tailPosition >= layoutState.headPosition) {
            var doNext = false
            helper.getUnfinishedChild(layoutState.tailPosition, section)?.use {
                removedHeight += trimBottom(scrolled, helper, helper.numViews - numViews)
                doNext = numViews == 0
            } ?: helper.getAttachedViewAt(helper.numViews - 1).let {
                if (helper.getTop(it) > helper.layoutLimit) {
                    removedHeight += it.height
                    helper.removeView(it)
                    doNext = true
                }
            }

            if (doNext) {
                layoutState.tailPosition -= 1
            } else
                break
        }

        if (helper.numViews == 0) {
            layoutState.headPosition = -1
            layoutState.tailPosition = -1
        }
        layoutState.bottom -= removedHeight
        return removedHeight
    }
}
