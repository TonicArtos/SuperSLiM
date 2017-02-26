package com.tonicartos.superslim

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.IntDef
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.tonicartos.superslim.BuildConfig.DEBUG
import com.tonicartos.superslim.internal.*

internal interface AdapterContract<ID> {
    fun getRoot(): SectionConfig
    fun setRootId(id: Int)
    fun populateRoot(out: SectionData)

    fun getSections(): Map<ID, SectionConfig>
    fun setSectionIds(idMap: Map<*, Int>)
    fun populateSection(data: Pair<*, SectionData>)

    fun onLayoutManagerAttached(layoutManager: SuperSlimLayoutManager)
    fun onLayoutManagerDetached(layoutManager: SuperSlimLayoutManager)
}

class SectionData {
    var adapterPosition: Int = 0
    var itemCount: Int = 0
    var hasHeader = false
    var childCount = 0
    var subsectionsById = emptyList<Int>()
    internal var subsections = emptyList<SectionState>()
}

class SuperSlimLayoutManager : RecyclerView.LayoutManager, ManagerHelper, ReadWriteLayoutHelper {
    @JvmOverloads @Suppress("unused")
    constructor(@Suppress("unused_parameter") context: Context, @Orientation orientation: Int = VERTICAL,
                reverseLayout: Boolean = false, stackFromEnd: Boolean = false) {
        this.orientation = orientation
        this.reverseLayout = reverseLayout
        this.stackFromEnd = stackFromEnd
    }

