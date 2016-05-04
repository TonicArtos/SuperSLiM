package com.tonicartos.superslim.internal

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.util.SparseArray
import android.view.View
import com.tonicartos.superslim.*
import com.tonicartos.superslim.internal.layout.HeaderLayoutManager
import java.util.*

internal class GraphManager(adapter: AdapterContract<*>) {
    private companion object {
        private const val ENABLE_ITEM_CHANGE_LOGGING = false
    }


    val root: SectionState
    private val sectionIndex = SectionManager()

    init {
        // Init root
        root = adapter.getRoot().makeSection()
        val rootId = sectionIndex.add(root)
        adapter.setRootId(rootId)
        // Init rest
        val adapterIds2SlmIds = adapter.getSections().mapValues { sectionIndex.add(it.value.makeSection()) }
        adapter.setSectionIds(adapterIds2SlmIds)

        val sectionData = SectionData()
        // Populate root
        adapter.populateRoot(sectionData)
        sectionData.subsections = sectionData.subsectionsById.map { sectionIndex[it] }
        sectionIndex[rootId].load(sectionData)
        // Populate rest
        adapterIds2SlmIds.forEach {
            adapter.populateSection(it.key to sectionData)
            sectionData.subsections = sectionData.subsectionsById.map { sectionIndex[it] }
            sectionIndex[it.value].load(sectionData)
        }
    }

    internal fun scrollToPocrollition(adapterPosition: Int) {
        var position = root.constrainToItemRange(adapterPosition)
        root.setLayoutPositionFromAdapter(position)
    }

    /*************************
     * Layout
     *************************/
    internal var requestedPosition = 0
        get() {
            //            hasRequestedPosition = false
            return field
        }
        set(value) {
            field = value
            hasRequestedPosition = true
        }
    private var hasRequestedPosition = false

    fun layout(helper: RootLayoutHelper) {
        if (!helper.isPreLayout) {
            //doSectionMoves()
            doSectionUpdates()
        }

        if (hasRequestedPosition) root.setLayoutPositionFromAdapter(requestedPosition)
        //        Log.d("layout", "helper = $helper\nsection = $root\n\n")
        root.layout(helper, 0, 0, helper.layoutWidth)

        if (helper.isPreLayout) {
            doSectionRemovals()
        }
    }

    fun scrollBy(d: Int, helper: RootLayoutHelper): Int {
        Log.d("Graph", "scrollBy($d)")
        if (d == 0) return 0
        // If d is +ve, then scrolling to end. To apply the effect we offset children upwards (-ve).
        return if (d > 0) scrollDown(d, helper) else scrollUp(d, helper)
    }

    private fun scrollDown(dy: Int, helper: RootLayoutHelper): Int {
        val bottomEdge = fillBottom(dy, helper)
        val scrolled = if (bottomEdge - dy < helper.layoutLimit - helper.basePaddingBottom) bottomEdge - helper.layoutLimit - helper.basePaddingBottom else dy
        if (scrolled > 0) {
            helper.offsetChildrenVertical(-scrolled)
        }

        trimTop(helper)

        return scrolled
    }

    private fun scrollUp(dy: Int, helper: RootLayoutHelper): Int {
        val filled = fillTop(dy, helper)
        val scrolled = if (-filled < dy) -dy else filled
        Log.d("scrollUp", "dy = $dy, scroll by = $scrolled")

        if (scrolled > 0) {
            helper.offsetChildrenVertical(scrolled)
        }

        trimBottom(helper)

        return -scrolled
    }

    private fun fillTop(dy: Int, helper: RootLayoutHelper) = root.fillTop(dy, helper)
    private fun fillBottom(dy: Int, helper: RootLayoutHelper) = root.fillBottom(dy, helper)
    private fun trimTop(helper: RootLayoutHelper) = root.trimTop(helper, helper.basePaddingLeft, helper.basePaddingTop, helper.layoutWidth - helper.basePaddingRight)
    private fun trimBottom(helper: RootLayoutHelper) = root.trimBottom(helper, helper.basePaddingLeft, helper.basePaddingTop, helper.layoutWidth - helper.basePaddingRight)

    /*************************
     * Scheduling section changes
     *************************/

    private data class ScheduledSectionRemoval(val section: Int, val parent: Int)

    private data class ScheduledSectionUpdate(val section: Int, val config: SectionConfig)

    //private data class ScheduledSectionMove(val section: Int, val fromParent: Int, val fromPosition: Int, val toParent: Int, val toPosition: Int)

    private val sectionsToRemove = arrayListOf<ScheduledSectionRemoval>()
    private val sectionsToUpdate = arrayListOf<ScheduledSectionUpdate>()
    //private val sectionsToMove = arrayListOf<ScheduledSectionMove>()

    fun sectionAdded(parent: Int, position: Int, config: SectionConfig): Int {
        val newSection = config.makeSection()
        sectionIndex[parent].insertSection(position, newSection)
        return sectionIndex.add(newSection)
    }

