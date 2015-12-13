package com.tonicartos.superslim

import android.os.Parcelable
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import com.tonicartos.superslim.adapter.Section
import com.tonicartos.superslim.internal.*
import com.tonicartos.superslim.internal.layout.LinearSectionConfig
import com.tonicartos.superslim.internal.layout.LinearSectionState

interface AdapterContract {
    fun getSections(): List<Section.Config>
    fun setSectionIds(map: List<Int>)
    fun populateSection(sectionId: Int, sectionData: SectionData)

    interface SectionData {
        var hasHeader: Boolean
        var numChildren: Int
        var childSections: IntArray
        var adapterPosition: Int
        var itemCount: Int
    }
}

/**
 *
 */
class SuperSlimLayoutManager : RecyclerView.LayoutManager(), ManagerHelper, ReadWriteLayoutHelper {
    companion object {
        const val VERTICAL = 0
        const val HORIZONTAL = 1
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams? {
        throw UnsupportedOperationException()
    }

    /*************************
     * Graph
     *************************/

    var adapterContract: AdapterContract? = null

    override fun onAdapterChanged(oldAdapter: RecyclerView.Adapter<*>?, newAdapter: RecyclerView.Adapter<*>?) {
        super.onAdapterChanged(oldAdapter, newAdapter)
        if (oldAdapter == newAdapter) return
        newAdapter ?: return graph.reset()

        val adapter = newAdapter as? AdapterContract ?: throw IllegalArgumentException("adapter does not implement AdapterContract")
        graph.loadGraph(adapter)
        adapterContract = adapter
    }

    override fun onAttachedToWindow(view: RecyclerView) {
        super.onAttachedToWindow(view)
        view.adapter ?: return graph.reset()
        graph.loadGraph(view.adapter as? AdapterContract ?: throw IllegalArgumentException("adapter does not implement AdapterContract"))
    }

    override fun onDetachedFromWindow(view: RecyclerView?, recycler: RecyclerView.Recycler?) {
        super.onDetachedFromWindow(view, recycler)
        graph.reset()
    }

    override fun onItemsChanged(view: RecyclerView) {
        val adapter = view.adapter as? AdapterContract ?: throw IllegalArgumentException("adapter does not implement AdapterContract")
        graph.loadGraph(adapter)
        adapterContract = adapter
    }

    /*************************
     * Configuration
     *************************/

    private var _orientation = VERTICAL
    var orientation: Int
        get() = _orientation
        set(@Orientation value) {
            if (value != HORIZONTAL && value != VERTICAL) throw IllegalArgumentException("invalid orientation: {$value}")
            assertNotInLayoutOrScroll(null)
            if (orientation == value) return
            _orientation = value
            configChanged = true
            requestLayout()
        }

    private var _reverseLayout = false
    var reverseLayout: Boolean
        get() = _reverseLayout
        set(value) {
            assertNotInLayoutOrScroll(null)
            if (_reverseLayout == value) return
            _reverseLayout = value
            configChanged = true
            requestLayout()
        }

    private var _stackFromEnd: Boolean = false
    var stackFromEnd: Boolean
        get() = _stackFromEnd
        set(value) {
            assertNotInLayoutOrScroll(null)
            if (_stackFromEnd == value) return
            _stackFromEnd = value
            configChanged = true
            requestLayout()
        }

    private var configChanged = true
    private var _configHelper: ReadWriteLayoutHelper? = null
    private val configHelper: ReadWriteLayoutHelper
        get() = if (configChanged) {
            // Build a chain of configuration transformations.
            var chain: ReadWriteLayoutHelper? = if (stackFromEnd) StackFromEndConfigHelper(this) else null
            chain = if (reverseLayout) ReverseLayoutConfigHelper(chain ?: this) else chain
            chain = if (orientation == HORIZONTAL) HorizontalConfigHelper(chain ?: this) else chain
            chain = if (layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL) RtlConfigHelper(chain ?: this) else chain
            _configHelper = chain
            configChanged = false
            chain ?: this
        } else {
            _configHelper ?: this
        }

    /****************************************************
     * Layout
     ****************************************************/

    private val recyclerHelper = RecyclerWrapper()
    private val stateHelper = StateWrapper()

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        graph.layout(RootLayoutHelper(this, configHelper, recyclerHelper.wrap(recycler), stateHelper.wrap(state)))
    }

    /****************************************************
     * Scrolling
     ****************************************************/

    override fun canScrollVertically() = orientation == VERTICAL

