package com.tonicartos.superslim

import android.support.v7.widget.RecyclerView
import android.view.View
import com.tonicartos.superslim.adapter.Section
import com.tonicartos.superslim.slm.LinearSectionConfig
import com.tonicartos.superslim.slm.LinearSectionState

/**
 *
 */
class LayoutManager : RecyclerView.LayoutManager() {
    companion object {
        const val ITEM_UNCHANGED = 0
        const val ITEM_ADDED = 1
        const val ITEM_REMOVED = 2
        const val ITEM_MOVED = 3
        const val ITEM_UPDATED = 4

        const val VERTICAL = 0
        const val HORIZONTAL = 1
    }

    @Orientation val orientation: Int = VERTICAL
    val reverseLayout: Boolean = false
    val stackFromEnd: Boolean = false

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams? {
        throw UnsupportedOperationException()
    }

    /****************************************************
     * Layout
     ****************************************************/

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        super.onLayoutChildren(recycler, state)
    }

    /****************************************************
     * Scrolling
     ****************************************************/

    override fun canScrollVertically(): Boolean {
        return super.canScrollVertically()
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        return super.scrollVerticallyBy(dy, recycler, state)
    }

    override fun canScrollHorizontally(): Boolean {
        return super.canScrollHorizontally()
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        return super.scrollHorizontallyBy(dx, recycler, state)
    }

    /****************************************************
     * Scroll indicator computation
     ****************************************************/

    override fun computeVerticalScrollExtent(state: RecyclerView.State?): Int {
        return super.computeVerticalScrollExtent(state)
    }

    override fun computeVerticalScrollRange(state: RecyclerView.State?): Int {
        return super.computeVerticalScrollRange(state)
    }

    override fun computeVerticalScrollOffset(state: RecyclerView.State?): Int {
        return super.computeVerticalScrollOffset(state)
    }

    override fun computeHorizontalScrollExtent(state: RecyclerView.State?): Int {
        return super.computeHorizontalScrollExtent(state)
    }

    override fun computeHorizontalScrollRange(state: RecyclerView.State?): Int {
        return super.computeHorizontalScrollRange(state)
    }

    override fun computeHorizontalScrollOffset(state: RecyclerView.State?): Int {
        return super.computeHorizontalScrollOffset(state)
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
        return graph.sectionAdded(parent, position, config)
    }

    fun notifySectionRemoved(section: Int, parent: Int, position: Int) {
        graph.queueSectionRemoved(section, parent, position)
    }

//    fun notifySectionMoved(section: Int, fromParent: Int, fromPosition: Int, toParent: Int, toPosition: Int) {
//        graph.queueSectionMoved(section, fromParent, fromPosition, toParent, toPosition)
//    }

    fun notifySectionUpdated(section: Int, config: Section.Config) {
        graph.queueSectionUpdated(section, config)
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
}

private class RecyclerHelperImpl(val recycler: RecyclerView.Recycler) : RecyclerHelper {
    override fun getView(position: Int): View = recycler.getViewForPosition(position)

    override val scrap: List<RecyclerView.ViewHolder>
        get() = recycler.scrapList
}

private class StateHelperImpl(val state: RecyclerView.State) : StateHelper {
    override val isPreLayout: Boolean
        get() = state.isPreLayout
}
