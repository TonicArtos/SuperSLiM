package com.tonicartos.superslim.internal.layout

import android.util.Log
import com.tonicartos.superslim.*
import com.tonicartos.superslim.adapter.HeaderStyle
import com.tonicartos.superslim.internal.SectionState

class LinearSectionConfig(gutterStart: Int = SectionConfig.DEFAULT_GUTTER, gutterEnd: Int = SectionConfig.DEFAULT_GUTTER,
                          @HeaderStyle headerStyle: Int = SectionConfig.DEFAULT_HEADER_STYLE) : SectionConfig(gutterStart, gutterEnd, headerStyle) {

    override protected fun onMakeSection(oldState: SectionState?): SectionState = LinearSectionState(this, oldState)

    override protected fun onCopy(): LinearSectionConfig {
        return LinearSectionConfig(gutterStart, gutterEnd, headerStyle)
    }
}

internal open class LinearSectionState(val configuration: LinearSectionConfig, oldState: SectionState? = null) : SectionState(configuration, oldState) {
    override fun doLayout(helper: LayoutHelper) = LinearSlm.onLayout(helper, this)
    override fun doFillTop(dy: Int, helper: LayoutHelper) = LinearSlm.onFillTop(dy, helper, this)
    override fun doFillBottom(dy: Int, helper: LayoutHelper) = LinearSlm.onFillTop(dy, helper, this)
    override fun doTrimTop(helper: LayoutHelper) = LinearSlm.onTrimTop(helper, this)
    override fun doTrimBottom(helper: LayoutHelper) = LinearSlm.onTrimBottom(helper, this)
}

private object LinearSlm : SectionLayoutManager<LinearSectionState> {
    override fun onLayout(helper: LayoutHelper, section: LinearSectionState) {
        var currentPosition = section.layout.headPosition
        var y = 0

        while (helper.moreToLayout(currentPosition, section)) {
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
            currentPosition += 1

            child.done()
        }
        section.layout.height = y
        section.layout.tailPosition = currentPosition - 1
    }

    /**
     * Fill revealed area where content has been scrolled down the screen by dy.
     */
    override fun onFillTop(dy: Int, helper: LayoutHelper, section: LinearSectionState): Int {
        Log.d("LinearSLM", "section = $section")
        var y = section.layout.overdraw

        var currentPos = section.layout.headPosition

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

        section.layout.headPosition = currentPos
        section.layout.height -= y
        section.layout.overdraw = y - dy

        return -y
    }

    /**
     * Fill revealed area where content has been scrolled up the screen by dy.
     */
    override fun onFillBottom(dy: Int, helper: LayoutHelper, section: LinearSectionState): Int {
        var y = section.layout.height - dy

        var currentPos = section.layout.tailPosition + 1
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

        section.layout.tailPosition = currentPos
        section.layout.height = y

        return y - (section.layout.height - dy)
    }

    override fun onTrimTop(helper: LayoutHelper, section: LinearSectionState) {
    }

    override fun onTrimBottom(helper: LayoutHelper, section: LinearSectionState) {
    }
}