    fun queueSectionRemoved(section: Int, parent: Int) {
        sectionsToRemove.add(ScheduledSectionRemoval(section, parent))
    }

    fun queueSectionUpdated(section: Int, config: SectionConfig) {
        sectionsToUpdate.add(ScheduledSectionUpdate(section, config))
    }

    //fun queueSectionMoved(section: Int, fromParent: Int, fromPosition: Int, toParent: Int, toPosition: Int) {
    //    sectionsToMove.add(ScheduledSectionMove(section, fromParent, fromPosition, toParent, toPosition))
    //}

    private fun doSectionRemovals() {
        for (remove in sectionsToRemove) {
            sectionIndex[remove.parent].removeSection(sectionIndex[remove.section])
            sectionIndex.remove(remove.section)
        }
        sectionsToRemove.clear()
    }

    private fun doSectionUpdates() {
        for (update in sectionsToUpdate) {
            sectionIndex[update.section] = update.config.makeSection(sectionIndex[update.section])
        }
    }

    //private fun doSectionMoves() {
    //    for (move in sectionsToMove) {
    //        sections[move.fromParent).removeSection(move.fromPosition)
    //        sections[move.toParent).insertSection(move.toPosition, sections[move.section))
    //    }
    //    sectionsToMove.clear()
    //}

    /*************************
     * Item events
     *************************/
    fun addItems(eventSectionData: EventSectionData, positionStart: Int, itemCount: Int) {
        if (ENABLE_ITEM_CHANGE_LOGGING) Log.d("Sslm-DC events", "addItems(event = $eventSectionData, positionStart = $positionStart, itemCount = $itemCount)")
        val section = sectionIndex[eventSectionData.section]
        if (eventSectionData.action and EventSectionData.HEADER > 0) {
            if (itemCount > 1) throw IllegalArgumentException("Expected item count of 1 for add header operation.")
            section.addHeader()
        } else {
            section.addItems(eventSectionData.start, itemCount)
        }
    }

    fun removeItems(eventSectionData: EventSectionData, positionStart: Int, itemCount: Int) {
        if (ENABLE_ITEM_CHANGE_LOGGING) Log.d("Sslm-DC events", "removeItems(event = $eventSectionData, positionStart = $positionStart, itemCount = $itemCount)")
        val section = sectionIndex[eventSectionData.section]
        if (eventSectionData.action and EventSectionData.HEADER > 0) {
            if (itemCount > 1) throw IllegalArgumentException("Expected item count of 1 for remove header operation.")
            section.removeHeader()
        } else {
            section.removeItems(positionStart, itemCount)
        }
    }

    fun moveItems(eventSectionData: EventSectionData, from: Int, to: Int) {
        if (ENABLE_ITEM_CHANGE_LOGGING) Log.d("Sslm-DC events", "moveItem(eventData = $eventSectionData, from = $from, to = $to)")
        sectionIndex[eventSectionData.fromSection].removeItems(from, 1)
        sectionIndex[eventSectionData.toSection].addItems(eventSectionData.to, 1)
    }
}

private class SectionManager {
    private var numSectionsSeen = 0
    private val sectionIndex = SparseArray<SectionState>()

    fun add(section: SectionState): Int {
        val id = numSectionsSeen
        numSectionsSeen += 1
        sectionIndex.put(id, section)
        return id
    }

    fun remove(section: Int) {
        sectionIndex.remove(section)
    }

    operator fun get(id: Int) = sectionIndex[id]

    operator fun set(id: Int, newSection: SectionState) {
        sectionIndex.put(id, newSection)
    }
}

/**
 * Section data
 */
abstract class SectionState(val baseConfig: SectionConfig, oldState: SectionState? = null) {
    private companion object {
        const val NUM_HEADER_CHILDREN: Int = 2
        const val UNSET_OR_BEFORE_CHILDREN: Int = -1

        const val ENABLE_LAYOUT_LOGGING = false
    }


    open class LayoutState {
        /**
         * Number of views.
         */
        var numViews = 0
        /**
         * The height of the section for this layout pass. Only valid after section is laid out, and never use outside the
         * same layout pass.
         */
        open var height = 0

        /**
         * Position that is the head of the displayed section content. -1 is the unset default value.
         */
        var headPosition = -1
            set(value) {
                field = value
            }
        /**
         * Position that is the tail of the displayed section content. -1 is the unset and default value.
         */
        var tailPosition = -1

        var left = 0
        var right = 0

        /**
         * Area drawn pass y0.
         */
        var overdraw = 0

        /**
         * Reset layout state.
         */
        internal open fun reset() {
            height = 0
            headPosition = -1
            tailPosition = -1
            left = 0
            right = 0
            overdraw = 0
        }

        internal fun copy(old: LayoutState) {
            height = old.height
            headPosition = old.headPosition
            tailPosition = old.tailPosition
        }

        override fun toString(): String {
            return "(headPosition = $headPosition, tailPosition = $tailPosition, numViews = $numViews, left = $left, right = $right, height = $height, overdraw = $overdraw)"
        }
    }

