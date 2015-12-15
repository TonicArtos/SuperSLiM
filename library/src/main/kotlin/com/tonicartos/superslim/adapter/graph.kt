package com.tonicartos.superslim.adapter

import com.tonicartos.superslim.SectionConfig
import com.tonicartos.superslim.internal.layout.LinearSectionConfig
import java.util.*

sealed class Node {
    var positionInParent: Int = 0
    val positionInAdapter: Int
        get() = (parent?.positionInAdapter ?: 0) + peersItemsBeforeThis

    var parent: Section? = null
        internal set

    var itemCount: Int = 0
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
            itemCount = 1
        }
    }

    abstract class SectionNode : Node() {
    }
}

open class Item(val type: Int = 0, val data: Any? = null) : Node.ItemNode() {
    internal override fun insertItemsToAdapter() {
        itemManager?.insert(positionInAdapter, this)
    }

    internal override fun removeItemsFromAdapter() {
        itemManager?.remove(positionInAdapter)
    }
}

class Section internal constructor(var scw: SectionChangeWatcher? = null) : Node.SectionNode(), Iterable<Node> {
    /**
     * An id assigned by the layout manager.
     */
    internal var id: Int = -1

    internal var registration: Registration<*>? = null
    fun deregister() {
        registration?.deregister()
    }

    private var _configuration: SectionConfig = LinearSectionConfig()
    var configuration: SectionConfig
        get() = _configuration
        set(value) {
            _configuration = value
            scw?.notifySectionUpdated(this)
        }

    override var itemManager: ItemManager?
        get() = super.itemManager
        set(value) {
            super.itemManager = value
            children.forEach { child -> child.itemManager = value }
        }

    /*************************
     * Header management
     *************************/

    private var _header: Item? = null

    var header: Item?
        get() = _header
        set(value) {
            configuration.hasHeader = value != null
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
                scw?.notifySectionHeaderInserted(this)
                children.forEach { it.peersItemsBeforeThis += 1 }
                totalItemsChanged(1)
            }
            _header = value
        }

    fun removeHeader() {
        header?.let { header ->
            header.removeItemsFromAdapter()
            scw?.notifySectionHeaderRemoved(this)
            totalItemsChanged(-1)
            children.forEach { child -> child.peersItemsBeforeThis -= 1 }
            _header = null
        }
    }

    private fun fulfillRemoveHeaderContract() {

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
            if (child is ItemNode) {
                scw?.notifySectionItemsInserted(this, child.positionInAdapter, 1)
            }
            numItemsAdded += child.itemCount
        }
        totalItemsChanged(numItemsAdded)
        collapsed = false
    }

    private fun collapseChildren() {
        val jumpHeader = if (header == null) 0 else 1
        val numItemsToRemove = itemCount - jumpHeader
        itemManager?.removeRange(positionInAdapter + jumpHeader, numItemsToRemove)
        scw?.notifySectionItemsRemoved(this, positionInAdapter + jumpHeader, numItemsToRemove)
        children.forEach { it.reset() }
        totalItemsChanged(-numItemsToRemove)
        collapsed = true
    }

    /*************************
     * Item management
     *************************/

    override internal fun insertItemsToAdapter() {
        id = scw?.notifySectionInserted(this) ?: -1

        header?.let {
            it.itemManager = itemManager
            it.insertItemsToAdapter()
            scw?.notifySectionHeaderInserted(this)
        }

        if (!collapsed) {
            for (child in children) {
                child.itemManager = itemManager
                child.insertItemsToAdapter()
                if (child is ItemNode) {
                    scw?.notifySectionItemsInserted(this, child.positionInAdapter, 1)
                }
            }
        }
    }

    override internal fun removeItemsFromAdapter() {
        scw?.notifySectionRemoved(this)

        itemManager?.removeRange(positionInAdapter, itemCount)
        if (header == null) {
            scw?.notifySectionItemsRemoved(this, positionInAdapter, itemCount)
        } else {
            scw?.notifySectionHeaderInserted(this)
            scw?.notifySectionItemsRemoved(this, positionInAdapter + 1, itemCount)
        }
    }

    /****************************************************
     * Children stuff
     ****************************************************/

    private val mutableChildren: ArrayList<Node> = arrayListOf()
    val children: List<Node>
        get() = mutableChildren

    override val childCount: Int get() = children.size

    private fun initChild(position: Int, child: Node) {
        val numItemsBeforeChild =
                if (position > 0) {
                    val prior = children[position - 1]
                    prior.peersItemsBeforeThis + prior.itemCount
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
            val numItemsAdded = child.itemCount
            children.subList(dest, children.size).forEach {
                it.peersItemsBeforeThis += numItemsAdded
                it.positionInParent += 1
            }
            totalItemsChanged(numItemsAdded)
        }
        mutableChildren.add(dest, child)
    }

    /**
     * Move child found at [from], to [to].
     */
    fun move(from: Int, to: Int) {
        if (from == to) return

        val moving = mutableChildren.removeAt(from)
        if (!collapsed) {
            moving.removeItemsFromAdapter()

            // Update children between from and to.
            val numItemsRemoved = moving.itemCount
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
        mutableChildren.add(to, moving)
    }

    /**
     * Remove child at [position].
     */
    fun remove(position: Int) {
        val removed = mutableChildren.removeAt(position)

        if (!collapsed) {
            removed.removeItemsFromAdapter()

            // Update children after the removed child.
            val numItemsRemoved = removed.itemCount
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
            val numItemsAdded = replacement.itemCount - toRemove.itemCount
            if (numItemsAdded != 0) {
                children.subList(position + 1, children.size).forEach {
                    it.peersItemsBeforeThis += numItemsAdded
                }
                totalItemsChanged(numItemsAdded)
            }
        }

        mutableChildren[position] = replacement
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

        itemCount += change
        parent?.totalItemsChangedInChild(positionInParent, change)
    }

    private fun totalItemsChangedInChild(position: Int, change: Int) {
        if (change == 0) return

        // Update children after child that called in the change.
        children.subList(position + 1, children.size).forEach { it.peersItemsBeforeThis += change }

        totalItemsChanged(change)
    }

}

