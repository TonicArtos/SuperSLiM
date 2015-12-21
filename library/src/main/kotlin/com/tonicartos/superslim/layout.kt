package com.tonicartos.superslim

import android.view.View
import com.tonicartos.superslim.internal.BaseLayoutHelper
import com.tonicartos.superslim.internal.RootLayoutHelper
import com.tonicartos.superslim.internal.SectionState

interface SectionLayoutManager<T : SectionState> {
    fun onLayout(helper: LayoutHelper, section: T)
    fun fillTopScrolledArea(dy: Int, helper: LayoutHelper, section: T): Int
    fun fillBottomScrolledArea(dy: Int, helper: LayoutHelper, section: T): Int
}

class LayoutHelper private constructor(private var root: RootLayoutHelper) : BaseLayoutHelper by root {
    internal constructor(root: RootLayoutHelper, x: Int, y: Int, width: Int) : this(root) {
        offset.x = x
        offset.y = y
        this.width = width
    }

    /*************************
     * Init stuff
     *************************/

    private var offset = Offset()
    private var width: Int = 0

    internal fun acquireSubsectionHelper(left: Int, top: Int, right: Int): LayoutHelper = root.acquireSubsectionHelper(offset.x + left, offset.y + top, offset.x + right)
    internal fun release() {
        root.releaseSubsectionHelper(this)
    }

    internal fun reInit(root: RootLayoutHelper, x: Int, y: Int, width: Int): LayoutHelper {
        this.root = root
        offset.x = x
        offset.y = y
        this.width = width
        return this
    }

    /*************************
     * layout stuff
     *************************/

    override val layoutWidth: Int
        get() = width
    override val layoutLimit: Int
        get() = root.layoutLimit - offset.y

    override fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int, marginLeft: Int, marginTop: Int, marginRight: Int, marginBottom: Int) {
        root.layout(view, offset.x + left, offset.y + top, offset.x + right, offset.y + bottom, marginLeft, marginTop, marginRight, marginBottom)
    }

    override fun measure(view: View, usedWidth: Int, usedHeight: Int) {
        root.measure(view, usedWidth + root.layoutWidth - width, usedHeight + offset.y)
    }

    var filledArea: Int = 0

    private val willCheckForDisappearedItems = !isPreLayout && willRunPredictiveAnimations && supportsPredictiveItemAnimations

    internal fun getHeader(section: SectionState): Child? {
        if (filledArea >= layoutLimit && willCheckForDisappearedItems && section.hasDisappearedItemsToLayOut) {
            return section.getDisappearingHeader(this)
        } else {
            return section.getHeader(this)
        }
    }

    fun getChild(currentPosition: Int, section: SectionState): Child? {
        if (currentPosition < section.numChildren) {
            if (filledArea >= layoutLimit && willCheckForDisappearedItems && section.hasDisappearedItemsToLayOut) {
                return section.getDisappearingChildAt(this, currentPosition)
            } else {
                return section.getChildAt(this, currentPosition)
            }
        }
        return null
    }

    fun moreToLayout(currentPosition: Int, section: SectionState): Boolean {
        if (currentPosition < section.numChildren) {
            if (filledArea >= layoutLimit) {
                return willCheckForDisappearedItems && section.hasDisappearedItemsToLayOut
            }
            return true
        }
        return false
    }

    private val SectionState.hasDisappearedItemsToLayOut: Boolean get() {
        for (scrap in scrap) {
            if (scrap in this) {
                return true
            }
        }
        return false
    }

    internal fun scrapHasPosition(position: Int): Boolean {
        for (scrap in scrap) {
            if (scrap.layoutPosition == position) {
                return true
            }
        }
        return false
    }

    override fun toString(): String = "SubsectionHelper($offset, width = $width, limit = $layoutLimit, root = \n$root)".replace("\n", "\n\t")

    private data class Offset(var x: Int = 0, var y: Int = 0)
}