    internal class HeaderLayoutState(val layout: LayoutState) : LayoutState() {
        /**
         * The current state of the header. State values are header implementation dependent.
         */
        var state = 0

        override var height: Int
            get() = layout.height
            set(value) {
                layout.height = value
            }

        override fun reset() {
            super.reset()
            state = 0
        }

        override fun toString(): String {
            return "(state = $state, headPosition = $headPosition, tailPosition = $tailPosition, numViews = $numViews, left = $left, right = $right, height = $height, overdraw = $overdraw)"
        }
    }

    /**
     * Used to persist state between layout and scroll passes.
     */
    val layout = LayoutState()

    /**
     * Tracks state for the header layout with invisibly wraps the section layout. It is best to treat the header layout
     * as its own section with two children, the header, and the section. This makes tracking laid out positions easier.
     */
    internal val headerLayout = HeaderLayoutState(layout)

    internal fun resetLayout() {
        layout.reset()
        headerLayout.reset()
    }

    /****************************************************
     * Section
     ****************************************************/

    /**
     * Total number of children. Children does not equate to items as some subsections may be empty.
     */
    var numChildren = 0
        private set(value) {
            field = value
        }

    /**
     * Total number of items in the section, including the header and items in subsections.
     */
    internal var totalItems = 0
        private set(value) {
            if (totalItemsAreImmutable) return
            parent?.itemCountsChangedInSubsection(this, value - field)
            field = value
        }

    /**
     * Ignore changes called in from subsections while true.
     */
    private var totalItemsAreImmutable = false

    internal var parent: SectionState? = null

    /**
     * Sorted list of subsections.
     */
    internal val subsections: ArrayList<SectionState>

    /**
     * Position of this section in the adapter.
     */
    internal var positionInAdapter = 0
        get() = field
        set(value) {
            subsections.forEach { it.positionInAdapter += value - field }
            field = value
        }

    init {
        if (oldState != null) {
            layout.copy(oldState.layout)
            headerLayout.copy(oldState.headerLayout)
            totalItems = oldState.totalItems
            numChildren = oldState.numChildren
            subsections = oldState.subsections
            positionInAdapter = oldState.positionInAdapter
            parent = oldState.parent
        } else {
            subsections = ArrayList()
        }
    }

    /*************************
     * Access items
     *************************/

    internal var hasHeader: Boolean = false

    internal fun getHeader(helper: LayoutHelper): Child? =
            if (hasHeader) {
                ItemChild.wrap(helper.getView(positionInAdapter), helper)
            } else {
                null
            }

    internal fun getDisappearingHeader(helper: LayoutHelper): Child? =
            if (hasHeader && helper.scrapHasPosition(positionInAdapter)) {
                DisappearingItemChild.wrap(helper.getView(positionInAdapter), helper)
            } else {
                null
            }

    /**
     * Gets a child at the specified position that might need more layout.
     *
     * @return Null if the child at the position is known to be fully laid out.
     */
    internal fun getNonFinalChildAt(helper: LayoutHelper, position: Int): Child? {
        var hiddenItems = positionInAdapter + if (hasHeader) 1 else 0
        var lastSectionPosition = 0
        for ((i, it) in subsections.withIndex()) {
            if (it.positionInAdapter - hiddenItems + i > position) {
                break
            } else if (it.positionInAdapter - hiddenItems + i == position) {
                return SectionChild.wrap(it, helper)
            } else {
                hiddenItems += it.totalItems
                lastSectionPosition = i
            }
        }

        return null
    }

    internal fun getChildAt(helper: LayoutHelper, position: Int): Child {
        var hiddenItems = positionInAdapter + if (hasHeader) 1 else 0
        var lastSectionPosition = 0
        for ((i, it) in subsections.withIndex()) {
            if (it.positionInAdapter - hiddenItems + i > position) {
                break
            } else if (it.positionInAdapter - hiddenItems + i == position) {
                return SectionChild.wrap(it, helper)
            } else {
                hiddenItems += it.totalItems
                lastSectionPosition = i
            }
        }

        return ItemChild.wrap(helper.getView(hiddenItems + position - lastSectionPosition), helper)
    }

    /**
     * Get a disappearing child. This is a special child which is used to correctly position disappearing views. Any
     * views not in the scrap list are virtual and are not fetched from the recycler. Instead a dummy item is returned
     * which behaves as a view with layout params(width = match_parent, height = 0).
     */
    internal fun getDisappearingChildAt(helper: LayoutHelper, position: Int): Child {
        var hiddenItems = positionInAdapter + if (hasHeader) 1 else 0
        var lastSectionPosition = 0
        for ((i, it) in subsections.withIndex()) {
            if (it.positionInAdapter - hiddenItems + i > position) {
                break
            } else if (it.positionInAdapter - hiddenItems + i == position) {
                return SectionChild.wrap(it, helper)
            } else {
                hiddenItems += it.totalItems
                lastSectionPosition = i
            }
        }

        val viewPosition = hiddenItems + position - lastSectionPosition
        if (helper.scrapHasPosition(viewPosition)) {
            return DisappearingItemChild.wrap(helper.getView(viewPosition), helper)
        } else {
            return DummyChild.wrap(helper)
        }
    }

