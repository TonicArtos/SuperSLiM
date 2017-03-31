package com.tonicartos.superslim

import android.content.Context
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

    fun getData(position: Int): Data

    fun onLayoutManagerAttached(layoutManager: SuperSlimLayoutManager)
    fun onLayoutManagerDetached(layoutManager: SuperSlimLayoutManager)

    companion object {
        val data: Data = DataImpl(0, 0, Data.OTHER)
    }

    interface Data {
        companion object {
            const val HEADER = 0
            const val FOOTER = 1
            const val OTHER = 2
        }

        val section: Int
        val position: Int
        val isHeader: Boolean
        val isFooter: Boolean
        fun pack(section: Int, position: Int, type: Int): Data
    }

    private class DataImpl(override var section: Int, override var position: Int, var type: Int) : Data {
        override val isHeader get() = type == Data.HEADER
        override val isFooter get() = type == Data.FOOTER
        override fun pack(section: Int, position: Int, type: Int): Data {
            this.section = section
            this.position = position
            this.type = type
            return this
        }
    }
}

class SectionData {
    var adapterPosition: Int = 0
    var itemCount: Int = 0
    var hasHeader = false
    var childCount = 0
    var subsectionsById = emptyList<Int>()
    internal var subsections = emptyList<SectionState>()
}

class SuperSlimLayoutManager() : RecyclerView.LayoutManager(), ManagerHelper, ConfigHelper,
                                 ItemManagement by ItemManager() {
    @JvmOverloads @Suppress("unused")
    constructor(@Suppress("unused_parameter") context: Context, @Orientation orientation: Int = VERTICAL,
                reverseLayout: Boolean = false, stackFromEnd: Boolean = false) : this() {
        this.orientation = orientation
        this.reverseLayout = reverseLayout
        this.stackFromEnd = stackFromEnd
    }

    @Suppress("unused")
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : this() {
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

    override fun addTemporaryView(child: View) {
        addView(child)
    }

    override fun addTemporaryView(child: View, index: Int) {
        addView(child, index)
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        val adapter = adapterContract ?: return
        val graph = graph ?: return

        applyChanges(adapter, graph, recycler)

        pendingSavedState?.let {
            if (state.itemCount == 0) {
                removeAndRecycleAllViews(recycler)
                return
            }

            if (it.position >= 0) {
                graph.requestedAnchor = it
            }
            pendingSavedState = null
//        } ?: let {
//            Log.d("SavedSate", "Forced position to 5.")
//            graph?.requestedPosition = 5
        }

        if (ENABLE_LAYOUT_LOGGING) {
            if (state.isPreLayout) Log.d("SSlm", "Prelayout")
            else if (!state.willRunPredictiveAnimations()) Log.d("Sslm", "layout")
            else Log.d("Sslm", "Postlayout")

            Log.d("Sslm-graph", "$graph")
        }
        detachAndScrapAttachedViews(recycler)
        graph.layout(RootLayoutHelper(this, configHelper, recyclerHelper.wrap(recycler), stateHelper.wrap(state)))
        graph.postLayout()
    }

    override fun offsetHorizontal(view: View, dx: Int) = view.offsetLeftAndRight(dx)
    override fun offsetVertical(view: View, dy: Int) = view.offsetTopAndBottom(dy)

    override fun removeView(child: View, recycler: RecyclerView.Recycler) {
        super.removeAndRecycleView(child, recycler)
    }

    /****************************************************
     * Scrolling
     ****************************************************/

    override fun canScrollVertically() = orientation == VERTICAL

    override fun canScrollHorizontally() = orientation == HORIZONTAL

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State)
            = configHelper.scrollBy(dy, recycler, state)

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State)
            = configHelper.scrollBy(dx, recycler, state)

    override fun scrollBy(d: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State)
            = graph?.scrollBy(d, RootLayoutHelper(this, configHelper, recyclerHelper.wrap(recycler),
                                                  stateHelper.wrap(state))) ?: 0

    override fun scrollToPosition(position: Int) {
        graph?.apply {
            requestedAnchor = Anchor(position)
            requestLayout()
        }
    }

