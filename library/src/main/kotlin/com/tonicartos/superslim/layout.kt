package com.tonicartos.superslim

import android.support.v7.widget.RecyclerView
import android.view.View
import com.tonicartos.superslim.internal.AttachedView
import com.tonicartos.superslim.internal.BaseLayoutHelper
import com.tonicartos.superslim.internal.RootLayoutHelper
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.SectionState.LayoutState

interface SectionLayoutManager<in T : SectionState> {

    /**
     * Check if the section is at the top.
     */
    fun isAtTop(section: T, layoutState: LayoutState): Boolean

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
     * @return How much filled. Valid values are 0 to dy.
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
     *
     * @return How much filled. Valid values are 0 to dy.
     */
    fun onFillBottom(dy: Int, helper: LayoutHelper, section: T, layoutState: LayoutState): Int

    /**
     * Remove views managed by this SectionLayoutManager that are before 0. Remember to update layoutState values accordingly.
     *
     * @return Height removed from section.
     */
    fun onTrimTop(scrolled: Int, helper: LayoutHelper, section: T, layoutState: LayoutState): Int

    /**
     * Remove views managed by this SectionLayoutManager that are after [LayoutHelper.layoutLimit]. Remember to update
     * state values appropriately.
     *
     * @return Height removed from section.
     */
    fun onTrimBottom(scrolled: Int, helper: LayoutHelper, section: T, layoutState: LayoutState): Int
}