    /*************************
     * Utility
     *************************/

    internal fun constrainToItemRange(position: Int): Int {
        return Math.max(position, totalItems - 1)
    }

    /*************************
     * Layout
     *************************/

    fun layout(helper: LayoutHelper, left: Int, top: Int, right: Int) {
        if (totalItems == 0) return

        headerLayout.left = left
        headerLayout.right = right

        val subsectionHelper = helper.acquireSubsectionHelper(top, headerLayout.left, headerLayout.right)
        if (headerLayout.headPosition == -1) headerLayout.headPosition = 0
        HeaderLayoutManager.onLayout(subsectionHelper, this)
        subsectionHelper.release()
    }

    internal fun layout(helper: RootLayoutHelper, left: Int, top: Int, right: Int) {
        if (ENABLE_LAYOUT_LOGGING) Log.d("SectionState", "Layout $this")
        if (totalItems == 0) return

        layout.left = left
        layout.right = right

        val subsectionHelper = helper.acquireSubsectionHelper(top, layout.left, layout.right)
        if (layout.headPosition == -1) layout.headPosition = 0
        doLayout(subsectionHelper)
        subsectionHelper.release()
    }

    internal fun layoutContent(helper: LayoutHelper, left: Int, top: Int, right: Int) {
        if (totalItems == 0) return

        // Store layout positions for use during scrolling.
        layout.left = left
        layout.right = right

        val subsectionHelper = helper.acquireSubsectionHelper(top, layout.left, layout.right)
        if (layout.headPosition == -1) layout.headPosition = 0
        doLayout(subsectionHelper)
        subsectionHelper.release()
    }

    protected abstract fun doLayout(helper: LayoutHelper)

    /**
     * Set the layout position of this section.
     *
     * @return False if the position is not within this section.
     */
    internal fun setLayoutPositionFromAdapter(requestedAdapterPosition: Int): Boolean {
        // Check requested position is in this section.
        if (requestedAdapterPosition < positionInAdapter || positionInAdapter + totalItems <= requestedAdapterPosition) return false

        // Check if position is header.
        if (hasHeader && requestedAdapterPosition == positionInAdapter) {
            layout.headPosition = 0
            headerLayout.headPosition = 0
            return true
        }

        /*
         * Position is within content. It may be in a subsection or an item of this section. Calculating the child
         * position is a little difficult as it must account for interleaved child items and subsections.
         */
        var offset = positionInAdapter + if (hasHeader) 1 else 0
        var childrenAccountedFor = 0 + if (hasHeader) 1 else 0
        for ((i, section) in subsections.withIndex()) {
            if (requestedAdapterPosition < section.positionInAdapter) {
                // Position is before this subsection, so it must be a child item not a member of a subsection.
                headerLayout.headPosition = 1
                layout.headPosition = childrenAccountedFor + requestedAdapterPosition - offset - if (hasHeader) 1 else 0
                return true
            }

            // Add items before this subsection (but after the last subsection) to children count.
            childrenAccountedFor += section.positionInAdapter - offset

            if (section.setLayoutPositionFromAdapter(requestedAdapterPosition)) {
                // Requested position was within the subsection so store it as the layout position of this section.
                headerLayout.headPosition = 1
                layout.headPosition = childrenAccountedFor - if (hasHeader) 1 else 0
                return true
            }

            // Update offset to be after the subsection content.
            offset = section.positionInAdapter + section.totalItems
            // Account for this subsection.
            childrenAccountedFor += 1
        }

        // Position must be a child item after the last subsection.
        headerLayout.headPosition = 1
        layout.headPosition = childrenAccountedFor + requestedAdapterPosition - offset - if (hasHeader) 1 else 0
        return true
    }

    internal infix operator fun contains(
            viewHolder: RecyclerView.ViewHolder): Boolean = viewHolder.layoutPosition >= positionInAdapter && viewHolder.layoutPosition < positionInAdapter + totalItems

    /*************************
     * Scrolling
     *************************/

    /****************
     * Root
     ****************/
    internal fun fillTop(dy: Int, helper: RootLayoutHelper): Int {
        val subsectionHelper = helper.acquireSubsectionHelper(0, layout.left, layout.right)
        if (layout.headPosition == UNSET_OR_BEFORE_CHILDREN) layout.headPosition = NUM_HEADER_CHILDREN
        return doFillTop(dy, subsectionHelper).apply { subsectionHelper.release() }
    }

    internal fun fillBottom(dy: Int, helper: RootLayoutHelper): Int {
        // The initial subsection helper is offset to the bottom of the already filled area.
        val subsectionHelper = helper.acquireSubsectionHelper(layout.height, layout.left, layout.right)
        return doFillBottom(dy, subsectionHelper).apply { subsectionHelper.release() }
    }