// TODO: Scroll to position with offset.
//    fun scrollToPositionWithOffset(position: Int, offset: Int) {
//        graph?.apply {
//            requestedPosition = position
//            requestedOffset = offset // Offsets layout and forces fill top.
//            requestLayout()
//        }
//    }

// TODO: Custom find views.
//    override fun findViewByPosition(position: Int) = graph?.findViewByPosition(position, this)

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
    private var configHelper: ConfigHelper = this
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

    override var stickyStartInset = 0
    override var stickyEndInset = 0
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

    override fun getMeasuredHeight(child: View): Int = getDecoratedMeasuredHeight(child)
    override fun getMeasuredWidth(child: View): Int = getDecoratedMeasuredWidth(child)
    override fun getLeft(child: View): Int = getDecoratedLeft(child)
    override fun getTop(child: View): Int = getDecoratedTop(child)
    override fun getRight(child: View): Int = getDecoratedRight(child)
    override fun getBottom(child: View): Int = getDecoratedBottom(child)

    override fun getAttachedRawView(position: Int): View {
        require(position in 0..(childCount - 1))
        return getChildAt(position)
    }

    override fun detachViewAtPosition(position: Int) = getChildAt(position)?.also { detachViewAt(position) }
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
        if (ENABLE_NOTIFICATION_LOGGING) Log.d("SSlm",
                                               "sectionAdded(parent: $parent, position: $position, config: $config)")
        // Always copy the config as soon as it enters this domain.
        return graph!!.sectionAdded(parent, position, config.copy())
    }

    /**
     * Notify that a section is to be removed. The removal happens after all layout passes, whereupon the section id
     * becomes invalid.
     */
    fun notifySectionRemoved(section: Int, parent: Int) {
        if (ENABLE_NOTIFICATION_LOGGING) Log.d("SSlm", "sectionAdded(section: $section, parent: $parent)")
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
        if (ENABLE_NOTIFICATION_LOGGING) Log.d("SSlm", "sectionAdded(section: $section, config: $config)")
        // Always copy the config as soon as it enters this domain.
        graph!!.queueSectionUpdated(section, config.copy())
    }

    /*************************
     * Item changes from recycler view
     *************************/

    override fun onItemsAdded(recyclerView: RecyclerView?, positionStart: Int, itemCount: Int) {
        if (ENABLE_ITEM_CHANGE_LOGGING) Log.d("Sslm", "itemsAdded(position: $positionStart, itemCount: $itemCount)")
        addItems(positionStart, itemCount)
    }

    override fun onItemsRemoved(recyclerView: RecyclerView?, positionStart: Int, itemCount: Int) {
        if (ENABLE_ITEM_CHANGE_LOGGING) Log.d("Sslm", "itemsRemoved(position: $positionStart, itemCount: $itemCount)")
        removeItems(positionStart, itemCount)
    }

    override fun onItemsMoved(recyclerView: RecyclerView?, from: Int, to: Int, itemCount: Int) {
        if (ENABLE_ITEM_CHANGE_LOGGING) Log.d("Sslm", "itemsMoved(from: $from, to: $to, itemCount: $itemCount)")
        moveItems(from, to, itemCount)
    }

    /*************************
     * State management
     *************************/
    private var pendingSavedState: Anchor? = null
    override val supportsPredictiveItemAnimations get() = pendingSavedState == null
    override fun supportsPredictiveItemAnimations() = pendingSavedState == null

    override fun assertNotInLayoutOrScroll(message: String?) {
        if (pendingSavedState == null) super.assertNotInLayoutOrScroll(message)
    }

    override fun onSaveInstanceState(): Parcelable? = graph?.anchor

    override fun onRestoreInstanceState(state: Parcelable?) {
        pendingSavedState = state as? Anchor
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
