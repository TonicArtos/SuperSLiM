package com.tonicartos.superslim.internal

import android.os.Parcel
import android.os.Parcelable
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.NO_POSITION
import android.util.Log
import android.util.SparseArray
import android.view.View
import com.tonicartos.superslim.*
import com.tonicartos.superslim.SectionConfig.Companion.GUTTER_AUTO
import com.tonicartos.superslim.internal.layout.FooterLayoutManager
import com.tonicartos.superslim.internal.layout.HeaderLayoutManager
import com.tonicartos.superslim.internal.layout.PaddingLayoutManager
import java.util.*

private const val ENABLE_FOOTER = true
private const val ENABLE_HEADER = true
private const val ENABLE_PADDING = true
private val FOOTER_LAYER = 1
private val HEADER_LAYER = 1 + (if (ENABLE_FOOTER) 1 else 0)
private val PADDING_LAYER = 1 + (if (ENABLE_FOOTER) 1 else 0) + (if (ENABLE_HEADER) 1 else 0)
private const val ENABLE_ITEM_CHANGE_LOGGING = false

internal class GraphManager(adapter: AdapterContract<*>) {
    val root: SectionState = adapter.getRoot().makeSection()
    private val sectionIndex = SectionManager()

    init {
        // Init root
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
    internal val anchor get() = root.anchor

    internal var requestedAnchor: Anchor? = null

    fun layout(helper: RootLayoutHelper) {
        if (!helper.isPreLayout) {
            //doSectionMoves()
            doSectionUpdates()
        }

        requestedAnchor?.let {
            root.resetLayout()
            root.setLayoutPositionFromAnchor(it)
            requestedAnchor = null
        }
        root.layout(helper, 0, 0, helper.layoutWidth)
        val sectionHeight = root.height
        if (sectionHeight < helper.layoutLimit) {
            val overscroll = scrollBy(root.height - helper.layoutLimit, helper)
            if (overscroll != 0) helper.offsetChildrenVertical(overscroll)
        }

        if (helper.isPreLayout) {
            doSectionRemovals()
        }

//        if (hasRequestedPosition && requestedPositionOffset != 0) {
//            scrollBy(requestedPositionOffset, helper)
//        }
    }

    fun scrollBy(d: Int, helper: RootLayoutHelper): Int {
        requestedAnchor = null
//        Log.d("Graph", "scrollBy($d)")
        if (d == 0) return 0
        // If d is +ve, then scrolling to end.
        return if (d > 0) scrollTowardsBottom(d, helper) else -scrollTowardsTop(-d, helper)
    }

    /**
     * Scroll down.
     *
     * @param dy Distance to scroll up by. Expects +ve value.
     * @param helper Root layout helper.
     *
     * @return Actual distance scrolled.
     */
    private fun scrollTowardsBottom(dy: Int, helper: RootLayoutHelper): Int {
        val scrolled = Math.min(dy, fillBottom(dy, helper))
        if (scrolled > 0) {
            helper.offsetChildrenVertical(-scrolled)
        }

        trimTop(scrolled, helper)

        return scrolled
    }

    /**
     * Scroll up.
     *
     * @param dy Distance to scroll up by. Expects +ve value.
     * @param helper Root layout helper.
     *
     * @return Actual distance scrolled.
     */
    private fun scrollTowardsTop(dy: Int, helper: RootLayoutHelper): Int {
        val scrolled = Math.min(dy, fillTop(dy, helper))
//        Log.d("scrollUp", "dy = $dy, scroll by = $scrolled")

        if (scrolled > 0) {
            helper.offsetChildrenVertical(scrolled)
        }

        trimBottom(scrolled, helper)

        return scrolled
    }

    private fun fillTop(dy: Int, helper: RootLayoutHelper) = root.fillTop(dy, helper)
    private fun fillBottom(dy: Int, helper: RootLayoutHelper) = root.fillBottom(dy, helper)
    private fun trimTop(scrolled: Int, helper: RootLayoutHelper) = root.trimTop(scrolled, helper)
    private fun trimBottom(scrolled: Int, helper: RootLayoutHelper) = root.trimBottom(scrolled, helper)
    fun postLayout() = root.postLayout()

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
        for ((section, config) in sectionsToUpdate) {
            sectionIndex[section] = config.makeSection(sectionIndex[section])
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
    fun addHeader(sectionId: Int) {
        sectionIndex[sectionId].addHeader()
    }

    fun removeHeader(sectionId: Int) {
        sectionIndex[sectionId].removeHeader()
    }

    fun addFooter(sectionId: Int) {
        sectionIndex[sectionId].addFooter()
    }

    fun removeFooter(sectionId: Int) {
        sectionIndex[sectionId].removeFooter()
    }

    fun addItems(sectionId: Int, childStart: Int, itemCount: Int) {
        if (itemCount == 0) return
        sectionIndex[sectionId].addItems(childStart, itemCount)
    }

    fun removeItems(sectionId: Int, childStart: Int, itemCount: Int) {
        if (itemCount == 0) return
        sectionIndex[sectionId].removeItems(childStart, itemCount)
    }

    fun moveItems(fromSection: Int, toSection: Int, from: Int, to: Int, itemCount: Int) {
        if (itemCount == 0) return
        sectionIndex[fromSection].removeItems(from, itemCount)
        sectionIndex[toSection].addItems(to, itemCount)
    }

    override fun toString(): String {
        return "$root"
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

    operator fun get(id: Int): SectionState = sectionIndex[id]

    operator fun set(id: Int, newSection: SectionState) {
        sectionIndex.put(id, newSection)
    }
}

/**
 * Section data
 */
abstract class SectionState(val baseConfig: SectionConfig, oldState: SectionState? = null) {
    private companion object {
        const val ENABLE_LAYOUT_LOGGING = false
    }

    internal val anchor: Anchor get() = layoutState.babushka { state ->
        (state as? InternalLayoutState)
                ?.anchor(this@SectionState)
                ?: let { findAndWrap(state.headPosition, { it.anchor }, { Anchor(it, state.overdraw) }) }
    }

    override fun toString() = "Section: start=$positionInAdapter, totalItems=$totalItems, numChildren=$numChildren, numSections=${subsections.size}"

    /**
     * A stack of states. Plm, hlm, flm, slm. Except in special circumstances only the top one should be accessed at
     * a time.
     */
    private val layoutState = Stack<LayoutState>()
    internal val disappearedHeight get() = layoutState.peek().disappearedOrRemovedHeight
    internal val height get() = layoutState.peek().bottom
    internal val numViews get() = layoutState.peek().numViews
    internal fun resetLayout() {
        layoutState.forEach { it.reset() }
        subsections.forEach { it.resetLayout() }
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
     * Total number of items in the section, including the header, footer, and items in subsections.
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
        layoutState.push(LayoutState())
        if (ENABLE_FOOTER) layoutState.push(FooterLayoutState())
        if (ENABLE_HEADER) layoutState.push(HeaderLayoutState())
        if (ENABLE_PADDING) layoutState.push(PaddingLayoutState())
        if (oldState != null) {
            layoutState[0].copy(oldState.layoutState[0])
            if (ENABLE_FOOTER) layoutState[FOOTER_LAYER].copy(oldState.layoutState[FOOTER_LAYER])
            if (ENABLE_HEADER) layoutState[HEADER_LAYER].copy(oldState.layoutState[HEADER_LAYER])
            if (ENABLE_PADDING) layoutState[PADDING_LAYER].copy(oldState.layoutState[PADDING_LAYER])
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

    internal var hasHeader = false
    internal var hasFooter = false

    internal fun getHeader(helper: LayoutHelper): Child? =
            if (hasHeader) {
                ItemChild.wrap(positionInAdapter, helper)
            } else {
                null
            }

    internal fun getFooter(helper: LayoutHelper): Child? =
            if (hasFooter) {
                val footerPositionInAdapter = positionInAdapter + totalItems - 1
                ItemChild.wrap(footerPositionInAdapter, helper)
            } else {
                null
            }

    internal fun getDisappearingHeader(helper: LayoutHelper): Child? =
            if (hasHeader && helper.scrapHasPosition(positionInAdapter)) {
                DisappearingItemChild.wrap(positionInAdapter, helper)
            } else {
                null
            }

    internal fun getDisappearingFooter(helper: LayoutHelper): Child? {
        val footerPositionInAdapter = positionInAdapter + totalItems - 1
        return if (hasFooter && helper.scrapHasPosition(footerPositionInAdapter)) {
            DisappearingItemChild.wrap(footerPositionInAdapter, helper)
        } else {
            null
        }
    }

    /**
     * Gets a child at the specified position that might need more layout.
     *
     * @return Null if the child at the position is known to be fully laid out.
     */
    internal fun getNonFinalChildAt(helper: LayoutHelper, position: Int): Child?
            = findAndWrap(position, { SectionChild.wrap(it, helper) }, { null })

    /**
     * Get the child at the position.
     */
    internal fun getChildAt(helper: LayoutHelper, position: Int): Child
            = findAndWrap(position, { SectionChild.wrap(it, helper) },
                          { ItemChild.wrap(it, helper) })

    /**
     * Get a disappearing child. This is a special child which is used to correctly position disappearing views. Any
     * views not in the scrap list are virtual and are not fetched from the recycler. Instead a dummy item is returned
     * which behaves as a view with layout params(width = match_parent, height = 0).
     */
    internal fun getDisappearingChildAt(helper: LayoutHelper, position: Int): Child
            = findAndWrap<Child>(position, { SectionChild(it, helper) }, {
        if (helper.scrapHasPosition(it)) {
            DisappearingItemChild.wrap(it, helper)
        } else {
            DummyChild.wrap(helper)
        }
    })

    inline private fun <T> findAndWrap(position: Int, wrapSection: (SectionState) -> T,
                                       wrapItem: (viewPosition: Int) -> T): T {
        var hiddenItems = positionInAdapter + if (hasHeader) 1 else 0
        var lastSectionPosition = 0

        for ((i, it) in subsections.withIndex()) {
            if (it.positionInAdapter - hiddenItems + i > position) {
                break
            } else if (it.positionInAdapter - hiddenItems + i == position) {
                return wrapSection(it)
            } else {
                hiddenItems += it.totalItems
                lastSectionPosition = i
            }
        }
        return wrapItem(hiddenItems + position - lastSectionPosition)
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

    fun layout(helper: LayoutHelper, left: Int, top: Int, right: Int, numViewsBefore: Int = 0) {
        if (totalItems == 0) {
            layoutState.peek().reset()
            return
        }

        layoutState.babushka { state ->
            state.withPadding(helper) { paddingLeft, paddingTop, paddingRight, paddingBottom ->
                state.left = left + paddingLeft
                state.right = right - paddingRight
                state.numViews = 0

                helper.useSubsectionHelper(top, state.left, state.right, paddingTop, paddingBottom,
                                           helper.viewsBefore + numViewsBefore, state) { helper ->
                    state.layout(helper, this@SectionState)
                }
            }
        }
    }

    /**
     * First call into the root section. Comes from graph manager.
     */
    internal fun layout(rootHelper: RootLayoutHelper, left: Int, top: Int, right: Int) {
        if (totalItems == 0) {
            layoutState.peek().reset()
            return
        }

        // Use recycler view (base) padding.
        val paddingLeft = rootHelper.basePaddingLeft
        val paddingTop = rootHelper.basePaddingTop
        val paddingRight = rootHelper.basePaddingRight
        val paddingBottom = rootHelper.basePaddingBottom

        layoutState.babushka { state ->
            state.left = left + paddingLeft
            state.right = right - paddingRight
            state.numViews = 0

            rootHelper.useSubsectionHelper(top, state.left, state.right, paddingTop, paddingBottom, 0,
                                           state) { helper ->
                state.layout(helper, this@SectionState)
            }
        }
    }

    internal fun postLayout() {
        layoutState.asReversed().forEach { it.postLayout() }
        subsections.forEach { it.postLayout() }
    }

    internal val atTop get() = layoutState.babushka { it.atTop(this@SectionState) }

    fun isChildAtTop(position: Int) = findAndWrap(position, { it.atTop }, { true })

    /**
     * Set the layout position of this section.
     *
     * @return False if the position is not within this section.
     */
    internal fun setLayoutPositionFromAnchor(anchor: Anchor): Boolean {
        // Check requested position is in this section.
        if (anchor.position < positionInAdapter ||
                positionInAdapter + totalItems <= anchor.position) {
            return false
        }

        // Check if position needs special padding handling.
        if (ENABLE_PADDING) {
            val pls = layoutState[PADDING_LAYER] as PaddingLayoutState
            pls.headPosition = 0
            when (anchor.special) {
                Anchor.Special.IN_PADDING_TOP    -> {
                    pls.overdraw = anchor.overdraw
                    pls.set(PaddingLayoutState.TOP_ADDED)
                    return true
                }
                Anchor.Special.IN_PADDING_BOTTOM -> {
                    pls.overdraw = anchor.overdraw
                    pls.set(PaddingLayoutState.BOTTOM_ADDED)
                    return true
                }
                Anchor.Special.NOTHING           -> {
                    pls.unset(PaddingLayoutState.BOTTOM_ADDED or PaddingLayoutState.TOP_ADDED)
                    pls.onScreen = true
                }
            }
        }

        // Check if position is header.
        if (ENABLE_HEADER) {
            val hls = layoutState[HEADER_LAYER] as HeaderLayoutState
            if (hasHeader && anchor.position == positionInAdapter) {
                hls.headPosition = 0
                hls.overdraw = anchor.overdraw
                return true
            }
            hls.headPosition = 1
        }

        // Check if position is footer.
        if (ENABLE_FOOTER) {
            val fls = layoutState[FOOTER_LAYER] as FooterLayoutState
            if (hasFooter && anchor.position == positionInAdapter + totalItems - 1) {
                fls.headPosition = 1
                fls.overdraw = anchor.overdraw
                return true
            }
            fls.headPosition = 0
        }

        /*
         * Position is within content. It may be in a subsection or an item of this section. Calculating the child
         * position is a little difficult as it must account for interleaved child items and subsections.
         */
        val headerCount = if (hasHeader) 1 else 0
        var offset = positionInAdapter + headerCount
        var childrenAccountedFor = 0 + headerCount

        val sls = layoutState[0]
        for (section in subsections) {
            if (anchor.position < section.positionInAdapter) {
                // Position is before this subsection, so it must be a child item not a member of a subsection.
                sls.headPosition = childrenAccountedFor + anchor.position - offset - headerCount
                sls.overdraw = anchor.overdraw
                return true
            }

            // Add items before this subsection (but after the last subsection) to children count.
            childrenAccountedFor += section.positionInAdapter - offset

            if (section.setLayoutPositionFromAnchor(anchor)) {
                // Requested position was within the subsection so store it as the layout position of this section.
                sls.headPosition = childrenAccountedFor - headerCount
                return true
            }

            // Update offset to be after the subsection content.
            offset = section.positionInAdapter + section.totalItems
            // Account for this subsection.
            childrenAccountedFor += 1
        }

        // Position must be a child item after the last subsection.
        sls.headPosition = childrenAccountedFor + anchor.position - offset - headerCount
        sls.overdraw = anchor.overdraw
        return true
    }

    internal infix operator fun contains(viewHolder: RecyclerView.ViewHolder): Boolean {
//        Log.d("SADFASDF", "pia = $positionInAdapter, vh pos = ${((viewHolder.itemView as LinearLayout).getChildAt(0) as TextView).text} vh lay = ${viewHolder.layoutPosition}, ss end = ${positionInAdapter + totalItems - 1}")
        if (viewHolder.adapterPosition == NO_POSITION) return false
        return positionInAdapter <= viewHolder.layoutPosition
                && viewHolder.layoutPosition < positionInAdapter + totalItems
    }

    /*************************
     * Scrolling
     *************************/

    /****************
     * Root
     ****************/
    internal fun fillTop(dy: Int, rootHelper: RootLayoutHelper) =
            layoutState.babushka { state ->
                val paddingTop = rootHelper.basePaddingTop
                val paddingBottom = rootHelper.basePaddingBottom

                rootHelper.useSubsectionHelper(0, state.left, state.right, paddingTop, paddingBottom, 0,
                                               state) { helper ->
                    state.fillTop(dy, helper, this@SectionState)
                }
            }

    internal fun fillBottom(dy: Int, rootHelper: RootLayoutHelper) =
            layoutState.babushka { state ->
                val paddingTop = rootHelper.basePaddingTop
                val paddingBottom = rootHelper.basePaddingBottom

                rootHelper.useSubsectionHelper(0, state.left, state.right, paddingTop, paddingBottom, 0,
                                               state) { helper ->
                    state.fillBottom(dy, helper, this@SectionState)
                }
            }

    internal fun trimTop(scrolled: Int, rootHelper: RootLayoutHelper) {
        layoutState.babushka { state ->
            val paddingTop = rootHelper.basePaddingTop
            val paddingBottom = rootHelper.basePaddingBottom

            rootHelper.useSubsectionHelper(0, state.left, state.right, paddingTop, paddingBottom, 0,
                                           state) { helper ->
                state.trimTop(scrolled, helper, this@SectionState)
            }
        }
    }

    internal fun trimBottom(scrolled: Int, rootHelper: RootLayoutHelper) {
        layoutState.babushka { state ->
            val paddingTop = rootHelper.basePaddingTop
            val paddingBottom = rootHelper.basePaddingBottom

            rootHelper.useSubsectionHelper(0, state.left, state.right, paddingTop, paddingBottom, 0,
                                           state) { helper ->
                state.trimBottom(scrolled, helper, this@SectionState)
            }
        }
    }

    /***************
     * Sections
     ***************/
    internal fun fillTop(dy: Int, left: Int, top: Int, right: Int, helper: LayoutHelper, numViewsBefore: Int = 0) =
            layoutState.babushka { state ->
                state.withPadding(helper) { paddingLeft, paddingTop, paddingRight, paddingBottom ->
                    state.left = left + paddingLeft
                    state.right = right - paddingRight

                    helper.useSubsectionHelper(top, state.left, state.right, paddingTop, paddingBottom,
                                               helper.viewsBefore + numViewsBefore, state) {
                        state.fillTop(dy, it, this@SectionState)
                    }
                }
            }

    internal fun fillBottom(dy: Int, left: Int, top: Int, right: Int, helper: LayoutHelper,
                            numViewsBefore: Int = 0) =
            layoutState.babushka { state ->
                state.withPadding(helper) { paddingLeft, paddingTop, paddingRight, paddingBottom ->
                    state.left = left + paddingLeft
                    state.right = right - paddingRight

                    helper.useSubsectionHelper(top, state.left, state.right, paddingTop, paddingBottom,
                                               helper.viewsBefore + numViewsBefore, state) {
                        state.fillBottom(dy, it, this@SectionState)
                    }
                }
            }

    internal fun trimTop(scrolled: Int, top: Int, helper: LayoutHelper, numViewsBefore: Int = 0) =
            layoutState.babushka { state ->
                state.withPadding(helper) { paddingTop, paddingBottom ->
                    helper.useSubsectionHelper(top, state.left, state.right, paddingTop, paddingBottom,
                                               helper.viewsBefore + numViewsBefore, state) {
                        state.trimTop(scrolled, it, this@SectionState)
                    }
                }
            }

    internal fun trimBottom(scrolled: Int, top: Int, helper: LayoutHelper, numViewsBefore: Int = 0) =
            layoutState.babushka { state ->
                state.withPadding(helper) { paddingTop, paddingBottom ->
                    helper.useSubsectionHelper(top, state.left, state.right, paddingTop, paddingBottom,
                                               helper.viewsBefore + numViewsBefore, state) {
                        state.trimBottom(scrolled, it, this@SectionState)
                    }
                }
            }

    protected abstract fun isAtTop(layoutState: LayoutState): Boolean
    protected abstract fun doLayout(helper: LayoutHelper, layoutState: LayoutState)
    protected abstract fun doFillTop(dy: Int, helper: LayoutHelper, layoutState: LayoutState): Int
    protected abstract fun doFillBottom(dy: Int, helper: LayoutHelper, layoutState: LayoutState): Int
    protected abstract fun doTrimTop(scrolled: Int, helper: LayoutHelper, layoutState: LayoutState): Int
    protected abstract fun doTrimBottom(scrolled: Int, helper: LayoutHelper, layoutState: LayoutState): Int

    /*************************
     * Item management
     *************************/

    internal fun addHeader() {
        hasHeader = true
        subsections.forEach { it.positionInAdapter += 1 }
        totalItems += 1
    }

    internal fun addFooter() {
        hasFooter = true
        totalItems += 1
    }

    internal fun removeHeader() {
        hasHeader = false
        subsections.forEach { it.positionInAdapter -= 1 }
        totalItems -= 1
    }

    internal fun removeFooter() {
        hasFooter = false
        totalItems -= 1
    }

    internal fun addItems(position: Int, itemCount: Int) {
        var childPositionStart = position
        if (childPositionStart < 0) {
            childPositionStart = numChildren
        }

        applyToSubsectionsAfterChildPosition(childPositionStart) { _, it ->
            it.positionInAdapter += itemCount
        }

        numChildren += itemCount
        totalItems += itemCount
    }

    internal fun removeItems(fromAdapterPosition: Int, count: Int) {
        removeItemsInt(fromAdapterPosition, count)
    }

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

        if (hasFooter && currentRemoveFrom + itemsRemaining >= positionInAdapter + totalItems) {
            itemsRemoved += 1
            itemsRemaining -= 1
            hasFooter = false
        }

        if (itemsRemaining == 0) {
            totalItems -= itemsRemoved
            return itemsBeforeSection to itemsRemoved
        }

        var removedInSection = 0
        blockTotalItemChanges {
            for (subsection in subsections) {
                val (before, removed) = subsection.removeItemsInt(currentRemoveFrom, itemsRemaining)
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

    private inline fun applyToSubsectionsAfterChildPosition(childPositionStart: Int,
                                                            crossinline f: (Int, SectionState) -> Unit) {
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

    internal inline fun rightGutter(autoWidth: () -> Int)
            = if (baseConfig.gutterRight == GUTTER_AUTO) autoWidth() else baseConfig.gutterRight

    internal inline fun leftGutter(autoWidth: () -> Int)
            = if (baseConfig.gutterLeft == GUTTER_AUTO) autoWidth() else baseConfig.gutterLeft

    internal fun logGraph() {
        Log.d("GRAPH", "$this")
        layoutState.asReversed().forEach { Log.d("GRAPH", "$it") }
        subsections.forEach { it.logGraph() }
    }

    internal inline fun <R> LayoutState.withPadding(helper: LayoutHelper, block: (Int, Int) -> R): R {
        val paddingTop: Int
        val paddingBottom: Int

        if (this is PaddingLayoutState) {
            paddingTop = helper.getTransformedPaddingTop(baseConfig)
            paddingBottom = helper.getTransformedPaddingBottom(baseConfig)
        } else {
            paddingTop = 0
            paddingBottom = 0
        }
        return block(paddingTop, paddingBottom)
    }

    internal inline fun <R> LayoutState.withPadding(helper: LayoutHelper, block: (Int, Int, Int, Int) -> R): R {
        val paddingLeft: Int
        val paddingTop: Int
        val paddingRight: Int
        val paddingBottom: Int

        if (this is PaddingLayoutState) {
            paddingLeft = helper.getTransformedPaddingLeft(baseConfig)
            paddingTop = helper.getTransformedPaddingTop(baseConfig)
            paddingRight = helper.getTransformedPaddingRight(baseConfig)
            paddingBottom = helper.getTransformedPaddingBottom(baseConfig)
        } else {
            paddingLeft = 0
            paddingTop = 0
            paddingRight = 0
            paddingBottom = 0
        }
        return block(paddingLeft, paddingTop, paddingRight, paddingBottom)
    }

    open class LayoutState {
        companion object {
            const val UNSET_OR_BEFORE_CHILDREN: Int = -1
        }

        /**
         * Number of views.
         */
        var numViews = 0
            internal set(value) {
                field = value
            }

        /**
         * The height of the section for this layout pass. Only valid after section is laid out.
         */
        open var bottom = 0

        /**
         * Position that is the head of the displayed section content. -1 is the unset default value.
         */
        var headPosition = UNSET_OR_BEFORE_CHILDREN
            set(value) {
                field = value
            }
        /**
         * Position that is the tail of the displayed section content. -1 is the unset and default value.
         */
        var tailPosition = UNSET_OR_BEFORE_CHILDREN

        var left = 0
        var right = 0

        /**
         * Area drawn past y0 and dy.
         */
        var overdraw = 0

        /**
         * Height which will be removed from the section state after this layout pass.
         */
        var disappearedOrRemovedHeight = 0

        var numTemporaryViews = 0

        /**
         * Reset layout state.
         */
        internal open fun reset() {
            bottom = 0
            headPosition = UNSET_OR_BEFORE_CHILDREN
            tailPosition = UNSET_OR_BEFORE_CHILDREN
            left = 0
            right = 0
            overdraw = 0
            numViews = 0
        }

        internal fun copy(old: LayoutState) {
            bottom = old.bottom
            headPosition = old.headPosition
            tailPosition = old.tailPosition
        }

        override fun toString() = "($string)"

        protected open val string get() = "headPosition = $headPosition, tailPosition = $tailPosition, numViews = $numViews, left = $left, right = $right, height = $bottom, overdraw = $overdraw"

        open internal fun fillBottom(dy: Int, helper: LayoutHelper, section: SectionState)
                = section.doFillBottom(dy, helper, this)

        open internal fun fillTop(dy: Int, helper: LayoutHelper, section: SectionState)
                = section.doFillTop(dy, helper, this)

        open internal fun trimTop(scrolled: Int, helper: LayoutHelper, section: SectionState)
                = if (numViews == 0) 0 else section.doTrimTop(scrolled, helper, this)

        open internal fun trimBottom(scrolled: Int, helper: LayoutHelper, section: SectionState)
                = if (numViews == 0) 0 else section.doTrimBottom(scrolled, helper, this)

        open internal fun layout(helper: LayoutHelper, section: SectionState) = section.doLayout(helper, this)
        internal fun postLayout() {
            bottom -= disappearedOrRemovedHeight
            disappearedOrRemovedHeight = 0
            numViews -= numTemporaryViews
            numTemporaryViews = 0
            if (numViews == 0) reset()
        }

        open fun atTop(section: SectionState) = section.isAtTop(this)
    }

    internal abstract class InternalLayoutState : LayoutState() {
        /**
         * The current layout state mode.
         */
        var mode = 0

        override fun reset() {
            super.reset()
            mode = 0
        }

        override val string get() = "mode = $mode, ${super.string}"
        abstract fun anchor(section: SectionState): Anchor

        infix fun set(flag: Int) {
            mode = mode or flag
        }

        infix fun unset(flag: Int) {
            mode = mode and flag.inv()
        }

        infix fun flagSet(flag: Int) = mode and flag != 0
        infix fun flagUnset(flag: Int) = mode and flag == 0
    }

    internal class PaddingLayoutState : InternalLayoutState() {
        companion object {
            internal const val TOP_ADDED = 1 shl 0
            internal const val BOTTOM_ADDED = 1 shl 1
        }

        var paddingTop = 0
        var paddingBottom = 0

        override val string get() = "paddingTop = $paddingTop, paddingBottom = $paddingBottom, ${super.string}"

        override fun anchor(section: SectionState) = if (flagSet(TOP_ADDED)) {
            Anchor(section.positionInAdapter, overdraw, Anchor.Special.IN_PADDING_TOP)
        } else if (flagUnset(TOP_ADDED) && flagSet(BOTTOM_ADDED) && bottom + overdraw == paddingBottom) {
            Anchor(section.positionInAdapter, overdraw, Anchor.Special.IN_PADDING_BOTTOM)
        } else {
            section.anchor
        }

        override fun atTop(section: SectionState) = PaddingLayoutManager.isAtTop(section, this)

        override fun layout(helper: LayoutHelper, section: SectionState) {
            PaddingLayoutManager.onLayout(helper, section, this)
        }

        override fun fillBottom(dy: Int, helper: LayoutHelper, section: SectionState)
                = PaddingLayoutManager.onFillBottom(dy, helper, section, this)

        override fun fillTop(dy: Int, helper: LayoutHelper, section: SectionState)
                = PaddingLayoutManager.onFillTop(dy, helper, section, this)

        override fun trimTop(scrolled: Int, helper: LayoutHelper, section: SectionState)
                = PaddingLayoutManager.onTrimTop(scrolled, helper, section, this)

        override fun trimBottom(scrolled: Int, helper: LayoutHelper, section: SectionState)
                = PaddingLayoutManager.onTrimBottom(scrolled, helper, section, this)

        override fun toString() = "Padding ${super.toString()}"

        var onScreen get() = headPosition == 0
            set(value) {
                headPosition = if (value) 0 else -1
            }
    }

    internal class HeaderLayoutState : InternalLayoutState() {
        override fun anchor(section: SectionState) = if (section.hasHeader && headPosition == 0) {
            Anchor(section.positionInAdapter, overdraw)
        } else {
            section.anchor
        }

        override fun atTop(section: SectionState) = HeaderLayoutManager.isAtTop(section, this)

        override fun layout(helper: LayoutHelper, section: SectionState) {
            HeaderLayoutManager.onLayout(helper, section, this)
        }

        override fun fillBottom(dy: Int, helper: LayoutHelper, section: SectionState)
                = HeaderLayoutManager.onFillBottom(dy, helper, section, this)

        override fun fillTop(dy: Int, helper: LayoutHelper, section: SectionState)
                = HeaderLayoutManager.onFillTop(dy, helper, section, this)

        override fun trimTop(scrolled: Int, helper: LayoutHelper, section: SectionState)
                = if (numViews == 0) 0 else HeaderLayoutManager.onTrimTop(scrolled, helper, section, this)

        override fun trimBottom(scrolled: Int, helper: LayoutHelper, section: SectionState)
                = if (numViews == 0) 0 else HeaderLayoutManager.onTrimBottom(scrolled, helper, section, this)

        override fun toString() = "Header ${super.toString()}"
    }

    internal class FooterLayoutState : InternalLayoutState() {
        override fun anchor(section: SectionState) = if (section.hasFooter && headPosition == 1) {
            Anchor(section.positionInAdapter + section.totalItems - 1, overdraw)
        } else {
            section.anchor
        }

        override fun atTop(section: SectionState) = FooterLayoutManager.isAtTop(section, this)

        override fun layout(helper: LayoutHelper, section: SectionState) {
            FooterLayoutManager.onLayout(helper, section, this)
        }

        override fun fillBottom(dy: Int, helper: LayoutHelper, section: SectionState)
                = FooterLayoutManager.onFillBottom(dy, helper, section, this)

        override fun fillTop(dy: Int, helper: LayoutHelper, section: SectionState)
                = FooterLayoutManager.onFillTop(dy, helper, section, this)

        override fun trimTop(scrolled: Int, helper: LayoutHelper, section: SectionState)
                = if (numViews == 0) 0 else FooterLayoutManager.onTrimTop(scrolled, helper, section, this)

        override fun trimBottom(scrolled: Int, helper: LayoutHelper, section: SectionState)
                = if (numViews == 0) 0 else FooterLayoutManager.onTrimBottom(scrolled, helper, section, this)

        override fun toString() = "Footer ${super.toString()}"
    }
}

internal abstract class ChildInternal(var helper: LayoutHelper) : Child {
    override fun trimTop(scrolled: Int, top: Int, helper: LayoutHelper, numViewsBefore: Int) = 0
    override fun trimBottom(scrolled: Int, top: Int, helper: LayoutHelper, numViewsBefore: Int) = 0
}

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

    override val numViews get() = section.numViews
    override val isRemoved get() = false
    private var _measuredWidth = 0
    override val measuredWidth get() = _measuredWidth
    override val measuredHeight get() = Child.INVALID

    override fun measure(usedWidth: Int, usedHeight: Int) {
        _measuredWidth = helper.layoutWidth - usedWidth
    }

    override var left = 0
    override var top = 0
    override var right = 0
    override val bottom get() = Child.INVALID

    override fun layout(left: Int, top: Int, right: Int, bottom: Int, numViewsBefore: Int) {
        this.left = left
        this.top = top
        this.right = right
        section.layout(helper, left, top, right, numViewsBefore)
    }

    override fun fillTop(dy: Int, left: Int, top: Int, right: Int, bottom: Int, numViewsBefore: Int): Int {
        return section.fillTop(dy, left, top, right, helper, numViewsBefore)
    }

    override fun trimTop(scrolled: Int, top: Int, helper: LayoutHelper, numViewsBefore: Int): Int {
        return section.trimTop(scrolled, top, helper, numViewsBefore)
    }

    override fun fillBottom(dy: Int, left: Int, top: Int, right: Int, bottom: Int, numViewsBefore: Int): Int {
        return section.fillBottom(dy, left, top, right, helper, numViewsBefore)
    }

    override fun trimBottom(scrolled: Int, top: Int, helper: LayoutHelper, numViewsBefore: Int): Int {
        return section.trimBottom(scrolled, top, helper, numViewsBefore)
    }

    override val width get() = right - left
    override val height get() = section.height
    override val disappearedHeight get() = section.disappearedHeight

    override fun addToRecyclerView(i: Int) {
    }
}

private open class ItemChild(var view: View, helper: LayoutHelper, var positionInAdapter: Int) : ChildInternal(
        helper) {
    companion object {
        val pool = arrayListOf<ItemChild>()

        fun wrap(pos: Int, helper: LayoutHelper): ItemChild {
            val view = helper.getView(pos)
            return if (pool.isEmpty()) {
                ItemChild(view, helper, pos)
            } else {
                pool.removeAt(0).reInit(view, helper, pos)
            }
        }
    }

    private fun reInit(view: View, helper: LayoutHelper, positionInAdapter: Int): ItemChild {
        this.view = view
        this.helper = helper
        this.positionInAdapter = positionInAdapter
        return this
    }

    override fun done() {
        pool.add(this)
    }

    override val isRemoved get() = view.rvLayoutParams.isItemRemoved

    override val measuredWidth get() = helper.getMeasuredWidth(view)
    override val measuredHeight get() = helper.getMeasuredHeight(view)

    override fun measure(usedWidth: Int, usedHeight: Int) {
        helper.measure(view, usedWidth, usedHeight)
    }

    override val left get() = helper.getLeft(view)
    override val top get() = helper.getTop(view)
    override val right get() = helper.getRight(view)
    override val bottom get() = helper.getBottom(view)

    override fun layout(left: Int, top: Int, right: Int, bottom: Int, numViewsBefore: Int) {
        val m = view.rvLayoutParams
        helper.layout(view, left, top, right, bottom, m.leftMargin, m.topMargin, m.rightMargin, m.bottomMargin)
    }

    override fun fillTop(dy: Int, left: Int, top: Int, right: Int, bottom: Int, numViewsBefore: Int): Int {
        layout(left, top, right, bottom, numViewsBefore)
        return height
    }

    override fun fillBottom(dy: Int, left: Int, top: Int, right: Int, bottom: Int, numViewsBefore: Int): Int {
        layout(left, top, right, bottom, numViewsBefore)
        return height
    }

    override val width get() = helper.getMeasuredWidth(view)
    override val height get() = helper.getMeasuredHeight(view)
    override val disappearedHeight get() = 0

    override fun addToRecyclerView(i: Int) {
        when {
            isRemoved -> helper.addTemporaryView(view, i)
            else      -> helper.addView(view, i)
        }
    }
}

private class DisappearingItemChild(view: View, helper: LayoutHelper, positionInAdapter: Int) :
        ItemChild(view, helper, positionInAdapter) {
    companion object {
        val pool = arrayListOf<DisappearingItemChild>()

        fun wrap(pos: Int, helper: LayoutHelper): DisappearingItemChild {
            val view = helper.getView(pos)
            return if (pool.isEmpty()) {
                DisappearingItemChild(view, helper, pos)
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

    val isDisappeared get() = true
    override val disappearedHeight get() = height
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

    override val isRemoved get() = false
    private var _measuredWidth = 0
    override val measuredWidth get() = _measuredWidth
    override val measuredHeight get() = 0
    override val disappearedHeight get() = 0

    override fun measure(usedWidth: Int, usedHeight: Int) {
        _width = helper.layoutWidth - usedWidth
    }

    private var _left = 0
    private var _top = 0
    private var _right = 0
    private var _bottom = 0

    override val left get() = _left
    override val top get() = _top
    override val right get() = _right
    override val bottom get() = _bottom

    override fun layout(left: Int, top: Int, right: Int, bottom: Int, numViewsBefore: Int) {
        _left = left
        _top = top
        _right = right
        _bottom = bottom

        _width = right - left
        _height = bottom - top
    }

    override fun fillTop(dy: Int, left: Int, top: Int, right: Int, bottom: Int, numViewsBefore: Int): Int {
        layout(left, top, right, bottom, numViewsBefore)
        return height
    }

    override fun fillBottom(dy: Int, left: Int, top: Int, right: Int, bottom: Int, numViewsBefore: Int): Int {
        layout(left, top, right, bottom, numViewsBefore)
        return height
    }

    private var _width = 0
    private var _height = 0

    override val width get() = _width
    override val height get() = _height

    override fun addToRecyclerView(i: Int) {
    }
}

internal data class Anchor(val position: Int, val overdraw: Int = 0,
                           val special: Special = Anchor.Special.NOTHING) : Parcelable {
    companion object {
        @JvmField val CREATOR: Parcelable.Creator<Anchor> = object : Parcelable.Creator<Anchor> {
            override fun createFromParcel(source: Parcel): Anchor = Anchor(source)
            override fun newArray(size: Int): Array<Anchor?> = arrayOfNulls(size)
        }
    }

    constructor(source: Parcel) : this(source.readInt(), source.readInt(), Special.values()[source.readInt()])

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest ?: return
        dest.writeInt(position)
        dest.writeInt(special.ordinal)
        dest.writeInt(overdraw)
    }

    enum class Special { IN_PADDING_TOP, IN_PADDING_BOTTOM, NOTHING }
}