    internal fun trimTop(helper: RootLayoutHelper, left: Int, top: Int, right: Int) {
        val subsectionHelper = helper.acquireSubsectionHelper(top, left, right)
        // TODO: Remember to adjust tail and head positions, and also to use -1 for when nothing is attached anymore.
        doTrimTop(subsectionHelper)
        subsectionHelper.release()
    }

    internal fun trimBottom(helper: RootLayoutHelper, left: Int, top: Int, right: Int) {
        val subsectionHelper = helper.acquireSubsectionHelper(top, left, right)
        doTrimBottom(subsectionHelper)
        subsectionHelper.release()
    }

    /***************
     * Sections
     ***************/
    internal fun fillTop(dy: Int, helper: LayoutHelper, left: Int, right: Int): Int {
        headerLayout.left = left
        headerLayout.right = right

        val subsectionHelper = helper.acquireSubsectionHelper(0, headerLayout.left, headerLayout.right)
        if (headerLayout.headPosition == UNSET_OR_BEFORE_CHILDREN) headerLayout.headPosition = NUM_HEADER_CHILDREN
        return HeaderLayoutManager.onFillTop(dy, subsectionHelper, this).apply { subsectionHelper.release() }
    }

    internal fun fillBottom(dy: Int, helper: LayoutHelper, left: Int, right: Int): Int {
        headerLayout.left = left
        headerLayout.right = right

        val subsectionHelper = helper.acquireSubsectionHelper(0, headerLayout.left, headerLayout.right)
        return HeaderLayoutManager.onFillBottom(dy, subsectionHelper, this).apply { subsectionHelper.release() }
    }

    internal fun fillContentTop(dy: Int, helper: LayoutHelper, left: Int, right: Int): Int {
        layout.left = left
        layout.right = right

        val subsectionHelper = helper.acquireSubsectionHelper(0, layout.left, layout.right)
        if (layout.headPosition == UNSET_OR_BEFORE_CHILDREN) layout.headPosition = numChildren
        return doFillTop(dy, subsectionHelper).apply { subsectionHelper.release() }
    }

    internal fun fillContentBottom(dy: Int, helper: LayoutHelper, left: Int, right: Int): Int {
        layout.left = left
        layout.right = right

        val subsectionHelper = helper.acquireSubsectionHelper(0, layout.left, layout.right)
        return doFillBottom(dy, subsectionHelper).apply { subsectionHelper.release() }
    }

    protected abstract fun doFillTop(dy: Int, helper: LayoutHelper): Int
    protected abstract fun doFillBottom(dy: Int, helper: LayoutHelper): Int

    internal fun trimTop(helper: LayoutHelper, left: Int, top: Int, right: Int) {
        val subsectionHelper = helper.acquireSubsectionHelper(top, left, right)
        HeaderLayoutManager.onTrimTop(subsectionHelper, this)
        subsectionHelper.release()
    }

    internal fun trimBottom(helper: LayoutHelper, left: Int, top: Int, right: Int) {
        val subsectionHelper = helper.acquireSubsectionHelper(top, left, right)
        HeaderLayoutManager.onTrimBottom(subsectionHelper, this)
        subsectionHelper.release()
    }

    internal fun trimContentTop(helper: LayoutHelper, left: Int, top: Int, right: Int) {
        val subsectionHelper = helper.acquireSubsectionHelper(top, left, right)
        doTrimTop(subsectionHelper)
        subsectionHelper.release()
    }

    internal fun trimContentBottom(helper: LayoutHelper, left: Int, top: Int, right: Int) {
        val subsectionHelper = helper.acquireSubsectionHelper(top, left, right)
        doTrimBottom(subsectionHelper)
        subsectionHelper.release()
    }

    protected abstract fun doTrimTop(helper: LayoutHelper)
    protected abstract fun doTrimBottom(helper: LayoutHelper)

    /*************************
     * Item management
     *************************/

    internal fun addHeader() {
        hasHeader = true
        subsections.forEach { it.positionInAdapter += 1 }
        totalItems += 1
    }

    internal fun removeHeader() {
        hasHeader = false
        subsections.forEach { it.positionInAdapter -= 1 }
        totalItems -= 1
    }

    internal fun addItems(position: Int, itemCount: Int) {
        var childPositionStart = position
        if (childPositionStart < 0) {
            childPositionStart = numChildren
        }

        applyToSubsectionsAfterChildPosition(childPositionStart) { i, it ->
            it.positionInAdapter += itemCount
        }

        numChildren += itemCount
        totalItems += itemCount
    }

