package com.tonicartos.superslim.internal.layout

import android.util.Log
import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionConfig
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.adapter.HeaderStyle
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.SectionState.LayoutState

class LinearSectionConfig(gutterStart: Int = SectionConfig.DEFAULT_GUTTER, gutterEnd: Int = SectionConfig.DEFAULT_GUTTER,
                          @HeaderStyle headerStyle: Int = SectionConfig.DEFAULT_HEADER_STYLE,
                          paddingStart: Int = 0, paddingTop: Int = 0, paddingEnd: Int = 0, paddingBottom: Int = 0) :
        SectionConfig(gutterStart, gutterEnd, headerStyle, paddingStart, paddingTop, paddingEnd, paddingBottom) {

    override fun onMakeSection(oldState: SectionState?): SectionState = LinearSectionState(this, oldState)

    override fun onCopy(): LinearSectionConfig {
        return LinearSectionConfig(gutterStart, gutterEnd, headerStyle)
    }
}

internal open class LinearSectionState(val configuration: LinearSectionConfig, oldState: SectionState? = null) : SectionState(configuration, oldState) {
    override fun doLayout(helper: LayoutHelper, layoutState: LayoutState) = LinearSlm.onLayout(helper, this, layoutState)
    override fun doFillTop(dy: Int, helper: LayoutHelper, layoutState: LayoutState): Int = LinearSlm.onFillTop(dy, helper, this, layoutState)
    override fun doFillBottom(dy: Int, helper: LayoutHelper, layoutState: LayoutState): Int = LinearSlm.onFillTop(dy, helper, this, layoutState)
    override fun doTrimTop(helper: LayoutHelper, layoutState: LayoutState) = LinearSlm.onTrimTop(helper, this, layoutState)
    override fun doTrimBottom(helper: LayoutHelper, layoutState: LayoutState) = LinearSlm.onTrimBottom(helper, this, layoutState)
}

private object LinearSlm : SectionLayoutManager<LinearSectionState> {
    override fun onLayout(helper: LayoutHelper, section: LinearSectionState, layoutState: LayoutState) {
        Log.d("linear", "Laying out section ${section.positionInAdapter} with height limit ${helper.layoutLimit}")
        var currentPosition = layoutState.headPosition
        var y = -layoutState.overdraw

        while (helper.moreToLayout(currentPosition, section)) {
            Log.d("linear", "Laying out child $currentPosition")
            val child = helper.getChild(currentPosition, section) ?: break
            child.addToRecyclerView()
            child.measure()
            child.layout(0, y, child.measuredWidth, y + child.measuredHeight)

            val childHeight = child.height
            if (helper.isPreLayout && child.isRemoved) {
                helper.addIgnoredHeight(child.height)
            }
            y += childHeight
            helper.filledArea += childHeight

            child.done()
            Log.d("linear", "Laid out child $currentPosition with height $childHeight")
            currentPosition += 1
        }
        Log.d("linear", "$y")
        layoutState.bottom = y
        layoutState.tailPosition = currentPosition - 1
    }

    override fun onFillTop(dy: Int, helper: LayoutHelper, section: LinearSectionState, layoutState: LayoutState): Int {
        Log.d("LinearSlm", "section = $section, headPosition = ${layoutState.headPosition}, overdraw = ${layoutState.overdraw}")

        // How much distance left to fill.
        var dyRemaining = dy - layoutState.overdraw
        if (dyRemaining <= 0) {
            layoutState.overdraw -= dy
            Log.d("LinearSlm", "Nothing todo: dyRemaining = $dyRemaining, overdraw = ${layoutState.overdraw}")
            return dy
        }
        // Where we are filling at.
        var y = -layoutState.overdraw
        Log.d("LinearSlm", "Before: y = $y, dyRemaining = $dyRemaining")

        var currentPos = layoutState.headPosition

        // Try filling the current child. Ignore if off the bottom.
        if (currentPos < section.numChildren) {
            // Only try to fill a non-final child (subsection).
            section.getNonFinalChildAt(helper, currentPos)?.let {
                it.measure()
                val filled = it.fillTop(dyRemaining, 0, y - it.measuredHeight, it.measuredWidth, y)
                y -= filled
                dyRemaining -= filled
                Log.d("LinearSlm", "After non-final child: filled = $filled, y = $y, dyRemaining = $dyRemaining")
            }
        } else {
            Log.d("LinearSlm", "Started from bottom. *************************************************")
        }

        // Fill remaining dy with remaining content.
        while (dyRemaining > 0 && currentPos > 0) {
            currentPos -= 1
            section.getChildAt(helper, currentPos).let {
                it.addToRecyclerView(0)
                it.measure()
                Log.d("LinearSlm", "Fill top with child $currentPos, top = ${y - it.measuredHeight}, bottom = ${it.measuredHeight}")
                val filled = it.fillTop(dyRemaining, 0, y - it.measuredHeight, it.measuredWidth, y)
                y -= filled
                dyRemaining -= filled
                Log.d("LinearSlm", "After child: filled = $filled, y = $y, childHeight = ${it.measuredHeight}, dyRemaining = $dyRemaining")

                it.done()
            }
        }
        // Note that currentPos is left at the last child filled in the loop, which will be the new head position.

        val filled = Math.min(dy, dy - dyRemaining) // Cap filled distance at dy. Any left over is overdraw.
        layoutState.overdraw = Math.max(0, -dyRemaining) // If dyRemaining is -ve, then overdraw happened.
        layoutState.bottom += filled // Section got taller by the filled amount.
        layoutState.headPosition = currentPos
        Log.d("LinearSlm", "Finished: filled = $filled, dyRemaining = $dyRemaining, overdraw = ${layoutState.overdraw}")
        return filled
    }

    /**
     * Fill revealed area where content has been scrolled up the screen by dy.
     */
    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: LinearSectionState, layoutState: LayoutState): Int {
        var y = layoutState.bottom - dy

        var currentPos = layoutState.tailPosition + 1
        while (y < helper.layoutLimit && currentPos < section.numChildren) {
            val child = section.getChildAt(helper, currentPos)
            child.addToRecyclerView(0)
            child.measure()
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight
            child.layout(0, y, childWidth, y + childHeight)
            y += childHeight
            currentPos += 1

            child.done()
        }

        layoutState.tailPosition = currentPos
        layoutState.bottom = y

        return y - (layoutState.bottom - dy)
    }

    override fun onTrimTop(helper: LayoutHelper, section: LinearSectionState, layoutState: LayoutState) {
    }

    override fun onTrimBottom(helper: LayoutHelper, section: LinearSectionState, layoutState: LayoutState) {
    }
}