internal class GraphImpl() : Graph, SectionChangeWatcher {
    internal val root: Section

    init {
        root = Section(this)
    }

    private var itemManager: ItemManager?
        get() = root.itemManager
        set(value) {
            root.itemManager = value
            root.insertItemsToAdapter()
        }

    fun init(itemManager: ItemManager) {
        this.itemManager = itemManager
    }

    override fun getNumSections() = root.childCount

    override fun getSection(position: Int): Section = root.get<Section>(position)

    override fun addSection(section: Section) {
        root.add(section)
    }

    override fun insertSection(position: Int, section: Section) {
        root.insert(position, section)
    }

    override fun removeSection(position: Int): Section {
        val s = root.get<Section>(position)
        root.remove(position)
        return s
    }

    override fun moveSection(from: Int, to: Int) {
        root.move(from, to)
    }

    var sectionChangeWatcher: SectionChangeWatcher? = null

    override fun notifySectionInserted(section: Section) = sectionChangeWatcher?.notifySectionInserted(section) ?: -1

    override fun notifySectionRemoved(section: Section) {
        sectionChangeWatcher?.notifySectionRemoved(section)
    }

    override fun notifySectionUpdated(section: Section) {
        sectionChangeWatcher?.notifySectionUpdated(section)
    }

    //    override fun notifySectionMoved(section: Section, toParent: Section, toPosition: Int) {
    //        scw?.notifySectionMoved(section, toParent, toPosition)
    //    }

    override fun notifySectionHeaderInserted(section: Section) {
        sectionChangeWatcher?.notifySectionHeaderInserted(section)
    }

    override fun notifySectionHeaderRemoved(section: Section) {
        sectionChangeWatcher?.notifySectionHeaderRemoved(section)
    }

    override fun notifySectionItemsInserted(section: Section, adapterPositionStart: Int, itemCount: Int) {
        sectionChangeWatcher?.notifySectionItemsInserted(section, adapterPositionStart, itemCount)
    }

    override fun notifySectionItemsRemoved(section: Section, adapterPositionStart: Int, itemCount: Int) {
        sectionChangeWatcher?.notifySectionItemsRemoved(section, adapterPositionStart, itemCount)
    }

    override fun notifySectionItemsMoved(fromSection: Section, fromAdapterPosition: Int, toSection: Section, toAdapterPosition: Int) {
        sectionChangeWatcher?.notifySectionItemsMoved(fromSection, fromAdapterPosition, toSection, toAdapterPosition)
    }
}
