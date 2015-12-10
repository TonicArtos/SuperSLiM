package com.tonicartos.superslim.adapter

import com.tonicartos.superslim.SectionState
import com.tonicartos.superslim.slm.LinearSectionConfig
import java.util.*

sealed class Node {
    var positionInParent: Int = 0
    val positionInAdapter: Int
        get() = (parent?.positionInAdapter ?: 0) + peersItemsBeforeThis

    var parent: Section? = null
        internal set

    var totalItems: Int = 0
        protected set
    open val childCount: Int get() = 0

    fun removeFromParent() = parent?.remove(positionInParent)

    /**
     * Number of items held by other nodes before this one in the same section.
     */
    internal var peersItemsBeforeThis: Int = 0
    open internal var itemManager: ItemManager? = null
    internal abstract fun insertItemsToAdapter()
    internal abstract fun removeItemsFromAdapter()

    open internal fun init(positionInParent: Int, itemsBeforeThis: Int, parent: Section, itemManager: ItemManager?) {
        this.itemManager = itemManager
        this.parent = parent
        this.peersItemsBeforeThis = itemsBeforeThis
        this.positionInParent = positionInParent
    }

    open internal fun reset() {
        parent = null
        itemManager = null
    }

    abstract class ItemNode : Node {
        constructor() {
            totalItems = 1
        }
    }

    abstract class SectionNode : Node() {
    }
}

class Item(val type: Int = 0, val data: Any? = null) : Node.ItemNode() {
    internal override fun insertItemsToAdapter() {
        itemManager?.insert(positionInAdapter, this)
    }

    internal override fun removeItemsFromAdapter() {
        itemManager?.remove(positionInAdapter)
    }
}

open class Section(var scw: SectionChangeWatcher? = null) : Node.SectionNode(), Iterable<Node> {

    /**
     * An id assigned by the layout manager.
     */
    internal var id: Int = 0

    internal var registration: Registration<*>? = null
    fun deregister() {
        registration?.deregister()
    }

    var configuration: Section.Config = LinearSectionConfig()

    override var itemManager: ItemManager?
        get() = super.itemManager
        set(value) {
            super.itemManager = value
            children.forEach { child -> child.itemManager = value }
        }

    /*************************
     * Header stuff
     *************************/

    private var _header: Item? = null
        set(value) {
            _header = value
            configuration.hasHeader = value != null
        }

    var header: Item?
        get() = _header
        set(value) {
            if (value == null) {
                removeHeader()
                return
            }
            initChild(0, value)
            value.peersItemsBeforeThis = 0
            if (header != null) {
                itemManager?.set(positionInAdapter, value)
            } else {
                itemManager?.insert(positionInAdapter, value)
                children.forEach { it.peersItemsBeforeThis += 1 }
                totalItemsChanged(1)
            }
            _header = value
        }

    fun removeHeader() {
        header?.let { header ->
            header.removeItemsFromAdapter()
            totalItemsChanged(-1)
            children.forEach { child -> child.peersItemsBeforeThis -= 1 }
            _header = null
        }
    }

    /*************************
     * Expandable section stuff
     *************************/

    var collapsed: Boolean = false
        private set

    fun toggleChildren() = if (collapsed) expandChildren() else collapseChildren()

    private fun expandChildren() {
        var numItemsAdded = 0
        children.forEachIndexed { i, child ->
            initChild(i, child)
            child.insertItemsToAdapter()
            numItemsAdded += child.totalItems
        }
        totalItemsChanged(numItemsAdded)
        collapsed = false
    }

    private fun collapseChildren() {
        val jumpHeader = if (header == null) 0 else 1
        val numItemsToRemove = totalItems - jumpHeader
        itemManager?.removeRange(positionInAdapter + jumpHeader, numItemsToRemove)
        children.forEach { it.reset() }
        totalItemsChanged(-numItemsToRemove)
        collapsed = true
    }

    /*************************
     * Item management
     *************************/

    override internal fun insertItemsToAdapter() {
        scw?.notifySectionInserted(this)

        header?.itemManager = itemManager
        header?.insertItemsToAdapter()

        if (!collapsed) {
            for (child in children) {
                child.itemManager = itemManager
                child.insertItemsToAdapter()
            }
        }
    }

