package com.tonicartos.superslim.internal.layout

import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionConfig
import com.tonicartos.superslim.SectionLayoutManager
import com.tonicartos.superslim.adapter.HeaderStyle
import com.tonicartos.superslim.internal.SectionState

class LinearSectionConfig(gutterStart: Int = SectionConfig.DEFAULT_GUTTER, gutterEnd: Int = SectionConfig.DEFAULT_GUTTER,
                          @HeaderStyle headerStyle: Int = SectionConfig.DEFAULT_HEADER_STYLE) : SectionConfig(gutterStart, gutterEnd, headerStyle) {

    override protected fun onMakeSection(oldState: SectionState?): SectionState = LinearSectionState(this, oldState)

    override protected fun onCopy(): LinearSectionConfig {
        return LinearSectionConfig(gutterStart, gutterEnd, headerStyle)
    }
}

internal class LinearSectionState(val configuration: LinearSectionConfig, oldState: SectionState? = null) : SectionState(configuration, oldState) {
    override fun doLayout(helper: LayoutHelper) {
        LinearSlm.onLayout(helper, this)
    }
}

private object LinearSlm : SectionLayoutManager<LinearSectionState> {
    override fun onLayout(helper: LayoutHelper, section: LinearSectionState) {
        var currentPosition = section.headPosition
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
        section.height = y
        section.tailPosition = currentPosition - 1
    }

    /**
     * Fill revealed area where content has been scrolled down the screen by dy.
     */
    override fun fillTopScrolledArea(dy: Int, helper: LayoutHelper, section: LinearSectionState): Int {
        var y = dy

        var currentPos = section.headPosition - 1
        while (y >= 0 && currentPos >= 0) {
            val child = section.getChildAt(helper, currentPos)
            child.addToRecyclerView(0)
            child.measure()
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight
            child.layout(0, y - childHeight, childWidth, y)

            y -= childHeight
            currentPos -= 1

            child.done()
        }

        section.headPosition = currentPos
        section.height += dy - y

        return dy - y
    }

    /**
     * Fill revealed area where content has been scrolled up the screen by dy.
     */
    override fun fillBottomScrolledArea(dy: Int, helper: LayoutHelper, section: LinearSectionState): Int {
        var y = section.height - dy

        var currentPos = section.tailPosition + 1
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

        section.tailPosition = currentPos
        section.height = y

        return y - (section.height - dy)
    }
}
