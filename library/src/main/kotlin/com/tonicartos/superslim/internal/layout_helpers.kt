package com.tonicartos.superslim.internal

import android.support.v7.widget.RecyclerView
import android.view.View
import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.SectionConfig
import com.tonicartos.superslim.internal.SectionState.LayoutState

internal class RootLayoutHelper(val manager: ManagerHelper, val config: ReadWriteLayoutHelper,
                                val recycler: RecyclerHelper, val state: StateHelper) :
        BaseLayoutHelper, ManagerHelper by manager, ReadWriteLayoutHelper by config, RecyclerHelper by recycler,
        StateHelper by state {
    companion object {
        private var helperPool = LayoutHelperPool()
    }

    internal fun acquireSubsectionHelper(y: Int, left: Int, right: Int, paddingTop: Int, paddingBottom: Int,
                                         viewsBefore: Int, layoutState: LayoutState,
                                         tellParentViewsChangedBy: (Int) -> Unit,
                                         tellParentAboutTemporaryView: (Int) -> Unit)
            = helperPool.acquire(this, left, y, right - left, paddingTop, paddingBottom, viewsBefore, layoutState,
                                 tellParentViewsChangedBy, tellParentAboutTemporaryView)

    inline fun <T> useSubsectionHelper(y: Int, left: Int, right: Int, paddingTop: Int, paddingBottom: Int,
                                       viewsBefore: Int, layoutState: LayoutState, block: (LayoutHelper) -> T): T {
        val helper = acquireSubsectionHelper(y, left, right, paddingTop, paddingBottom, viewsBefore, layoutState,
                                             {}, {})
        val r = block(helper)
        helper.release()
        return r
    }

    fun releaseSubsectionHelper(helper: LayoutHelper) {
        helperPool.release(helper)
    }

    private var layoutLimitExtension = 0
    override val layoutLimit get() = config.layoutLimit + layoutLimitExtension

    override fun addIgnoredHeight(ignoredHeight: Int) {
        layoutLimitExtension += ignoredHeight
    }

    override var stickyStartInset = 0
    override var stickyEndInset = 0

    override fun toString(): String = "RootHelper(ignoredHeight = $layoutLimitExtension, layoutLimit = $layoutLimit, layoutWidth = $layoutWidth, \nconfig = $config,\nstate = $state)\n"
            .replace("\n", "\n\t")

    private class LayoutHelperPool {
        private val pool = arrayListOf<LayoutHelper>()

        fun acquire(root: RootLayoutHelper, x: Int, y: Int, width: Int, paddingTop: Int, paddingBottom: Int,
                    viewsBefore: Int, layoutState: LayoutState, tellParentViewsChangedBy: (Int) -> Unit,
                    tellParentAboutTemporaryView: (Int) -> Unit) =
                if (pool.isEmpty()) {
                    LayoutHelper(root, x, y, width, paddingTop, paddingBottom, viewsBefore, layoutState,
                                 tellParentViewsChangedBy, tellParentAboutTemporaryView)
                } else {
                    pool.removeAt(0).reInit(root, x, y, width, paddingTop, paddingBottom, viewsBefore, layoutState,
                                            tellParentViewsChangedBy, tellParentAboutTemporaryView)
                }

        fun release(helper: LayoutHelper) {
            pool.add(helper)
        }
    }
}

/**
 * Encapsulates the recycler into the layout helper chain.
 */
internal class RecyclerWrapper : RecyclerHelper {
    lateinit var recycler: RecyclerView.Recycler

    fun wrap(recycler: RecyclerView.Recycler): RecyclerWrapper {
        this.recycler = recycler
        return this
    }

    override fun getView(position: Int): View = recycler.getViewForPosition(position)

    override val scrap: List<RecyclerView.ViewHolder>
        get() = recycler.scrapList

    override fun removeView(child: View, helper: LayoutHelper) {
        helper.removeView(child, recycler)
    }
}

/**
 * Encapsulates the recycler view state into the layout helper chain.
 */
internal class StateWrapper : StateHelper {
    lateinit var state: RecyclerView.State

    fun wrap(state: RecyclerView.State): StateWrapper {
        this.state = state
        return this
    }

    override val hasTargetScrollPosition: Boolean
        get() = state.hasTargetScrollPosition()

    override val targetScrollPosition: Int
        get() = state.targetScrollPosition

    override val willRunPredictiveAnimations: Boolean
        get() = state.willRunPredictiveAnimations()

    override val isPreLayout: Boolean
        get() = state.isPreLayout

    override val itemCount: Int
        get() = state.itemCount