    @Suppress("unused")
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) {
        val properties = getProperties(context, attrs, defStyleAttr, defStyleRes)
        properties ?: return
        orientation = properties.orientation
        reverseLayout = properties.reverseLayout
        stackFromEnd = properties.stackFromEnd
    }

    companion object {
        const val TAG = "SuperSlimLayoutManager"
        const val VERTICAL: Int = RecyclerView.VERTICAL
        const val HORIZONTAL: Int = RecyclerView.HORIZONTAL

        private const val ENABLE_NOTIFICATION_LOGGING = false
        private const val ENABLE_ITEM_CHANGE_LOGGING = false
        private const val ENABLE_LAYOUT_LOGGING = false
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams? {
        throw UnsupportedOperationException()
    }

    /****************************************************
     * Layout
     ****************************************************/
    private val recyclerHelper = RecyclerWrapper()
    private val stateHelper = StateWrapper()

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        pendingSavedState?.let {
            if (state.itemCount == 0) {
                removeAndRecycleAllViews(recycler)
                return
            }

            if (it.position > 0) {
                graph?.requestedPosition = it.position
                graph?.requestedPositionOffset = it.offset
            }
        }

        if (ENABLE_LAYOUT_LOGGING) {
            if (state.isPreLayout) Log.d("SSlm", "Prelayout")
            else if (!state.willRunPredictiveAnimations()) Log.d("Sslm", "layout")
            else Log.d("Sslm", "Postlayout")

            Log.d("Sslm-graph", "$graph")
        }
        detachAndScrapAttachedViews(recycler)
        graph?.layout(RootLayoutHelper(this, configHelper, recyclerHelper.wrap(recycler), stateHelper.wrap(state)))
    }

    /****************************************************
     * Scrolling
     ****************************************************/

    override fun canScrollVertically() = orientation == VERTICAL

    override fun canScrollHorizontally() = orientation == HORIZONTAL

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        val scrolled = graph?.scrollBy(dy, RootLayoutHelper(this, configHelper, recyclerHelper.wrap(recycler),
                                                            stateHelper.wrap(state))) ?: 0
        return scrolled
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        val scrolled = graph?.scrollBy(dx, RootLayoutHelper(this, configHelper, recyclerHelper.wrap(recycler),
                                                            stateHelper.wrap(state))) ?: 0
        return scrolled
    }

    override fun scrollToPosition(position: Int) {
        graph?.apply {
            requestedPosition = position
            requestedPositionOffset = -paddingTop
            requestLayout()
        }
    }

    fun scrollToPositionWithOffset(position: Int, offset: Int) {
        graph?.apply {
            requestedPosition = position
            requestedPositionOffset = offset - paddingTop
            requestLayout()
        }
    }

    /****************************************************
     * Scroll indicator computation
     ****************************************************/

    override fun computeVerticalScrollExtent(state: RecyclerView.State?): Int {
        // TODO: compute scroll
        return super.computeVerticalScrollExtent(state)
    }

    override fun computeVerticalScrollRange(state: RecyclerView.State?): Int {
        // TODO: compute scroll
        return super.computeVerticalScrollRange(state)
    }

    override fun computeVerticalScrollOffset(state: RecyclerView.State?): Int {
        // TODO: compute scroll
        return super.computeVerticalScrollOffset(state)
    }

    override fun computeHorizontalScrollExtent(state: RecyclerView.State?): Int {
        // TODO: compute scroll
        return super.computeHorizontalScrollExtent(state)
    }

    override fun computeHorizontalScrollRange(state: RecyclerView.State?): Int {
        // TODO: compute scroll
        return super.computeHorizontalScrollRange(state)
    }

    override fun computeHorizontalScrollOffset(state: RecyclerView.State?): Int {
        // TODO: compute scroll
        return super.computeHorizontalScrollOffset(state)
    }

    /*************************
     * Graph
     *************************/

    private var adapterContract: AdapterContract<*>? = null

    override fun onAdapterChanged(oldAdapter: RecyclerView.Adapter<*>?, newAdapter: RecyclerView.Adapter<*>?) {
        super.onAdapterChanged(oldAdapter, newAdapter)
        if (oldAdapter == newAdapter) return

        adapterContract?.onLayoutManagerDetached(this)
        if (newAdapter == null) {
            graph = null
            return
        }
        contractAdapter(newAdapter)
    }

    override fun onAttachedToWindow(view: RecyclerView) {
        super.onAttachedToWindow(view)
        val adapter: RecyclerView.Adapter<*>? = view.adapter
        if (adapterContract == adapter) return
        if (adapter == null) {
            graph = null
            return
        }
        contractAdapter(adapter)
    }

    //    override fun onDetachedFromWindow(view: RecyclerView?, recycler: RecyclerView.Recycler?) {
    //        super.onDetachedFromWindow(view, recycler)
    //        adapterContract?.onLayoutManagerDetached(this)
    //        adapterContract = null
    //        graph.reset()
    //    }

    override fun onItemsChanged(view: RecyclerView) {
        graph = GraphManager(adapterContract ?: return)
    }

    private fun contractAdapter(adapter: RecyclerView.Adapter<*>) {
        val contract = adapter as? AdapterContract<*> ?:
                throw IllegalArgumentException("adapter does not implement AdapterContract")
        contract.onLayoutManagerAttached(this)
        adapterContract = contract
        graph = GraphManager(contract)
    }

    /*************************
     * Configuration
     *************************/

    var orientation: Int = VERTICAL
        get() = field
        set(@Orientation value) {
            if (value != HORIZONTAL && value != VERTICAL) throw IllegalArgumentException(
                    "invalid orientation: {$value}")
            assertNotInLayoutOrScroll(null)
            if (orientation == value) return
            field = value
            configChanged = true
            requestLayout()
        }

    var reverseLayout = false
        get() = field
        set(value) {
            assertNotInLayoutOrScroll(null)
            if (field == value) return
            field = value
            configChanged = true
            requestLayout()
        }

    var stackFromEnd = false
        get() = field
        set(value) {
            assertNotInLayoutOrScroll(null)
            if (field == value) return
            field = value
            configChanged = true
            requestLayout()
        }

    //TODO: Test configuration setup.
    private var configChanged = true
    private var configHelper: ReadWriteLayoutHelper = this
        get() = field.takeUnless { configChanged } ?: let {
            // Build chain of configuration transformations.
            field = this
            if (orientation == HORIZONTAL) field = HorizontalConfigHelper(field)
            if (stackFromEnd) field = StackFromEndConfigHelper(field)
            if (reverseLayout) field = ReverseLayoutConfigHelper(field)
            if (layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL) field = RtlConfigHelper(field)
            configChanged = false
            field
        }

    /****************************************************
     * ReadWriteLayoutHelper implementation
     ****************************************************/

    override val layoutLimit get() = height
    override val layoutWidth get() = width

    override val basePaddingBottom get() = paddingBottom
    override val basePaddingRight get() = paddingRight
    override val basePaddingTop get() = paddingTop
    override val basePaddingLeft get() = paddingLeft

    override fun getTransformedPaddingLeft(sectionConfig: SectionConfig): Int = sectionConfig.paddingLeft
    override fun getTransformedPaddingTop(sectionConfig: SectionConfig): Int = sectionConfig.paddingTop
    override fun getTransformedPaddingRight(sectionConfig: SectionConfig): Int = sectionConfig.paddingRight
    override fun getTransformedPaddingBottom(sectionConfig: SectionConfig): Int = sectionConfig.paddingBottom

    override fun getMeasuredHeight(child: View) = getDecoratedMeasuredHeight(child)
    override fun getMeasuredWidth(child: View) = getDecoratedMeasuredWidth(child)
    override fun getLeft(child: View) = getDecoratedLeft(child)
    override fun getTop(child: View) = getDecoratedTop(child)
    override fun getRight(child: View) = getDecoratedRight(child)
    override fun getBottom(child: View) = getDecoratedBottom(child)

    override fun detachViewAtPosition(position: Int) = getChildAt(position).also { detachViewAt(position) }
    override fun attachViewToPosition(position: Int, view: View) = attachView(view, position)
    override fun measure(view: View, usedWidth: Int, usedHeight: Int) = measureChildWithMargins(view, usedWidth,
                                                                                                usedHeight)

    override fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int,
                        marginLeft: Int, marginTop: Int, marginRight: Int, marginBottom: Int) =
            layoutDecorated(view, left, top, right, bottom)

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

    private var graph: GraphManager? = null
    private val itemChangeHelper = ItemChangeHelper()

    /*************************
     * Section changes from adapter
     *************************/

    /**
     * Notify that a section has been added. The returned id is immediately valid.
     *
     * @param[parent] Id of parent section.
     * @param[position] Position in parent section.
     * @param[config] Section configuration.
     */
    fun notifySectionAdded(parent: Int, position: Int, config: SectionConfig): Int {
        if (ENABLE_NOTIFICATION_LOGGING) Log.d("SSlm-DCs",
                                               "notifySectionAdded(parent: $parent, position: $position, config: $config)")
        // Always copy the config as soon as it enters this domain.
        return graph!!.sectionAdded(parent, position, config.copy())
    }

    /**
     * Notify that a section is to be removed. The removal happens after all layout passes, whereupon the section id
     * becomes invalid.
     */
    fun notifySectionRemoved(section: Int, parent: Int) {
        if (ENABLE_NOTIFICATION_LOGGING) Log.d("SSlm-DCs", "notifySectionAdded(section: $section, parent: $parent)")
        graph!!.queueSectionRemoved(section, parent)
    }

    //    fun notifySectionMoved(section: Int, fromParent: Int, fromPosition: Int, toParent: Int, toPosition: Int) {
    //        graph.queueSectionMoved(section, fromParent, fromPosition, toParent, toPosition)
    //    }

    /**
     * Notify that a section has been changed. This indicates a configuration change for the section. The effect is
     * applied in the post-layout, which means items should animate between the two states, pre-update, and port-update.
     * It may be necessary to notify changes for at least one item in the section.
     */
    fun notifySectionUpdated(section: Int, config: SectionConfig) {
        if (ENABLE_NOTIFICATION_LOGGING) Log.d("SSlm-DCs", "notifySectionAdded(section: $section, config: $config)")
        // Always copy the config as soon as it enters this domain.
        graph!!.queueSectionUpdated(section, config.copy())
    }

    /*************************
     * Section item changes from adapter
     *************************/

    fun notifySectionHeaderAdded(section: Int, adapterPosition: Int) {
        if (ENABLE_NOTIFICATION_LOGGING) Log.d("Sslm-DCs",
                                               "notifySectionHeaderAdded(section: $section, position: $adapterPosition)")
        itemChangeHelper.queueSectionHeaderAdded(section, 0, adapterPosition)
    }

    fun notifySectionHeaderRemoved(section: Int, adapterPosition: Int) {
        if (ENABLE_NOTIFICATION_LOGGING) Log.d("Sslm-DCs",
                                               "notifySectionHeaderRemoved(section: $section, position: $adapterPosition)")
        itemChangeHelper.queueSectionHeaderRemoved(section, 0, adapterPosition)
    }

    fun notifySectionItemsAdded(section: Int, start: Int, startAdapterPosition: Int, itemCount: Int) {
        if (ENABLE_NOTIFICATION_LOGGING) Log.d("Sslm-DCs",
                                               "notifySectionItemsAdded(section: $section, positionStart: $start, itemCount: $itemCount)")
        itemChangeHelper.queueSectionItemsAdded(section, start, startAdapterPosition, itemCount)
    }

    fun notifySectionItemsRemoved(section: Int, start: Int, startAdapterPosition: Int, itemCount: Int) {
        if (ENABLE_NOTIFICATION_LOGGING) Log.d("Sslm-DCs",
                                               "notifySectionItemsRemoved(section: $section, positionStart: $start, itemCount: $itemCount)")
        itemChangeHelper.queueSectionItemsRemoved(section, start, startAdapterPosition, itemCount)
    }

    fun notifySectionItemsMoved(fromSection: Int, from: Int, fromAdapterPosition: Int, toSection: Int, to: Int,
                                toAdapterPosition: Int) {
        if (ENABLE_NOTIFICATION_LOGGING) Log.d("Sslm-DCs",
                                               "notifySectionItemsMoved(fromSection: $fromSection, from: $from, toSection: $toSection, to: $to)")
        itemChangeHelper.queueSectionItemsMoved(fromSection, from, fromAdapterPosition, toSection, to,
                                                toAdapterPosition)
    }

    /*************************
     * Item changes from recycler view
     *************************/

    override fun onItemsAdded(recyclerView: RecyclerView?, positionStart: Int, itemCount: Int) {
        if (ENABLE_ITEM_CHANGE_LOGGING) Log.d("Sslm-DCs",
                                              "onItemsAdded(position: $positionStart, itemCount: $itemCount)")
        val event = itemChangeHelper.pullAddEventData(positionStart, itemCount)
        graph!!.addItems(event, positionStart, itemCount)
    }

    var bugCount = 0

    override fun onItemsRemoved(recyclerView: RecyclerView?, positionStart: Int, itemCount: Int) {
        if (ENABLE_ITEM_CHANGE_LOGGING) Log.d("Sslm-DCs",
                                              "onItemsRemoved(position: $positionStart, itemCount: $itemCount)")
        val event = itemChangeHelper.pullRemoveEventData(positionStart, itemCount)
        graph!!.removeItems(event, positionStart, itemCount)
    }

    override fun onItemsMoved(recyclerView: RecyclerView?, from: Int, to: Int, itemCount: Int) {
        if (ENABLE_ITEM_CHANGE_LOGGING) Log.d("Sslm-DCs", "onItemsMoved(from: $from, to: $to, itemCount: $itemCount)")
        val event = itemChangeHelper.pullMoveEventData(from, to)
        graph!!.moveItems(event, from, to)
    }

    /*************************
     * State management
     *************************/

    class SavedState(val position: Int, val offset: Int) : Parcelable {
        constructor(anchor: Pair<Int, Int>) : this(anchor.first, anchor.second)
        constructor(source: Parcel) : this(position = source.readInt(), offset = source.readInt())
        constructor(other: SavedState) : this(other.position, other.offset)

        companion object {
            @JvmField @Suppress("unused")
            val CREATOR = createParcel(::SavedState)
        }

        override fun describeContents() = 0
        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(position)
            dest.writeInt(offset)
        }
    }

    var pendingSavedState: SavedState? = null
    override val supportsPredictiveItemAnimations get() = pendingSavedState == null
    override fun supportsPredictiveItemAnimations() = pendingSavedState == null

    override fun assertNotInLayoutOrScroll(message: String?) {
        if (pendingSavedState == null) super.assertNotInLayoutOrScroll(message)
    }

    override fun onSaveInstanceState(): Parcelable? = graph?.let { SavedState(it.anchor) }

    override fun onRestoreInstanceState(state: Parcelable?) {
        pendingSavedState = state as? SavedState
        pendingSavedState?.let { requestLayout() }

        if (DEBUG) {
            when (pendingSavedState) {
                null -> Log.d(TAG, "Invalid saved state.")
                else -> Log.d(TAG, "Loaded saved state.")
            }
        }
    }
}

@IntDef(SuperSlimLayoutManager.HORIZONTAL.toLong(),
        SuperSlimLayoutManager.VERTICAL.toLong())
@Retention(AnnotationRetention.SOURCE)
annotation class Orientation