    internal fun removeItems(fromAdapterPosition: Int, count: Int) {
        removeItemsInt(fromAdapterPosition, count)
    }
//        var currentRemoveFrom = fromAdapterPosition
//        var countInRange = count
//
//        // Constrain removal to range of items within the section.
//        if (currentRemoveFrom < positionInAdapter) {
//            val delta = positionInAdapter - currentRemoveFrom
//            countInRange -= delta
//            currentRemoveFrom += delta
//        }
//
//        if (currentRemoveFrom + countInRange > positionInAdapter + totalItems) {
//            val delta = (currentRemoveFrom + countInRange) - (positionInAdapter + totalItems)
//            countInRange -= delta
//        }
//
//        if (countInRange <= 0) return
//
//        var itemsRemaining = countInRange
//        var itemsThatAreChildren = 0
//
//        var itemsRemoved = 0
//
//        if (hasHeader && currentRemoveFrom == positionInAdapter) {
//            itemsRemoved += 1
//            itemsRemaining -= 1
//            currentRemoveFrom += 1
//            hasHeader = false
//        }
//
//        blockTotalItemChanges {
//            for (it in subsections) {
//                var (skipped, removed) = it.removeItemsInt(currentRemoveFrom, itemsRemaining)
//                it.positionInAdapter -= skipped + itemsRemoved
//                itemsRemoved += removed
//                itemsThatAreChildren += skipped
//                currentRemoveFrom += skipped
//                itemsRemaining -= removed + skipped
//            }
//        }
//
//        itemsThatAreChildren += itemsRemaining
//        totalItems -= countInRange
//        numChildren -= itemsThatAreChildren
//    }

    private fun removeItemsInt(removeFromAdapterPosition: Int, count: Int): Pair<Int, Int> {
        if (count == 0) return 0 to 0
        if (positionInAdapter + totalItems <= removeFromAdapterPosition) return 0 to 0 // Before removed items

        val itemsBeforeSection = Math.min(count, Math.max(0, positionInAdapter - removeFromAdapterPosition))
        if (positionInAdapter >= removeFromAdapterPosition + count) return itemsBeforeSection to 0 //After removed items

        val itemsAfterSection = Math.max(0, (removeFromAdapterPosition + count) - (positionInAdapter + totalItems))

        var currentRemoveFrom = Math.max(positionInAdapter, removeFromAdapterPosition)
        var itemsRemaining = count - itemsBeforeSection - itemsAfterSection
        var itemsThatAreChildren = 0
        var itemsRemoved = 0

        if (hasHeader && currentRemoveFrom == positionInAdapter) {
            itemsRemoved += 1
            itemsRemaining -= 1
            currentRemoveFrom += 1
            hasHeader = false
        }

        if (itemsRemaining == 0) {
            totalItems -= itemsRemoved
            return itemsBeforeSection to itemsRemoved
        }

        var removedInSection = 0
        blockTotalItemChanges {
            for (subsection in subsections) {
                var (before, removed) = subsection.removeItemsInt(currentRemoveFrom, itemsRemaining)
                removedInSection += before
                subsection.positionInAdapter -= removedInSection
                itemsThatAreChildren += before
                currentRemoveFrom += before
                itemsRemaining -= removed + before
                itemsRemoved += removed
                if (itemsRemaining == 0) break
            }
        }

        itemsThatAreChildren += itemsRemaining
        totalItems -= itemsRemoved + itemsThatAreChildren
        numChildren -= itemsThatAreChildren
        itemsRemoved += itemsThatAreChildren

        return itemsBeforeSection to itemsRemoved
    }

    private inline fun blockTotalItemChanges(f: () -> Unit) {
        totalItemsAreImmutable = true
        f()
        totalItemsAreImmutable = false
    }

    internal fun itemCountsChangedInSubsection(child: SectionState, changedCount: Int) {
        // Find child and adjust adapter position for sections after it.
        val index = subsections.indexOfFirst { it == child }
        if (index + 1 < subsections.size) {
            for (i in (index + 1)..(subsections.size - 1)) {
                subsections[i].positionInAdapter += changedCount
            }
        }

        totalItems += changedCount
    }

    /*************************
     * Section management
     *************************/

    internal fun insertSection(position: Int, newSection: SectionState) {
        var childInsertionPoint = position
        if (childInsertionPoint < 0) {
            childInsertionPoint = numChildren
        }

        var insertPoint = subsections.size
        var firstTime = true
        applyToSubsectionsAfterChildPosition(childInsertionPoint) { i, it ->
            it.positionInAdapter += newSection.totalItems
            if (firstTime) {
                insertPoint = i
                firstTime = false
            }
        }
        subsections.add(insertPoint, newSection)

        newSection.parent = this
        var numItemsInPriorSiblings = 0
        if (insertPoint > 0) {
            for (i in 0..(insertPoint - 1)) {
                numItemsInPriorSiblings += subsections[i].totalItems
            }
        }
        val priorChildrenThatAreItems = childInsertionPoint - insertPoint
        newSection.positionInAdapter = positionInAdapter + priorChildrenThatAreItems + numItemsInPriorSiblings
        numChildren += 1
        totalItems += newSection.totalItems
    }