    override fun toString(): String = "State(itemCount = $itemCount, isPreLayout = $isPreLayout, willRunPredictiveAnimations = $willRunPredictiveAnimations)"
}

/****************************************************
 * This is the set of interfaces which collectively
 * express the hierarchy of delegates which make up
 * the bulk of [LayoutHelper]'s functionality.
 ****************************************************/

internal interface BaseLayoutHelper : ManagerHelper, StateHelper, RecyclerHelper, ReadWriteLayoutHelper {
    fun addIgnoredHeight(ignoredHeight: Int)
}

internal interface ManagerHelper {
    fun addView(child: View)
    fun addView(child: View, index: Int)
    fun addDisappearingView(child: View)
    fun addDisappearingView(child: View, index: Int)
    fun addTemporaryView(child: View)
    fun addTemporaryView(child: View, index: Int)

    fun removeView(child: View, recycler: RecyclerView.Recycler)

    val supportsPredictiveItemAnimations: Boolean
}

internal interface StateHelper {
    val isPreLayout: Boolean
    val willRunPredictiveAnimations: Boolean
    val itemCount: Int
    val hasTargetScrollPosition: Boolean
    val targetScrollPosition: Int
}

internal interface RecyclerHelper {
    fun getView(position: Int): View
    val scrap: List<RecyclerView.ViewHolder>
    fun removeView(child: View, helper: LayoutHelper)
}

internal interface ReadWriteLayoutHelper : ReadLayoutHelper, WriteLayoutHelper {
    val basePaddingLeft: Int
    val basePaddingTop: Int
    val basePaddingRight: Int
    val basePaddingBottom: Int

    fun attachViewToPosition(position: Int, view: View)
    fun detachViewAtPosition(position: Int): View?

    fun getTransformedPaddingLeft(sectionConfig: SectionConfig): Int
    fun getTransformedPaddingTop(sectionConfig: SectionConfig): Int
    fun getTransformedPaddingRight(sectionConfig: SectionConfig): Int
    fun getTransformedPaddingBottom(sectionConfig: SectionConfig): Int
}

class AttachedView private constructor(private var view: View, private var helper: LayoutHelper) {
    companion object {
        private val pool = ArrayList<AttachedView>()
        internal fun wrap(view: View, helper: LayoutHelper) = if (pool.size >= 1) pool.removeAt(0).apply {
            this.view = view
            this.helper = helper
        } else AttachedView(view, helper)
    }

    fun done() {
        pool.add(this)
    }

    val left get() = helper.getLeft(this.view)
    val top get() = helper.getTop(this.view)
    val right get() = helper.getRight(this.view)
    val bottom get() = helper.getBottom(this.view)
    val width get() = helper.getMeasuredWidth(this.view)
    val height get() = helper.getMeasuredHeight(this.view)

    fun remove() = helper.removeView(this.view, helper)
    fun offsetTopAndBottom(offset: Int) = helper.offsetVertical(this.view, offset)
    fun offsetLeftAndRight(offset: Int) = helper.offsetHorizontal(this.view, offset)
}

internal interface ReadLayoutHelper {
    fun getLeft(child: View): Int
    fun getTop(child: View): Int
    fun getRight(child: View): Int
    fun getBottom(child: View): Int
    fun getMeasuredWidth(child: View): Int
    fun getMeasuredHeight(child: View): Int

    fun getAttachedRawView(position: Int): View

    /**
     * Width of the layout area.
     */
    val layoutWidth: Int

    /**
     * Y limit that constrains the layout. This is used to know when to stop laying out items, and is nominally the
     * maximum height of the visible layout area.
     *
     * **Warning**: This value can change, and as such, should not be stored.
     */
    val layoutLimit: Int
    var stickyStartInset: Int
    var stickyEndInset: Int
}

internal inline fun <R> ReadLayoutHelper.insetStickyStart(inset: Int, block: () -> R): R {
    stickyStartInset += inset
    val r = block()
    stickyStartInset -= inset
    return r
}

internal inline fun <R> ReadLayoutHelper.insetStickyEnd(inset: Int, block: () -> R): R {
    stickyEndInset += inset
    val r = block()
    stickyEndInset -= inset
    return r
}

internal interface WriteLayoutHelper {
    fun measure(view: View, usedWidth: Int = 0, usedHeight: Int = 0)
    fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int, marginLeft: Int = 0, marginTop: Int = 0,
               marginRight: Int = 0, marginBottom: Int = 0)

    fun offsetVertical(view: View, dy: Int)
    fun offsetHorizontal(view: View, dx: Int)
    fun offsetChildrenVertical(dy: Int)
    fun offsetChildrenHorizontal(dx: Int)
}