    override fun canScrollHorizontally() = orientation == HORIZONTAL

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        // TODO:
        return super.scrollVerticallyBy(dy, recycler, state)
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        // TODO:
        return super.scrollHorizontallyBy(dx, recycler, state)
    }

    /****************************************************
     * Scroll indicator computation
     ****************************************************/

    override fun computeVerticalScrollExtent(state: RecyclerView.State?): Int {
        // TODO:
        return super.computeVerticalScrollExtent(state)
    }

    override fun computeVerticalScrollRange(state: RecyclerView.State?): Int {
        // TODO:
        return super.computeVerticalScrollRange(state)
    }

    override fun computeVerticalScrollOffset(state: RecyclerView.State?): Int {
        // TODO:
        return super.computeVerticalScrollOffset(state)
    }

    override fun computeHorizontalScrollExtent(state: RecyclerView.State?): Int {
        // TODO:
        return super.computeHorizontalScrollExtent(state)
    }

    override fun computeHorizontalScrollRange(state: RecyclerView.State?): Int {
        // TODO:
        return super.computeHorizontalScrollRange(state)
    }

    override fun computeHorizontalScrollOffset(state: RecyclerView.State?): Int {
        // TODO:
        return super.computeHorizontalScrollOffset(state)
    }

    /****************************************************
     * ReadWriteLayoutHelper implementation
     ****************************************************/

    override val layoutLimit: Int
        get() = height
    override val layoutWidth: Int
        get() = width

    override fun getMeasuredHeight(child: View) = getDecoratedMeasuredHeight(child)
    override fun getMeasuredWidth(child: View) = getDecoratedMeasuredWidth(child)
    override fun getLeft(child: View) = getDecoratedLeft(child)
    override fun getTop(child: View) = getDecoratedTop(child)
    override fun getRight(child: View) = getDecoratedRight(child)
    override fun getBottom(child: View) = getDecoratedBottom(child)

    override fun measure(view: View, usedWidth: Int, usedHeight: Int) {
        measureChildWithMargins(view, usedWidth, usedHeight)
    }

    override fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int, marginLeft: Int, marginTop: Int, marginRight: Int, marginBottom: Int) {
        layoutDecorated(view, left, top, right, bottom)
    }

    /****************************************************
     * Data change notifications
     *
     * Item change notifications need to be recorded so
     * that the layout helper can report what state a
     * child is in.
     *
     * The recorded notifications are cleared at the end
     * of each layout pass.
     *
     * When an item change notification comes in, the
     * section graph is updated. This is to keep the
     * graph and the recycler synchronised.
     *
     * Section change notifications come direct from the
     * adapter and need to be re-sequenced appropriately.
     ****************************************************/

    private val graph = GraphManager(LinearSectionState(LinearSectionConfig(0, 0, 0)))
    private val itemChangeHelper = ItemChangeHelper()

    /*************************
     * Section changes from adapter
     *************************/

    fun notifySectionAdded(parent: Int, position: Int, config: Section.Config): Int {
        // Always copy the config as soon as it enters this domain.
        return graph.sectionAdded(parent, position, config.copy())
    }

    fun notifySectionRemoved(section: Int, parent: Int, position: Int) {
        graph.queueSectionRemoved(section, parent, position)
    }

    //    fun notifySectionMoved(section: Int, fromParent: Int, fromPosition: Int, toParent: Int, toPosition: Int) {
    //        graph.queueSectionMoved(section, fromParent, fromPosition, toParent, toPosition)
    //    }

    fun notifySectionUpdated(section: Int, config: Section.Config) {
        // Always copy the config as soon as it enters this domain.
        graph.queueSectionUpdated(section, config.copy())
    }

    /*************************
     * Section item changes from adapter
     *************************/

    fun notifySectionHeaderAdded(section: Int, position: Int) {
        itemChangeHelper.queueSectionHeaderAdded(section, position)
    }

    fun notifySectionHeaderRemoved(section: Int, position: Int) {
        itemChangeHelper.queueSectionHeaderRemoved(section, position)
    }

    fun notifySectionItemsAdded(section: Int, positionStart: Int, itemCount: Int) {
        itemChangeHelper.queueSectionItemsAdded(section, positionStart, itemCount)
    }

    fun notifySectionItemsRemoved(section: Int, positionStart: Int, itemCount: Int) {
        itemChangeHelper.queueSectionItemsRemoved(section, positionStart, itemCount)
    }

    fun notifySectionItemsMoved(fromSection: Int, from: Int, toSection: Int, to: Int) {
        itemChangeHelper.queueSectionItemsMoved(fromSection, from, toSection, to)
    }

    /*************************
     * Item changes from recycler view
     *************************/

    override fun onItemsAdded(recyclerView: RecyclerView?, positionStart: Int, itemCount: Int) {
        val event = itemChangeHelper.pullAddEventData(positionStart, itemCount)
        graph.addItems(event, positionStart, itemCount)
    }

    override fun onItemsRemoved(recyclerView: RecyclerView?, positionStart: Int, itemCount: Int) {
        val event = itemChangeHelper.pullRemoveEventData(positionStart, itemCount)
        graph.removeItems(event, positionStart, itemCount)
    }

    override fun onItemsMoved(recyclerView: RecyclerView?, from: Int, to: Int, itemCount: Int) {
        var (fromSection, toSection) = itemChangeHelper.pullMoveEventData(from, to)
        graph.moveItems(fromSection, from, toSection, to)
    }

    /*************************
     * State management
     *************************/

    var pendingSavedState: RecyclerView.SavedState? = null

    override val supportsPredictiveItemAnimations: Boolean
        get() = pendingSavedState == null

    override fun assertNotInLayoutOrScroll(message: String?) {
        if (pendingSavedState == null) {
            super.assertNotInLayoutOrScroll(message)
        }
    }
}
