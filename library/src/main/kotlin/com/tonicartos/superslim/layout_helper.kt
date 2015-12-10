package com.tonicartos.superslim

import android.support.v7.widget.RecyclerView
import android.view.View

interface ReadLayoutHelper {
    fun getLeft(view: View): Int
    fun getTop(view: View): Int
    fun getRight(view: View): Int
    fun getBottom(view: View): Int
    fun getMeasuredWidth(view: View): Int
    fun getMeasuredHeight(view: View): Int

    /**
     * Width of the layout area.
     */
    val layoutWidth: Int

    /**
     * Y limit that constrains the layout. This is used to know when to stop laying out items, and is effectively the
     * maximum height of the layout area.
     *
     * **Warning**: This value can change, and as such should not be stored.
     */
    val layoutLimit: Int
}

interface WriteLayoutHelper {
    fun measure(view: View, usedWidth: Int = 0, usedHeight: Int = 0)
    fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int)
}

interface ReadWriteLayoutHelper : ReadLayoutHelper, WriteLayoutHelper


interface RecyclerHelper {
    fun getView(position: Int): View
    val scrap: List<RecyclerView.ViewHolder>
}

interface StateHelper {
    val isPreLayout: Boolean
}

interface ManagerHelper {
    fun addView(i: Int, view: View)
    fun addDisappearingView(i: Int, view: View)

    val willRunPredictiveAnimations: Boolean
    val supportsPredictiveItemAnimations: Boolean
}

/**
 * Interface for querying and modifying the assigned section layout.
 */
interface LayoutHelper : ManagerHelper, StateHelper, RecyclerHelper, ReadWriteLayoutHelper {
    /**
     * Create a new layout helper for a subsection of this helper's section.
     */
    fun acquireSubsectionHelper(left: Int, top: Int, right: Int): LayoutHelper

    fun release()

    fun addIgnoredHeight(ignoredHeight: Int)
}


/****************************************************
 * Implementations
 ****************************************************/

private object LayoutHelperPool {
}

private class RootHelper(val manager: ManagerHelper, val config: LayoutHelper,
                         val recycler: RecyclerHelper, val state: StateHelper) : LayoutHelper,
        ManagerHelper by manager, ReadWriteLayoutHelper by config, RecyclerHelper by recycler, StateHelper by state {
    private var layoutLimitExtension: Int = 0
    override val layoutLimit: Int
        get() = config.layoutLimit + layoutLimitExtension

    override fun acquireSubsectionHelper(left: Int, top: Int, right: Int): LayoutHelper = helperPool.acquire(this, left, top, right - left)
    fun releaseSubsectionHelper(helper: SubsectionHelper) {
        helperPool.release(helper)
    }

    override fun release() {
    }

    override fun addIgnoredHeight(ignoredHeight: Int) {
        layoutLimitExtension += ignoredHeight
    }

    private var helperPool = LayoutHelperPool()

    private class LayoutHelperPool {
        private val pool = arrayListOf<SubsectionHelper>()

        fun acquire(root: RootHelper, x: Int, y: Int, width: Int) =
                if (pool.isEmpty()) {
                    SubsectionHelper(root, x, y, width)
                } else {
                    pool.removeAt(0).reInit(root, x, y, width)
                }

        fun release(helper: SubsectionHelper) {
            pool.add(helper)
        }
    }
}

private class SubsectionHelper(var root: RootHelper) : LayoutHelper by root {
    private var offset = Offset()
    private var width: Int = 0

    constructor(root: RootHelper, x: Int, y: Int, width: Int) : this(root) {
        offset.x = x
        offset.y = y
        this.width = width
    }

    override fun acquireSubsectionHelper(left: Int, top: Int, right: Int): LayoutHelper = root.acquireSubsectionHelper(offset.x + left, offset.y + top, offset.x + right)
    override fun release() {
        root.releaseSubsectionHelper(this)
    }

    data class Offset(var x: Int = 0, var y: Int = 0)

    fun reInit(root: RootHelper, x: Int, y: Int, width: Int): SubsectionHelper {
        this.root = root
        offset.x = x
        offset.y = y
        this.width = width
        return this
    }
}
