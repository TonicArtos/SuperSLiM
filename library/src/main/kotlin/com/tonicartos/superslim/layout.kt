package com.tonicartos.superslim

import android.view.View
import com.tonicartos.superslim.internal.BaseLayoutHelper
import com.tonicartos.superslim.internal.RootLayoutHelper
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.SectionState.LayoutState

interface SectionLayoutManager<T : SectionState> {
    /**
     * Layout the section. Layout pass may be pre, post, or normal.
     *
     * @param helper Layout helper.
     * @param section The section to lay out.
     * @param layoutState In/out layout state.
     */
    fun onLayout(helper: LayoutHelper, section: T, layoutState: LayoutState)

    /**
     * Fill distance dy at top of the section. The layout state may already contain some filled distance recorded as
     * overdraw.
     *
     * @param dy Distance to fill.
     * @param helper Layout helper.
     * @param section Section to fill.
     * @param layoutState In/out layout state.
     *
     * @return How much of dy left to fill.
     */
    fun onFillTop(dy: Int, helper: LayoutHelper, section: T, layoutState: LayoutState): Int

    /**
     * Fill distance dy at bottom of the section. The layout state may already contain some filled distance recorded as
     * overdraw.
     *
     * @param dy Distance to fill.
     * @param helper Layout helper.
     * @param section Section to fill.
     * @param layoutState In/out layout state.
     */
    fun onFillBottom(dy: Int, helper: LayoutHelper, section: T, layoutState: LayoutState): Int

    fun onTrimTop(helper: LayoutHelper, section: T, layoutState: LayoutState)
    fun onTrimBottom(helper: LayoutHelper, section: T, layoutState: LayoutState)
}

class LayoutHelper private constructor(private var root: RootLayoutHelper) : BaseLayoutHelper by root {
    internal constructor(root: RootLayoutHelper, x: Int, y: Int, width: Int,
                         paddingLeft: Int, paddingTop: Int, paddingRight: Int, paddingBottom: Int,
                         viewsBefore: Int, layoutState: LayoutState) : this(root) {
        offset.x = x
        offset.y = y
        this.width = width
        this.paddingLeft = paddingLeft
        this.paddingTop = paddingTop
        this.paddingRight = paddingRight
        this.paddingBottom = paddingBottom
        this.viewsBefore = viewsBefore
        this.layoutState = layoutState
    }

    /*************************
     * Init stuff
     *************************/

    private var offset = Offset()
    private var width = 0

    internal var paddingLeft = 0
        private set
    internal var paddingTop = 0
        private set
    internal var paddingRight = 0
        private set
    internal var paddingBottom = 0
        private set

    private var viewsBefore = 0
    private lateinit var layoutState: LayoutState
    var numViews: Int
        get() = layoutState.numViews
        set(value) {
            layoutState.numViews = value
        }

    internal fun acquireSubsectionHelper(y: Int, left: Int, right: Int,
                                         paddingLeft: Int, paddingTop: Int, paddingRight: Int, paddingBottom: Int,
                                         viewsBefore: Int, layoutState: LayoutState): LayoutHelper =
            root.acquireSubsectionHelper(offset.y + y, offset.x + left, offset.x + right, paddingLeft, paddingTop, paddingRight, paddingBottom, viewsBefore, layoutState)

    internal fun release() {
        root.releaseSubsectionHelper(this)
    }

    internal fun reInit(root: RootLayoutHelper, x: Int, y: Int,
                        paddingLeft: Int, paddingTop: Int, paddingRight: Int, paddingBottom: Int,
                        width: Int, viewsBefore: Int, layoutState: LayoutState): LayoutHelper {
        this.root = root
        offset.x = x
        offset.y = y
        this.width = width
        filledArea = 0
        this.paddingLeft = paddingLeft
        this.paddingTop = paddingTop
        this.paddingRight = paddingRight
        this.paddingBottom = paddingBottom
        this.viewsBefore = viewsBefore
        this.layoutState = layoutState
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

    override fun addView(child: View) {
        root.addView(child)
        numViews += 1
    }

    override fun addView(child: View, index: Int) {
        if (index == -1) return addView(child)
        root.addView(child, viewsBefore + index)
        numViews += 1
    }

    override fun addDisappearingView(child: View) {
        root.addDisappearingView(child)
        numViews += 1
    }

    override fun addDisappearingView(child: View, index: Int) {
        if (index == -1) return addDisappearingView(child)
        root.addDisappearingView(child, viewsBefore + index)
        numViews += 1
    }

    override fun attachViewToPosition(position: Int, view: View) {
        numViews += 1
        root.attachViewToPosition(viewsBefore + position, view)
    }

    override fun detachViewAtPosition(position: Int): View {
        numViews -= 1
        return root.detachViewAtPosition(viewsBefore + position)
    }

    override fun toString(): String = "SubsectionHelper($offset, width = $width, limit = $layoutLimit, root = \n$root)".replace("\n", "\n\t")

    private data class Offset(var x: Int = 0, var y: Int = 0)
}
