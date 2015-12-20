package com.tonicartos.superslim.internal

import android.support.annotation.VisibleForTesting

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.util.SparseArray
import android.view.View
import com.tonicartos.superslim.*
import com.tonicartos.superslim.internal.layout.HeaderLayoutManager
import java.util.*

internal class GraphManager(adapter: AdapterContract<*>) {
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

    /*************************
     * Layout
     *************************/

    fun layout(helper: LayoutHelper) {
        if (!helper.isPreLayout) {
            //doSectionMoves()
            doSectionUpdates()
        }

        Log.d("layout", "helper = $helper\nsection = $root\n\n")
        root.layout(helper, 0, 0, helper.layoutWidth)

        if (helper.isPreLayout) {
            doSectionRemovals()
        }
    }

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
        if (BuildConfig.DEBUG) Log.d("Sslm-DC events", "addItems(event = $eventSectionData, positionStart = $positionStart, itemCount = $itemCount)")
        val section = sectionIndex[eventSectionData.section]
        if (eventSectionData.action and EventSectionData.HEADER > 0) {
            if (itemCount > 1) throw IllegalArgumentException("Expected item count of 1 for add header operation.")
            section.addHeader()
        } else {
            section.addItems(eventSectionData.start, positionStart, itemCount)
        }
    }

    fun removeItems(eventSectionData: EventSectionData, positionStart: Int, itemCount: Int) {
        if (BuildConfig.DEBUG) Log.d("Sslm-DC events", "removeItems(event = $eventSectionData, positionStart = $positionStart, itemCount = $itemCount)")
        val section = sectionIndex[eventSectionData.section]
        if (eventSectionData.action and EventSectionData.HEADER > 0) {
            if (itemCount > 1) throw IllegalArgumentException("Expected item count of 1 for remove header operation.")
            section.removeHeader()
        } else {
            section.removeItems(eventSectionData.start, positionStart, itemCount)
        }
    }

    fun moveItems(eventSectionData: EventSectionData, from: Int, to: Int) {
        if (BuildConfig.DEBUG) Log.d("Sslm-DC events", "moveItem(eventData = $eventSectionData, from = $from, to = $to)")
        sectionIndex[eventSectionData.fromSection].removeItems(eventSectionData.from, from, 1)
        sectionIndex[eventSectionData.toSection].addItems(eventSectionData.to, to, 1)
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
    /**
     * The height of the section for this layout pass. Only valid after section is laid out, and never use outside the
     * same layout pass.
     */
    var height: Int = 0

    /**
     * Position that is the head of the displayed section content.
     */
    var headPosition: Int = 0
    /**
     * Position that is the tail of the displayed section content.
     */
    var tailPosition: Int = 0

    /**
     * Total number of children. Children does not equate to items as some subsections may be empty.
     */
    var numChildren: Int = 0
        private set

    /**
     * Total number of items in the section, including the header and items in subsections.
     */
    private var totalItems: Int = 0
    internal var parent: SectionState? = null

    /**
     * Sorted list of subsections.
     */
    private val subsections: ArrayList<SectionState>

    /**
     * Position of this section in the adapter.
     */
    private var adapterPosition = 0
        get() = field
        set(value) {
            subsections.forEach { it.adapterPosition += value - field }
            field = value
        }

    init {
        if (oldState != null) {
            height = oldState.height
            headPosition = oldState.headPosition
            tailPosition = oldState.tailPosition
            totalItems = oldState.totalItems
            numChildren = oldState.numChildren
            subsections = oldState.subsections
            adapterPosition = oldState.adapterPosition
            parent = oldState.parent
        } else {
            subsections = ArrayList()
        }
    }

    internal var hasHeader: Boolean = false

    internal fun getHeader(helper: LayoutHelper): ChildInternal? =
            if (hasHeader) {
                ItemChild.wrap(helper.getView(adapterPosition), helper)
            } else {
                null
            }

    fun getChildAt(helper: LayoutHelper, position: Int): Child {
        var hiddenItems = adapterPosition + if (hasHeader) 1 else 0
        var lastSectionPosition = 0
        for ((i, it) in subsections.withIndex()) {
            if (it.adapterPosition - hiddenItems + i > position) {
                break
            } else if (it.adapterPosition - hiddenItems + i == position) {
                return SectionChild.wrap(it, helper)
            } else {
                hiddenItems += it.totalItems
                lastSectionPosition = i
            }
        }

        return ItemChild.wrap(helper.getView(hiddenItems + position - lastSectionPosition), helper)
    }

    final fun layout(helper: LayoutHelper, left: Int, top: Int, right: Int) {
        val subsectionHelper = helper.acquireSubsectionHelper(left, top, right)
        HeaderLayoutManager.onLayout(subsectionHelper, this)
        subsectionHelper.release()
    }

    final internal fun layoutContent(helper: LayoutHelper, left: Int, top: Int, right: Int) {
        val subsectionHelper = helper.acquireSubsectionHelper(left, top, right)
        doLayout(subsectionHelper)
        subsectionHelper.release()
    }

    protected abstract fun doLayout(helper: LayoutHelper)

    infix operator fun contains(
            viewHolder: RecyclerView.ViewHolder): Boolean = viewHolder.adapterPosition >= adapterPosition && viewHolder.adapterPosition < adapterPosition + totalItems

    /*************************
     * Item management
     *************************/

    internal fun addHeader() {
        hasHeader = true
        subsections.forEach { it.adapterPosition += 1 }
        parent?.itemCountsChangedInSubsection(this, 1)
        totalItems += 1
    }

    internal fun removeHeader() {
        hasHeader = false
        subsections.forEach { it.adapterPosition -= 1 }
        parent?.itemCountsChangedInSubsection(this, -1)
        totalItems -= 1
    }

    internal fun addItems(childPositionStart: Int, adapterPositionStart: Int, itemCount: Int) {
        applyToSubsectionsAfterChildPosition(childPositionStart) { i, it ->
            it.adapterPosition += itemCount
        }

        numChildren += itemCount
        totalItems += itemCount
        parent?.itemCountsChangedInSubsection(this, itemCount)
    }

    internal fun removeItems(childPositionStart: Int, adapterPositionStart: Int, itemCount: Int) {
        var currentAdapterStart = adapterPositionStart
        var itemsRemaining = itemCount
        var itemsThatAreChildren = 0

        var itemsRemoved = 0

        for (it in subsections) {
            var (skipped, removed) = it.removeItems(currentAdapterStart, itemsRemaining)
            it.adapterPosition -= skipped + itemsRemoved
            itemsRemoved += removed
            itemsThatAreChildren += skipped
            currentAdapterStart += removed + skipped
            itemsRemaining -= removed + skipped
        }

        itemsThatAreChildren += itemsRemaining
        parent?.itemCountsChangedInSubsection(this, -itemCount)
        totalItems -= itemCount
        numChildren -= itemsThatAreChildren
        hasHeader = adapterPositionStart != adapterPosition
    }

    private fun removeItems(adapterPositionStart: Int, itemCount: Int): Pair<Int, Int> {
        if (itemCount == 0) return 0 to 0
        val itemsBeforeSection = Math.min(itemCount, Math.max(0, adapterPosition - adapterPositionStart))
        val itemsAfterSection = Math.max(0, itemCount + adapterPositionStart - totalItems - adapterPosition)

        var currentAdapterStart = Math.max(adapterPosition, adapterPositionStart)
        var itemsRemaining = itemCount - itemsBeforeSection - itemsAfterSection
        var itemsThatAreChildren = 0
        var itemsRemoved = 0

        if (currentAdapterStart == adapterPosition) {
            itemsRemoved += 1
            itemsRemaining -= 1
            currentAdapterStart += 1
            hasHeader = false
        }

        if (itemsRemaining == 0) return itemsBeforeSection to 0

        for (subsection in subsections) {
            var (skipped, removed) = subsection.removeItems(currentAdapterStart, itemsRemaining)
            subsection.adapterPosition -= skipped + itemsRemoved
            itemsThatAreChildren += skipped
            currentAdapterStart += removed + skipped
            itemsRemaining -= removed + skipped
            itemsRemoved += removed
            if (itemsRemaining == 0) break
        }

        itemsThatAreChildren += itemsRemaining
        totalItems -= itemsRemoved + itemsThatAreChildren
        numChildren -= itemsThatAreChildren
        itemsRemoved += itemsThatAreChildren

        return itemsBeforeSection to itemsRemoved
    }

    /**
     * An ancestor has removed all items in this section.
     */
    private fun allItemsWereRemoved() {
        totalItems = 0
        hasHeader = false
        numChildren = subsections.size
        subsections.forEach { it.allItemsWereRemoved() }
    }

    private fun itemCountsChangedInSubsection(child: SectionState, changedCount: Int) {
        // Find child and adjust adapter position for sections after it.
        val index = subsections.indexOfFirst { it == child }
        if (index + 1 < subsections.size) {
            for (i in (index + 1)..(subsections.size - 1)) {
                subsections[i].adapterPosition += changedCount
            }
        }
        totalItems += changedCount
        parent?.itemCountsChangedInSubsection(this, changedCount)
    }

    /*************************
     * Section management
     *************************/

    internal fun insertSection(position: Int, newSection: SectionState) {
        var insertPoint = subsections.size
        var firstTime = true
        applyToSubsectionsAfterChildPosition(position) { i, it ->
            it.adapterPosition += newSection.totalItems
            if (firstTime) {
                insertPoint = i
                firstTime = false
            }
        }
        subsections.add(insertPoint, newSection)

        newSection.parent = this
        numChildren += 1
        totalItems += newSection.totalItems
        parent?.itemCountsChangedInSubsection(this, newSection.totalItems)
    }

    internal fun removeSection(section: SectionState) {
        var indexOfSection: Int = -1
        var afterSection = false
        subsections.forEachIndexed { i, it ->
            if (afterSection) {
                it.adapterPosition -= section.totalItems
            } else if (it === section) {
                indexOfSection = i
                afterSection = true
            }
        }
        if (indexOfSection == -1) return

        subsections.removeAt(indexOfSection)
        totalItems -= section.totalItems
        numChildren -= 1
        parent?.itemCountsChangedInSubsection(this, -section.totalItems)
    }

    internal fun load(data: SectionData) {
        numChildren = data.childCount
        adapterPosition = data.adapterPosition
        hasHeader = data.hasHeader
        totalItems = data.itemCount
        subsections.clear()
        subsections.addAll(data.subsections)
    }

    private inline fun applyToSubsectionsAfterChildPosition(childPositionStart: Int, f: (Int, SectionState) -> Unit) {
        var hiddenItems = adapterPosition
        var applying = false
        subsections.forEachIndexed { i, it ->
            if (applying) {
                f(i, it)
            } else if (it.adapterPosition - hiddenItems + i >= childPositionStart) {
                f(i, it)
                applying = true
            } else {
                hiddenItems += it.totalItems
            }
        }
    }

    override fun toString(): String = "SectionState(start = $adapterPosition, hasHeader = $hasHeader, numChildren = $numChildren, totalItems = $totalItems, numSubsections = ${subsections.size}, subgraph = ${subsections.fold("") { s, it -> "$s\n$it" }})".replace("\n", "\n\t")

    /****************************************************
     * Test access to private members. Proguard will remove these in release.
     *
     * TODO: Configure proguard to remove these things in release.
     ****************************************************/
    interface TestAccess {
        val totalItems: Int
        val subsections: ArrayList<SectionState>
        val adapterPosition: Int
        fun addItems(childPositionStart: Int, adapterPositionStart: Int, itemCount: Int)
        fun removeItems(childPositionStart: Int, adapterPositionStart: Int, itemCount: Int)
        fun addHeader()
        fun removeHeader()
        fun removeSection(section: SectionState)
        fun insertSection(position: Int, newSection: SectionState)
    }

    /**
     * Testing access to internal and private members of the instance. This will be removed in release by proguard.
     *
     * TODO: Proguard rule.
     */
    @VisibleForTesting
    val testAccess = object : TestAccess {
        override val totalItems: Int get() = this@SectionState.totalItems
        override val subsections: ArrayList<SectionState> get() = this@SectionState.subsections
        override val adapterPosition: Int get() = this@SectionState.adapterPosition

        override fun addItems(childPositionStart: Int, adapterPositionStart: Int, itemCount: Int) = this@SectionState.addItems(childPositionStart, adapterPositionStart, itemCount)
        override fun removeItems(childPositionStart: Int, adapterPositionStart: Int, itemCount: Int) = this@SectionState.removeItems(childPositionStart, adapterPositionStart, itemCount)

        override fun addHeader() = this@SectionState.addHeader()
        override fun removeHeader() = this@SectionState.removeHeader()

        override fun insertSection(position: Int, newSection: SectionState) = this@SectionState.insertSection(position, newSection)
        override fun removeSection(section: SectionState) = this@SectionState.removeSection(section)
    }
}