    override internal fun removeItemsFromAdapter() {
        scw?.notifySectionRemoved(this)
        itemManager?.removeRange(positionInAdapter, totalItems)
    }

    /****************************************************
     * Children stuff
     ****************************************************/

    private val _children: ArrayList<Node> = arrayListOf()
    val children: List<Node>
        get() = _children

    override val childCount: Int get() = children.size

    private fun initChild(position: Int, child: Node) {
        val numItemsBeforeChild =
                if (position > 0) {
                    val prior = children[position - 1]
                    prior.peersItemsBeforeThis + prior.totalItems
                } else {
                    if (header == null) 0 else 1
                }
        child.init(position, numItemsBeforeChild, this, itemManager)
    }

    /*************************
     * Child actions
     *************************/

    fun getAdapterPositionOfChild(position: Int): Int = children[position].positionInAdapter

    /**
     * Get child at [position]. Throws runtime exception if child isn't of the expected type.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T : Node> get(position: Int) = children[position] as T

    /**
     * Get child at [position]. Returns null if child isn't of the expected type.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Node> getOptional(position: Int) = children[position] as? T


    /**
     * Add child to end of children.
     */
    fun add(child: Node) {
        insert(children.size, child)
    }

    /**
     * Insert [child] into [position].
     */
    fun insert(position: Int, child: Node) {
        val dest = if (position !in 0..children.size - 1) children.size else position

        if (!collapsed) {
            initChild(dest, child)
            child.insertItemsToAdapter()

            // Update children after position.
            val numItemsAdded = child.totalItems
            children.subList(dest, children.size).forEach {
                it.peersItemsBeforeThis += numItemsAdded
                it.positionInParent += 1
            }
            totalItemsChanged(numItemsAdded)
        }
        _children.add(dest, child)
    }

    /**
     * Move child found at [from], to [to].
     */
    fun move(from: Int, to: Int) {
        if (from == to) return

        val moving = _children.removeAt(from)
        if (!collapsed) {
            moving.removeItemsFromAdapter()

            // Update children between from and to.
            val numItemsRemoved = moving.totalItems
            if (from < to) {
                children.subList(from, to).forEach {
                    it.peersItemsBeforeThis -= numItemsRemoved
                    it.positionInParent -= 1
                }
            } else {
                children.subList(to, from).forEach {
                    it.peersItemsBeforeThis += numItemsRemoved
                    it.positionInParent += 1
                }
            }

            // Re-init child in new place.
            initChild(to, moving)
            moving.insertItemsToAdapter()
        }
        _children.add(to, moving)
    }

    /**
     * Remove child at [position].
     */
    fun remove(position: Int) {
        val removed = _children.removeAt(position)

        if (!collapsed) {
            removed.removeItemsFromAdapter()

            // Update children after the removed child.
            val numItemsRemoved = removed.totalItems
            children.subList(position, children.size).forEach {
                it.peersItemsBeforeThis -= numItemsRemoved
                it.positionInParent -= 1
            }
            totalItemsChanged(-numItemsRemoved)
        }
        removed.reset()
    }

    /**
     * Replace child at [position] with [replacement].
     */
    operator fun set(position: Int, replacement: Node) {
        val toRemove = children[position]

        if (!collapsed) {
            toRemove.removeItemsFromAdapter()
            initChild(position, replacement)
            replacement.insertItemsToAdapter()

            // Update children after the target position if there is a change in total items.
            val numItemsAdded = replacement.totalItems - toRemove.totalItems
            if (numItemsAdded != 0) {
                children.subList(position + 1, children.size).forEach {
                    it.peersItemsBeforeThis += numItemsAdded
                }
                totalItemsChanged(numItemsAdded)
            }
        }

        _children[position] = replacement
        toRemove.reset()
    }

    /*************************
     * Listing and navigating children
     *************************/

    override fun iterator(): Iterator<Node> = children.iterator()

    /**
     * Returns a list containing all items matching the given [predicate].
     */
    inline fun filterItems(predicate: (Item) -> Boolean): List<Item> {
        return getItems().filterTo(ArrayList<Item>(), predicate)
    }

