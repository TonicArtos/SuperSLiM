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
                          @FooterStyle footerStyle: Int = SectionConfig.DEFAULT_FOOTER_STYLE) :
        SectionConfig(gutterStart, gutterEnd, headerStyle, footerStyle, 0, 0, 0, 0) {
    override fun onMakeSection(oldState: SectionState?): SectionState = LinearSectionState(this, oldState)

    override fun onCopy(): LinearSectionConfig {
        return LinearSectionConfig(gutterStart, gutterEnd, headerStyle, footerStyle)
    }
}

internal class LinearSectionState(configuration: LinearSectionConfig, oldState: SectionState? = null)
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
        layoutState.tailPosition = -1
        var currentPosition = layoutState.headPosition
        var y = -layoutState.overdraw

        while (helper.moreToLayout(currentPosition, section)) {
            helper.getChild(currentPosition, section)?.use { child ->
                child.addToRecyclerView()
                child.measure()
                child.layout(0, y, child.measuredWidth, y + child.measuredHeight, helper.numViews)
                if (helper.isPreLayout && child.isRemoved) {
                    helper.addIgnoredHeight(child.height)
                }
                layoutState.disappearedOrRemovedHeight += child.disappearedHeight
                y += child.height
                helper.filledArea += child.height
                if (child.disappearedHeight < child.height) layoutState.tailPosition = currentPosition
                currentPosition += 1
            } ?: break
        }
        layoutState.bottom = y
        if (layoutState.tailPosition == -1) layoutState.headPosition = -1
//        Log.d("Linear", "${section} --- $layoutState")
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
            helper.getUnfinishedChild(currentPos, section)?.use { child ->
                child.measure()
                // Different from fillBottom because overscroll hides the excess from section height.
                child.fillTop(dyRemaining, 0, y - child.measuredHeight, child.measuredWidth, y).let {
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
            helper.getChild(currentPos, section)?.use { child ->
                child.addToRecyclerView(0)
                child.measure()
                child.fillTop(dyRemaining, 0, y - child.measuredHeight, child.measuredWidth, y).let {
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

        var y = layoutState.bottom
        // Must fill section children first.
        var filled = 0
        if (layoutState.tailPosition >= 0) {
            // Must handle case where not just the trailing child needs updating.
            var stepBackHeight = 0
            var stepBackViews = 0
            for (position in layoutState.tailPosition downTo layoutState.headPosition) {
                helper.getUnfinishedChild(position, section)?.use { child ->
                    child.measure()

                    val before = child.height
                    val fill = child.fillBottom(dy, 0, y - stepBackHeight - child.height, child.measuredWidth, 0,
                                                helper.numViews - stepBackViews - child.numViews)
                    if (position == layoutState.tailPosition) {
                        y += child.height - before
                        filled += fill
                    }

                    stepBackHeight += child.height
                    stepBackViews += child.numViews
                }
                if (y - stepBackHeight <= helper.layoutLimit - helper.stickyEndInset) break
            }
        }

        // Check to see if we have to handle excess.
        val excess = if (layoutState.tailPosition >= 0 && filled == 0) {
            Math.max(0, layoutState.bottom - helper.layoutLimit)
        } else {
            0
        }

        // Fill in remaining space
        while (filled + excess < dy && layoutState.tailPosition + 1 < section.numChildren) {
            helper.getChild(layoutState.tailPosition + 1, section)?.use { child ->
                child.addToRecyclerView()
                child.measure()
                filled += child.fillBottom(dy - filled, 0, y, child.measuredWidth, y + child.measuredHeight,
                                           helper.numViews)
                layoutState.tailPosition += 1
                y += child.height
            }
        }

        filled += excess

        layoutState.bottom = y

        return Math.min(dy, filled)
    }

    override fun onTrimTop(scrolled: Int, helper: LayoutHelper, section: LinearSectionState,
                           layoutState: LayoutState): Int {
        var removedHeight = 0
        while (layoutState.headPosition <= layoutState.tailPosition) {
            var childRemoved = false
            helper.getUnfinishedChild(layoutState.headPosition, section)?.use { child ->
                removedHeight += child.trimTop(scrolled, 0, helper)
                childRemoved = child.numViews == 0
                // Don't adjust overdraw because section children don't report drawing into the area.
            } ?: helper.getAttachedViewAt(0) { child ->
                if (child.bottom < 0) {
                    child.remove()
                    removedHeight += Math.max(0, child.height - layoutState.overdraw)
                    layoutState.overdraw = Math.max(0, layoutState.overdraw - child.height)
                    childRemoved = true
                } else if (child.top < 0) {
                    val before = layoutState.overdraw
                    layoutState.overdraw = -child.top
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
        var y = layoutState.bottom
        // Trim child sections back to before the sticky edge. Remove child views after the limit. Track tail position.
        var stepBackViews = 0
        for (position in layoutState.tailPosition downTo layoutState.headPosition) {
            helper.getUnfinishedChild(position, section)?.use { child ->
                y -= child.height
                removedHeight += child.trimBottom(scrolled, y, helper, helper.numViews - stepBackViews - child.numViews)
                stepBackViews += child.numViews
                if (child.numViews == 0) layoutState.tailPosition -= 1
            } ?: helper.getAttachedViewAt(helper.numViews - 1) { child ->
                y -= child.height
                stepBackViews += 1
                if (child.top > helper.layoutLimit) {
                    removedHeight += child.height
                    child.remove()
                    layoutState.tailPosition -= 1
                    stepBackViews -= 1
                }
            }
            // Go until marker before sticky edge.
            if (y < helper.layoutLimit - helper.stickyEndInset) break
        }

        if (helper.numViews == 0) {
            layoutState.headPosition = -1
            layoutState.tailPosition = -1
        }
        layoutState.bottom -= removedHeight
        return removedHeight
    }
}
