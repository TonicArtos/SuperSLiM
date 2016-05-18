package com.tonicartos.superslim.internal

import android.support.v7.widget.RecyclerView
import android.view.View
import com.tonicartos.superslim.LayoutHelper
import com.tonicartos.superslim.internal.SectionState.LayoutState

internal class RootLayoutHelper(val manager: ManagerHelper, val config: ReadWriteLayoutHelper,
                                val recycler: RecyclerHelper, val state: StateHelper) : BaseLayoutHelper,
                                                                                        ManagerHelper by manager, ReadWriteLayoutHelper by config,
                                                                                        RecyclerHelper by recycler, StateHelper by state {
    private var helperPool = LayoutHelperPool()

    fun acquireSubsectionHelper(y: Int, left: Int, right: Int, viewsBefore: Int, layoutState: LayoutState): LayoutHelper = helperPool.acquire(this, left, y, right - left, viewsBefore, layoutState)
    fun releaseSubsectionHelper(helper: LayoutHelper) {
        helperPool.release(helper)
    }

    private var layoutLimitExtension: Int = 0
    override val layoutLimit: Int
        get() = config.layoutLimit + layoutLimitExtension

    override fun addIgnoredHeight(ignoredHeight: Int) {
        layoutLimitExtension += ignoredHeight
    }

    override fun toString(): String = "RootHelper(ignoredHeight = $layoutLimitExtension, layoutLimit = $layoutLimit, layoutWidth = $layoutWidth, \nconfig = $config,\nstate = $state)\n".replace("\n", "\n\t")

    private class LayoutHelperPool {
        private val pool = arrayListOf<LayoutHelper>()

        fun acquire(root: RootLayoutHelper, x: Int, y: Int, width: Int, viewsBefore: Int, layoutState: LayoutState) =
                if (pool.isEmpty()) {
                    LayoutHelper(root, x, y, width, viewsBefore, layoutState)
                } else {
                    pool.removeAt(0).reInit(root, x, y, width, viewsBefore, layoutState)
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
}

internal interface ReadWriteLayoutHelper : ReadLayoutHelper, WriteLayoutHelper {
    val basePaddingLeft: Int
    val basePaddingTop: Int
    val basePaddingRight: Int
    val basePaddingBottom: Int

    fun attachViewToPosition(position: Int, view: View)
    fun detachViewAtPosition(position: Int): View
}

internal interface ReadLayoutHelper {
    fun getLeft(child: View): Int
    fun getTop(child: View): Int
    fun getRight(child: View): Int
    fun getBottom(child: View): Int
    fun getMeasuredWidth(child: View): Int
    fun getMeasuredHeight(child: View): Int

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
}

internal interface WriteLayoutHelper {
    fun measure(view: View, usedWidth: Int = 0, usedHeight: Int = 0)
    fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int, marginLeft: Int = 0, marginTop: Int = 0, marginRight: Int = 0, marginBottom: Int = 0)

    fun offsetChildrenVertical(dy: Int)
    fun offsetChildrenHorizontal(dx: Int)
}

