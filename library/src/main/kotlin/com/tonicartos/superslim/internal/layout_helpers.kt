package com.tonicartos.superslim.internal

import android.support.v7.widget.RecyclerView
import android.view.View
import com.tonicartos.superslim.*

internal class RootLayoutHelper(val manager: ManagerHelper, val config: ReadWriteLayoutHelper,
                                val recycler: RecyclerHelper, val state: StateHelper) : LayoutHelper,
                                                                                        ManagerHelper by manager, ReadWriteLayoutHelper by config,
                                                                                        RecyclerHelper by recycler, StateHelper by state {
    private var layoutLimitExtension: Int = 0
    override val layoutLimit: Int
        get() = config.layoutLimit + layoutLimitExtension

    override fun acquireSubsectionHelper(left: Int, top: Int, right: Int): LayoutHelper = helperPool.acquire(this, left, top, right - left)
    fun releaseSubsectionHelper(helper: LayoutHelper) {
        helperPool.release(helper as? SubsectionHelper ?: return)
    }

    override fun release() {
    }

    override fun addIgnoredHeight(ignoredHeight: Int) {
        layoutLimitExtension += ignoredHeight
    }

    private var helperPool = LayoutHelperPool()

    private class LayoutHelperPool {
        private val pool = arrayListOf<SubsectionHelper>()

        fun acquire(root: RootLayoutHelper, x: Int, y: Int, width: Int) =
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

private class SubsectionHelper(var root: RootLayoutHelper) : LayoutHelper by root {
    private var offset = Offset()
    private var width: Int = 0
    override val layoutWidth: Int
        get() = width
    override val layoutLimit: Int
        get() = root.layoutLimit - offset.y

    constructor(root: RootLayoutHelper, x: Int, y: Int, width: Int) : this(root) {
        offset.x = x
        offset.y = y
        this.width = width
    }

    override fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int, marginLeft: Int, marginTop: Int, marginRight: Int, marginBottom: Int) {
        root.layout(view, offset.x + left, offset.y + top, offset.x + right, offset.y + bottom, marginLeft, marginTop, marginRight, marginBottom)
    }

    override fun acquireSubsectionHelper(left: Int, top: Int, right: Int): LayoutHelper = root.acquireSubsectionHelper(offset.x + left, offset.y + top, offset.x + right)
    override fun release() {
        root.releaseSubsectionHelper(this)
    }

    data class Offset(var x: Int = 0, var y: Int = 0)

    fun reInit(root: RootLayoutHelper, x: Int, y: Int, width: Int): SubsectionHelper {
        this.root = root
        offset.x = x
        offset.y = y
        this.width = width
        return this
    }

    override fun toString(): String = "SubsectionHelper($offset, width = $width, limit = $layoutLimit)"
}

internal class RecyclerWrapper : RecyclerHelper {
    var recycler: RecyclerView.Recycler? = null

    fun wrap(recycler: RecyclerView.Recycler): RecyclerWrapper {
        this.recycler = recycler
        return this
    }

    override fun getView(position: Int): View = recycler!!.getViewForPosition(position)

    override val scrap: List<RecyclerView.ViewHolder>
        get() = recycler!!.scrapList
}

internal class StateWrapper : StateHelper {
    var state: RecyclerView.State? = null

    fun wrap(state: RecyclerView.State): StateWrapper {
        this.state = state
        return this
    }

    override val hasTargetScrollPosition: Boolean
        get() = state!!.hasTargetScrollPosition()

    override val targetScrollPosition: Int
        get() = state!!.targetScrollPosition

    override val willRunPredictiveAnimations: Boolean
        get() = state!!.willRunPredictiveAnimations()

    override val isPreLayout: Boolean
        get() = state!!.isPreLayout

    override val itemCount: Int
        get() = state!!.itemCount
}