    internal fun removeSection(section: SectionState) {
        var indexOfSection: Int = -1
        var afterSection = false
        subsections.forEachIndexed { i, it ->
            if (afterSection) {
                it.positionInAdapter -= section.totalItems
            } else if (it === section) {
                indexOfSection = i
                afterSection = true
            }
        }
        if (indexOfSection == -1) return

        subsections.removeAt(indexOfSection)
        totalItems -= section.totalItems
        numChildren -= 1
    }

    internal fun load(data: SectionData) {
        numChildren = data.childCount
        positionInAdapter = data.adapterPosition
        hasHeader = data.hasHeader
        totalItems = data.itemCount
        subsections.clear()
        subsections.addAll(data.subsections)
    }

    private inline fun applyToSubsectionsAfterChildPosition(childPositionStart: Int, crossinline f: (Int, SectionState) -> Unit) {
        var hiddenItems = positionInAdapter
        var applying = false
        subsections.forEachIndexed { i, it ->
            if (applying) {
                f(i, it)
            } else if (it.positionInAdapter - hiddenItems + i >= childPositionStart) {
                f(i, it)
                applying = true
            } else {
                hiddenItems += it.totalItems
            }
        }
    }

    override fun toString(): String = "(start = $positionInAdapter, hasHeader = $hasHeader, numChildren = $numChildren, totalItems = $totalItems, numSubsections = ${subsections.size}, layoutState = $layout, headerState = $headerLayout)".replace("\n", "\n\t")
    //    override fun toString(): String = "SectionState(start = $adapterPosition, hasHeader = $hasHeader, numChildren = $numChildren, totalItems = $totalItems, numSubsections = ${subsections.size}, subgraph = ${subsections.fold("") { s, it -> "$s\n$it" }})".replace("\n", "\n\t")

    //    /****************************************************
    //     * Test access to private members. Proguard will remove these in release.
    //     *
    //     * TODO: Configure proguard to remove these things in release.
    //     ****************************************************/
    //    interface TestAccess {
    //        val totalItems: Int
    //        val subsections: ArrayList<SectionState>
    //        val adapterPosition: Int
    //        fun addItems(childPositionStart: Int, itemCount: Int)
    //        fun removeItems(adapterPositionStart: Int, itemCount: Int)
    //        fun addHeader()
    //        fun removeHeader()
    //        fun removeSection(section: SectionState)
    //        fun insertSection(position: Int, newSection: SectionState)
    //    }
    //
    //    /**
    //     * Testing access to internal and private members of the instance. This will be removed in release by proguard.
    //     *
    //     * TODO: Proguard rule.
    //     */
    //    @VisibleForTesting
    //    internal val testAccess = object : TestAccess {
    //        override val totalItems: Int get() = this@SectionState.totalItems
    //        override val subsections: ArrayList<SectionState> get() = this@SectionState.subsections
    //        override val adapterPosition: Int get() = this@SectionState.positionInAdapter
    //
    //        override fun addItems(childPositionStart: Int, itemCount: Int) = this@SectionState.addItems(childPositionStart, itemCount)
    //        override fun removeItems(adapterPositionStart: Int, itemCount: Int) = this@SectionState.removeItems(adapterPositionStart, itemCount)
    //
    //        override fun addHeader() = this@SectionState.addHeader()
    //        override fun removeHeader() = this@SectionState.removeHeader()
    //
    //        override fun insertSection(position: Int, newSection: SectionState) = this@SectionState.insertSection(position, newSection)
    //        override fun removeSection(section: SectionState) = this@SectionState.removeSection(section)
    //    }
}

internal abstract class ChildInternal(var helper: LayoutHelper) : Child

private open class SectionChild(var section: SectionState, helper: LayoutHelper) : ChildInternal(helper) {
    companion object {
        val pool = arrayListOf<SectionChild>()

        fun wrap(section: SectionState, helper: LayoutHelper): SectionChild {
            return if (pool.isEmpty()) {
                SectionChild(section, helper)
            } else {
                pool.removeAt(0).reInit(section, helper)
            }
        }
    }

    private fun reInit(section: SectionState, helper: LayoutHelper): SectionChild {
        this.section = section
        this.helper = helper
        return this
    }

    override fun done() {
        pool.add(this)
    }

    override val isRemoved: Boolean
        get() = false

    private var _measuredWidth: Int = 0
    override val measuredWidth: Int
        get() = _measuredWidth

    override val measuredHeight: Int
        get() = Child.INVALID

    override fun measure(usedWidth: Int, usedHeight: Int) {
        _measuredWidth = helper.layoutWidth - usedWidth
    }

    protected var _left = 0
    override val left: Int
        get() = _left

    protected var _top = 0
    override val top: Int
        get() = _top

    protected var _right = 0
    override val right: Int
        get() = _right

    override val bottom: Int
        get() = Child.INVALID

    override fun layout(left: Int, top: Int, right: Int, bottom: Int) {
        _left = left
        _top = top
        _right = right
        section.layout(helper, left, top, right)
    }

    override fun fillTop(dy: Int, left: Int, top: Int, right: Int, bottom: Int): Int {
        _left = left
        _right = right
        return section.fillTop(dy, helper, left, right)
    }