class LayoutHelper private constructor(private var root: RootLayoutHelper,
                                       private var tellParentViewsChangedBy: (Int) -> Unit,
                                       private var tellParentAboutTemporaryView: (Int) -> Unit) : BaseLayoutHelper {
    internal constructor(root: RootLayoutHelper, x: Int, y: Int, width: Int, paddingTop: Int, paddingBottom: Int,
                         viewsBefore: Int, layoutState: LayoutState, tellParentViewsChanged: (Int) -> Unit,
                         tellParentAboutTemporaryView: (Int) -> Unit) :
            this(root, tellParentViewsChanged, tellParentAboutTemporaryView) {
        offset.x = x
        offset.y = y
        this.width = width
        this.paddingTop = paddingTop
        this.paddingBottom = paddingBottom
        this.viewsBefore = viewsBefore
        this.layoutState = layoutState
    }

    /*************************
     * Init stuff
     *************************/

    internal fun reInit(root: RootLayoutHelper, x: Int, y: Int, width: Int, paddingTop: Int, paddingBottom: Int,
                        viewsBefore: Int, layoutState: LayoutState, tellParentViewsChangedBy: (Int) -> Unit,
                        tellParentAboutDisappearedView: (Int) -> Unit)
            : LayoutHelper {
        this.root = root
        offset.x = x
        offset.y = y
        this.width = width
        filledArea = 0
        this.paddingTop = paddingTop
        this.paddingBottom = paddingBottom
        this.viewsBefore = viewsBefore
        this.layoutState = layoutState
        this.tellParentViewsChangedBy = tellParentViewsChangedBy
        this.tellParentAboutTemporaryView = tellParentAboutDisappearedView
        return this
    }

    private fun acquireSubsectionHelper(y: Int, left: Int, right: Int, paddingTop: Int, paddingBottom: Int,
                                        viewsBefore: Int, layoutState: LayoutState): LayoutHelper
            = root.acquireSubsectionHelper(offset.y + y, offset.x + left, offset.x + right,
                                           paddingTop, paddingBottom, viewsBefore, layoutState,
                                           this::viewsChangedBy, this::temporaryViewsChangedBy)

    internal fun release() {
        root.releaseSubsectionHelper(this)
    }

    internal inline fun <T> useSubsectionHelper(y: Int, left: Int, right: Int, paddingTop: Int, paddingBottom: Int,
                                                viewsBefore: Int, layoutState: LayoutState,
                                                block: (LayoutHelper) -> T): T {
        val helper = acquireSubsectionHelper(y, left, right, paddingTop, paddingBottom, viewsBefore, layoutState)
        val r = block(helper)
        helper.release()
        return r
    }

    /*************************
     * layout stuff
     *************************/

    private var offset = Offset()
    private var width = 0

    internal var paddingTop = 0
        private set
    internal var paddingBottom = 0
        private set

    internal var viewsBefore = 0
        private set

    private lateinit var layoutState: LayoutState

    var numViews get() = layoutState.numViews
        private set(value) {
            viewsChangedBy(value - layoutState.numViews)
        }

    var numTemporaryViews get() = layoutState.numTemporaryViews
        private set(value) {
            temporaryViewsChangedBy(value - layoutState.numTemporaryViews)
        }

    internal fun viewsChangedBy(delta: Int) {
        layoutState.numViews += delta
        tellParentViewsChangedBy(delta)
    }

    private fun temporaryViewsChangedBy(delta: Int) {
        layoutState.numViews += delta
        layoutState.numTemporaryViews += delta
        tellParentAboutTemporaryView(delta)
    }

    override val layoutWidth get() = width
    override val layoutLimit get() = root.layoutLimit - offset.y

    override fun getLeft(child: View): Int = root.getLeft(child) - offset.x
    override fun getRight(child: View): Int = root.getRight(child) - offset.x
    override fun getTop(child: View): Int = root.getTop(child) - offset.y
    override fun getBottom(child: View): Int = root.getBottom(child) - offset.y

    override fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int,
                        marginLeft: Int, marginTop: Int, marginRight: Int, marginBottom: Int) {
        root.layout(view, offset.x + left, offset.y + top, offset.x + right, offset.y + bottom,
                    marginLeft, marginTop, marginRight, marginBottom)
    }

    override fun measure(view: View, usedWidth: Int, usedHeight: Int) {
        root.measure(view, usedWidth + root.layoutWidth - width, usedHeight + offset.y)
    }

    var filledArea: Int = 0

    private val willCheckForDisappearedItems get() = !isPreLayout && willRunPredictiveAnimations && supportsPredictiveItemAnimations

    internal fun getHeader(section: SectionState): Child? {
        return if (filledArea >= layoutLimit && willCheckForDisappearedItems && section.hasDisappearedItemsToLayOut) {
            section.getDisappearingHeader(this)
        } else {
            section.getHeader(this)
        }
    }

    internal fun getFooter(section: SectionState): Child? {
        return if (filledArea >= layoutLimit && willCheckForDisappearedItems && section.hasDisappearedItemsToLayOut) {
            section.getDisappearingFooter(this)
        } else {
            section.getFooter(this)
        }
    }

    fun getUnfinishedChild(position: Int, section: SectionState) = section.getNonFinalChildAt(this, position)

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
        return scrap.any { it in this }
    }

    internal fun scrapHasPosition(position: Int): Boolean {
        return scrap.any { it.layoutPosition == position }
    }

    override fun getView(position: Int) = root.getView(position)

    override fun removeView(child: View, recycler: RecyclerView.Recycler) {
        root.removeView(child, recycler)
        numViews -= 1
    }

    override fun addView(child: View) {
        addView(child, -1)
    }

    override fun addView(child: View, index: Int) {
        val dest = if (index == -1) numViews else index
        root.addView(child, viewsBefore + dest)
        numViews += 1
    }

    override fun addDisappearingView(child: View) {
        addDisappearingView(child, -1)
    }

    override fun addDisappearingView(child: View, index: Int) {
        val dest = if (index == -1) numViews else index
        root.addDisappearingView(child, viewsBefore + dest)
        numTemporaryViews += 1
    }

    override fun addTemporaryView(child: View) {
        addTemporaryView(child, -1)
    }

    override fun addTemporaryView(child: View, index: Int) {
        val dest = if (index == -1) numViews else index
        root.addTemporaryView(child, viewsBefore + dest)
        numTemporaryViews += 1
    }

    override fun getAttachedRawView(position: Int) = root.getAttachedRawView(viewsBefore + position)

    fun getAttachedViewAt(position: Int) = AttachedView.wrap(getAttachedRawView(position), this)
    inline fun <R> getAttachedViewAt(position: Int, block: (AttachedView) -> R): R
            = getAttachedViewAt(position).let { view -> block(view).also { view.done() } }

    override fun attachViewToPosition(position: Int, view: View) {
        numViews += 1
        root.attachViewToPosition(viewsBefore + position, view)
    }

    override fun detachViewAtPosition(position: Int): View? {
        numViews -= 1
        return root.detachViewAtPosition(viewsBefore + position)
    }

    override fun toString(): String = "SubsectionHelper($offset, width = $width, limit = $layoutLimit, views before = $viewsBefore, root = \n$root)".replace(
            "\n", "\n\t")

    private data class Offset(var x: Int = 0, var y: Int = 0)

    /********************************************************
     * Delegated stuff
     *******************************************************/
    override fun addIgnoredHeight(ignoredHeight: Int) {
        root.addIgnoredHeight(ignoredHeight)
        layoutState.disappearedOrRemovedHeight += ignoredHeight
    }

    override val supportsPredictiveItemAnimations get() = root.supportsPredictiveItemAnimations
    override val isPreLayout: Boolean get() = root.isPreLayout
    override val willRunPredictiveAnimations: Boolean get() = root.willRunPredictiveAnimations
    override val itemCount get() = root.itemCount
    override val hasTargetScrollPosition get() = root.hasTargetScrollPosition
    override val targetScrollPosition get() = root.targetScrollPosition
    override val scrap get() = root.scrap
    override val basePaddingLeft get() = root.basePaddingLeft
    override val basePaddingTop get() = root.basePaddingTop
    override val basePaddingRight get() = root.basePaddingRight
    override val basePaddingBottom get() = root.basePaddingBottom
    override fun getTransformedPaddingLeft(sectionConfig: SectionConfig) = root.getTransformedPaddingLeft(sectionConfig)
    override fun getTransformedPaddingTop(sectionConfig: SectionConfig) = root.getTransformedPaddingTop(sectionConfig)
    override fun getTransformedPaddingRight(sectionConfig: SectionConfig)
            = root.getTransformedPaddingRight(sectionConfig)

    override fun getTransformedPaddingBottom(sectionConfig: SectionConfig)
            = root.getTransformedPaddingBottom(sectionConfig)

    override fun getMeasuredWidth(child: View): Int = root.getMeasuredWidth(child)
    override fun getMeasuredHeight(child: View): Int = root.getMeasuredHeight(child)
    override var stickyStartInset get() = root.stickyStartInset
        set(value) {
            root.stickyStartInset = value
        }
    override var stickyEndInset get() = root.stickyEndInset
        set(value) {
            root.stickyEndInset = value
        }

    override fun offsetChildrenVertical(dy: Int) = root.offsetChildrenVertical(dy)
    override fun offsetChildrenHorizontal(dx: Int) = root.offsetChildrenHorizontal(dx)
    override fun offsetHorizontal(view: View, dx: Int) = root.offsetHorizontal(view, dx)
    override fun offsetVertical(view: View, dy: Int) = root.offsetVertical(view, dy)
    override fun removeView(child: View, helper: LayoutHelper) = root.removeView(child, helper)
}