    /**
     * A list containing all items in the section.
     */
    @Suppress("WARNINGS")
    inline fun getItems() = children.filterTo(ArrayList<Node>(), { it -> it is Item }).map { it as Item }

    /**
     * Returns a list containing all sections matching the given [predicate].
     */
    inline fun filterSubsections(predicate: (Section) -> Boolean): List<Section> {
        return getSubsections().filterTo(ArrayList<Section>(), predicate)
    }

    /**
     * A list of all subsections.
     */
    @Suppress("WARNINGS")
    inline fun getSubsections() = children.filterTo(ArrayList<Node>(), { it -> it is Section }).map { it as Section }

    /**
     * Returns a list containing all elements matching the given [predicate].
     */
    inline fun filterChildren(predicate: (Node) -> Boolean): List<Node> {
        return children.filterTo(ArrayList<Node>(), predicate)
    }

    override fun init(positionInParent: Int, itemsBeforeThis: Int, parent: Section, itemManager: ItemManager?) {
        super.init(positionInParent, itemsBeforeThis, parent, itemManager)
        scw = parent.scw
    }

    override fun reset() {
        super.reset()
        scw = null
    }

    private fun totalItemsChanged(change: Int) {
        if (change == 0) return

        totalItems += change
        parent?.totalItemsChangedInChild(positionInParent, change)
    }

    private fun totalItemsChangedInChild(position: Int, change: Int) {
        if (change == 0) return

        // Update children after child that called in the change.
        children.subList(position + 1, children.size).forEach { it.peersItemsBeforeThis += change }

        totalItemsChanged(change)
    }

    /**
     * Configuration of a section.
     */
    abstract class Config(gutterStart: Int = Config.DEFAULT_GUTTER, gutterEnd: Int = Config.DEFAULT_GUTTER,
                          @HeaderStyle var headerStyle: Int = Config.DEFAULT_HEADER_STYLE) {

        var gutterStart: Int = 0
            set(value) {
                gutterStart = if (value < 0) GUTTER_AUTO else value
            }
        var gutterEnd: Int = 0
            set(value) {
                gutterEnd = if (value < 0) GUTTER_AUTO else value
            }

        internal var hasHeader = false

        init {
            this.gutterStart = gutterStart
            this.gutterEnd = gutterEnd
        }

        // Remap names since internally left and right are used since section coordinates are LTR, TTB. The start and
        // end intention will be applied correctly (from left and right) through the config transformations.
        internal var gutterLeft: Int
            get() = gutterStart
            set(value) {
                gutterStart = value
            }
        internal var gutterRight: Int
            get() = gutterEnd
            set(value) {
                gutterEnd = value
            }

        internal fun makeSection(oldState: SectionState? = null) = onMakeSection(oldState)
        abstract protected fun onMakeSection(oldState: SectionState?): SectionState

        companion object {
            /**
             * Header is positioned at the head of the section content. Content starts below the header. Inline headers
             * are always sticky. Use the embedded style if you want an inline header that is not sticky.
             */
            const val HEADER_INLINE = 1

            /**
             * Header is positioned at the head of the section content. Content starts below the header, but the header
             * never becomes sticky. Embedded headers can not float and ignores that flag if set.
             */
            const val HEADER_EMBEDDED = 1 shl 1

            /**
             * Header is placed inside the gutter at the start edge of the section. This is the left for LTR locales.
             * Gutter headers are always sticky.
             */
            const val HEADER_START = 1 shl 2

            /**
             * Header is placed inside the gutter at the end edge of the section. This is the right for LTR locales.
             * Gutter headers are always sticky. Overridden
             */
            const val HEADER_END = 1 shl 3

            /**
             * Float header above the content. Floating headers are always sticky.
             */
            const val HEADER_FLOAT = 1 shl 4

            /**
             * Header is placed at the tail of the section. If sticky, it will stick to the bottom edge rather than the
             * top. Combines with all other options.
             */
            const val HEADER_TAIL = 1 shl 5

            const val GUTTER_AUTO = -1

            internal const val DEFAULT_GUTTER = GUTTER_AUTO
            internal const val DEFAULT_HEADER_STYLE = HEADER_INLINE
        }
    }
}