internal abstract class ChildInternal(var helper: LayoutHelper) : Child {
    @AnimationState override var animationState: Int = Child.ANIM_NONE
}

private class SectionChild(var section: SectionState, helper: LayoutHelper) : ChildInternal(helper) {
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

    private var _left = 0
    override val left: Int
        get() = _left

    private var _top = 0
    override val top: Int
        get() = _top

    private var _right = 0
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

    override val width: Int
        get() = _right - _left
    override val height: Int
        get() = section.height

    override fun addToRecyclerView(i: Int) {
    }
}

private class ItemChild(var view: View, helper: LayoutHelper) : ChildInternal(helper) {
    companion object {
        val pool = arrayListOf<ItemChild>()

        fun wrap(view: View, helper: LayoutHelper): ItemChild {
            return if (pool.isEmpty()) {
                ItemChild(view, helper)
            } else {
                pool[0].reInit(view, helper)
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

    override val width: Int
        get() = helper.getMeasuredWidth(view)
    override val height: Int
        get() = helper.getMeasuredHeight(view)

    override fun addToRecyclerView(i: Int) {
        val helper = this.helper
        val view = this.view
        when (animationState) {
            Child.ANIM_APPEARING, Child.ANIM_NONE -> helper.addView(view, i)
            Child.ANIM_DISAPPEARING               -> helper.addDisappearingView(view, i)
        }
    }
}