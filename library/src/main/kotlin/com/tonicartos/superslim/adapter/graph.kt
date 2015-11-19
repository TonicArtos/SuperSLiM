package com.tonicartos.superslim.adapter

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
    internal var itemManager: ItemManager? = null
    internal abstract fun insertItemsToAdapter()
    internal abstract fun removeItemsFromAdapter()

    internal fun init(positionInParent: Int, itemsBeforeThis: Int, parent: Section, itemManager: ItemManager?) {
        this.itemManager = itemManager
        this.parent = parent
        this.peersItemsBeforeThis = itemsBeforeThis
        this.positionInParent = positionInParent
    }

    internal fun reset() {
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

open class Section() : Node.SectionNode(), Iterable<Node> {
    internal constructor(itemManager: ItemManager) : this() {
        this.itemManager = itemManager
    }

    internal var registration: Registration<*>? = null
    fun deregister() {
        registration?.deregister()
    }

    var configuration: Config = LinearSectionConfig()


    private var _header: Item? = null
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
        }
    }


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


    private val _children: ArrayList<Node> = arrayListOf()
    val children: List<Node>
        get() = _children

    override val childCount: Int get() = children.size

    override fun iterator(): Iterator<Node> = children.iterator()


    override fun insertItemsToAdapter() {
        header?.itemManager = itemManager
        header?.insertItemsToAdapter()

        if (!collapsed) {
            for (child in children) {
                child.itemManager = itemManager
                child.insertItemsToAdapter()
            }
        }
    }

    override fun removeItemsFromAdapter() {
        itemManager?.removeRange(positionInAdapter, totalItems)
    }

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


    abstract class Config {
        constructor(customSlmLabel: String, headerMarginStart: Int = DEFAULT_MARGIN, headerMarginEnd: Int = DEFAULT_MARGIN, @HeaderStyle headerStyle: Int = DEFAULT_HEADER_STYLE) : this(headerMarginStart, headerMarginEnd, headerStyle) {
            this.customSlmLabel = customSlmLabel
        }

        constructor(headerMarginStart: Int = DEFAULT_MARGIN, headerMarginEnd: Int = DEFAULT_MARGIN, @HeaderStyle headerStyle: Int = DEFAULT_HEADER_STYLE) {
            this.headerMarginStart = headerMarginStart
            this.headerMarginEnd = headerMarginEnd
            this.headerStyle = headerStyle
        }

        @HeaderStyle
        var headerStyle: Int
        var headerMarginStart: Int
        var headerMarginEnd: Int
        var customSlmLabel: String? = null
            private set
        open val slmKind: Int = CUSTOM_SLM

        companion object {
            /**
             * Header is positioned at the top of the section content. Content starts below the
             * header. Inline headers are always sticky. Use the embedded style if you want an
             * inline header that is not sticky.
             */
            const val HEADER_INLINE = 1

            /**
             * Header is positioned at the top of the section content. Content starts below the
             * header, but the header never becomes sticky. Embedded headers may not be overlays
             * either.
             */
            const val HEADER_EMBEDDED = 2

            /**
             * Header is aligned to the start edge of the section. This is the left for LTR
             * locales. Start aligned headers are always sticky.
             */
            const val HEADER_START = 4

            /**
             * Header is aligned to the end edge of the section. This is the right for LTR locales.
             * End aligned headers are always sticky.
             */
            const val HEADER_END = 8

            /**
             * Overlay headers float above the content and are always sticky.
             */
            const val HEADER_OVERLAY = 16
            const val MARGIN_AUTO = -1
            internal const val CUSTOM_SLM = 0
            internal const val DEFAULT_MARGIN = MARGIN_AUTO
            internal const val DEFAULT_HEADER_STYLE = HEADER_INLINE
        }
    }
}