    override fun fillBottom(dy: Int, left: Int, top: Int, right: Int, bottom: Int): Int {
        _left = left
        _right = right
        return section.fillBottom(dy, helper, left, right)
    }

    override val width: Int
        get() = _right - _left
    override val height: Int
        get() = section.layout.height

    override fun addToRecyclerView(i: Int) {
    }
}

private open class ItemChild(var view: View, helper: LayoutHelper) : ChildInternal(helper) {
    companion object {
        val pool = arrayListOf<ItemChild>()

        fun wrap(view: View, helper: LayoutHelper): ItemChild {
            return if (pool.isEmpty()) {
                ItemChild(view, helper)
            } else {
                pool.removeAt(0).reInit(view, helper)
            }
        }
    }

    private fun reInit(view: View, helper: LayoutHelper): ItemChild {
        this.view = view
        this.helper = helper
        return this
    }

    override fun done() {
        pool.add(this)
    }

    override val isRemoved: Boolean
        get() = view.rvLayoutParams.isItemRemoved

    override val measuredWidth: Int
        get() = helper.getMeasuredWidth(view)
    override val measuredHeight: Int
        get() = helper.getMeasuredHeight(view)

    override fun measure(usedWidth: Int, usedHeight: Int) {
        helper.measure(view, usedWidth, usedHeight)
    }

    override val left: Int
        get() = helper.getLeft(view)
    override val top: Int
        get() = helper.getTop(view)
    override val right: Int
        get() = helper.getRight(view)
    override val bottom: Int
        get() = helper.getBottom(view)

    override fun layout(left: Int, top: Int, right: Int, bottom: Int) {
        val m = view.rvLayoutParams
        helper.layout(view, left, top, right, bottom, m.leftMargin, m.topMargin, m.rightMargin, m.bottomMargin)
    }

    override fun fillTop(dy: Int, left: Int, top: Int, right: Int, bottom: Int): Int {
        layout(left, top, right, bottom)
        return 0
    }

    override fun fillBottom(dy: Int, left: Int, top: Int, right: Int, bottom: Int): Int {
        layout(left, top, right, bottom)
        return 0
    }

    override val width: Int
        get() = helper.getMeasuredWidth(view)
    override val height: Int
        get() = helper.getMeasuredHeight(view)

    override fun addToRecyclerView(i: Int) {
        helper.addView(view, i)
    }
}

private class DisappearingItemChild(view: View, helper: LayoutHelper) : ItemChild(view, helper) {
    companion object {
        val pool = arrayListOf<DisappearingItemChild>()

        fun wrap(view: View, helper: LayoutHelper): DisappearingItemChild {
            return if (pool.isEmpty()) {
                DisappearingItemChild(view, helper)
            } else {
                pool.removeAt(0).reInit(view, helper)
            }
        }
    }

    private fun reInit(view: View, helper: LayoutHelper): DisappearingItemChild {
        this.view = view
        this.helper = helper
        return this
    }

    override fun done() {
        pool.add(this)
    }

    override fun addToRecyclerView(i: Int) {
        helper.addDisappearingView(view, i)
    }
}

/**
 * A child which fakes measurement and layout, and never adds to the recycler view.
 */
private class DummyChild(helper: LayoutHelper) : ChildInternal(helper) {
    companion object {
        val pool = arrayListOf<DummyChild>()

        fun wrap(helper: LayoutHelper): DummyChild {
            return if (pool.isEmpty()) {
                DummyChild(helper)
            } else {
                pool.removeAt(0).reInit(helper)
            }
        }
    }

    private fun reInit(helper: LayoutHelper): DummyChild {
        this.helper = helper
        return this
    }

    override fun done() {
        pool.add(this)
    }

    override val isRemoved: Boolean get() = false
    private var _measuredWidth = 0
    override val measuredWidth: Int get() = _measuredWidth
    override val measuredHeight: Int get() = 0

    override fun measure(usedWidth: Int, usedHeight: Int) {
        _width = helper.layoutWidth - usedWidth
    }

    private var _left = 0
    private var _top = 0
    private var _right = 0
    private var _bottom = 0

    override val left: Int get() = _left
    override val top: Int get() = _top
    override val right: Int get() = _right
    override val bottom: Int get() = _bottom

    override fun layout(left: Int, top: Int, right: Int, bottom: Int) {
        _left = left
        _top = top
        _right = right
        _bottom = bottom

        _width = right - left
        _height = bottom - top
    }

    override fun fillTop(dy: Int, left: Int, top: Int, right: Int, bottom: Int): Int {
        layout(left, top, right, bottom)
        return 0
    }

    override fun fillBottom(dy: Int, left: Int, top: Int, right: Int, bottom: Int): Int {
        layout(left, top, right, bottom)
        return 0
    }

    private var _width = 0
    private var _height = 0

    override val width: Int get() = _width
    override val height: Int get() = _height

    override fun addToRecyclerView(i: Int) {
    }
}
