package com.tonicartos.superslim.internal.layout

import android.util.Log
import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionConfig
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.adapter.HeaderStyle
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.SectionState.LayoutState

class LinearSectionConfig(gutterStart: Int = SectionConfig.DEFAULT_GUTTER, gutterEnd: Int = SectionConfig.DEFAULT_GUTTER,
                          @HeaderStyle headerStyle: Int = SectionConfig.DEFAULT_HEADER_STYLE) : SectionConfig(gutterStart, gutterEnd, headerStyle) {

    override protected fun onMakeSection(oldState: SectionState?): SectionState = LinearSectionState(this, oldState)

    override protected fun onCopy(): LinearSectionConfig {
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
    override fun onLayout(helper: LayoutHelper, section: LinearSectionState, state: LayoutState) {
        Log.d("linear", "Laying out section ${section.positionInAdapter} with height limit ${helper.layoutLimit}")
        var currentPosition = state.headPosition
        var y = -state.overdraw

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
        state.bottom = y
        state.tailPosition = currentPosition - 1
    }

    /**
     * Fill revealed area where content has been scrolled down the screen by dy.
     */
    override fun onFillTop(dy: Int, helper: LayoutHelper, section: LinearSectionState, layoutState: LayoutState): Int {
        Log.d("LinearSLM", "section = $section")
        var y = layoutState.overdraw

        var currentPos = layoutState.headPosition

        if (currentPos < section.numChildren) {
            // Cascade fill top to current position if valid.
            section.getNonFinalChildAt(helper, currentPos)?.let {
                it.measure()
                y = -it.fillTop(dy, 0, it.measuredWidth)
            }
        }

        // Move on to remaining positions.
        while (y > dy && currentPos > 0) {
            currentPos -= 1
            val child = section.getChildAt(helper, currentPos)
            child.addToRecyclerView(0)
            child.measure()
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight
            child.fillTop(dy, 0, y - childHeight, childWidth, y)

            y -= child.height

            child.done()
        }

        layoutState.headPosition = currentPos
        layoutState.bottom -= y
        layoutState.overdraw = y - dy

        return -y
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
