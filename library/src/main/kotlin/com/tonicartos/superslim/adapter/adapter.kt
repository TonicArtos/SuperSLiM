package com.tonicartos.superslim.adapter

import android.support.v7.widget.RecyclerView
import com.tonicartos.superslim.LayoutManager
import com.tonicartos.superslim.slm.LinearSectionConfig
import java.util.*

interface SectionGraph {
    fun init(itemManager: ItemManager)
    fun getNumSections(): Int
    fun getSection(position: Int): Section
    fun addSection(section: Section)
    fun insertSection(position: Int, section: Section)
    fun removeSection(position: Int): Section
    fun moveSection(from: Int, to: Int)
}

interface ItemManager {
    val itemCount: Int
    fun init(adapter: SectionGraphAdapter<*, *>)
    fun insert(position: Int, item: Item)
    fun insert(start: Int, items: List<Item>)
    fun move(from: Int, to: Int)
    fun remove(position: Int)
    fun removeRange(start: Int, range: Int)
    operator fun get(position: Int): Item
    operator fun set(position: Int, value: Item)
}

interface SectionManager<ID : Comparable<ID>> {
    fun createSection(id: ID): Section
    fun createSection(id: ID, header: Item? = null, config: Section.Config? = null): Section
    fun registerSection(id: ID, section: Section, cascade: Boolean = true)
    /**
     * Deregister section with [id], stripping it, and repeat the same for all descendant sections.
     */
    fun deregister(id: ID): Section?

    /**
     * Deregister section with [id].
     *
     * @param[id] Id of section to deregister.
     * @param[stripIds] Strip id from section. Behaviour propagates with cascade. Defaults to true.
     * @param[cascade] Deregister descendant sections. Defaults to true.
     */
    fun deregister(id: ID, stripIds: Boolean = true, cascade: Boolean = true): Section?

    fun getSectionWithId(id: ID): Section?
}

interface SectionChangeWatcher {
    fun notifySectionInserted(section: Section)
    fun notifySectionRemoved(section: Section)
    fun notifySectionMoved(section: Section, toParent: Section, toPosition: Int)

    fun notifySectionHeaderInserted(section: Section)
    fun notifySectionHeaderRemoved(section: Section)

    fun notifySectionItemsAdded(section: Section, positionStart: Int, itemCount: Int)
    fun notifySectionItemsRemoved(section: Section, positionStart: Int, itemCount: Int)
    fun notifySectionItemsMoved(fromSection: Section, from: Int, toSection: Section, to: Int)
}

abstract class SectionGraphAdapter<ID : Comparable<ID>, VH : RecyclerView.ViewHolder>
@JvmOverloads constructor(val graph: SectionGraph = SectionGraphImpl(),
                          private val itemManager: ItemManager = ItemManagerImpl(),
                          manager: SectionManager<ID> = SectionManagerImpl()) : RecyclerView.Adapter<VH>(),
        SectionManager<ID> by manager, SectionGraph by graph, SectionChangeWatcher {

    init {
        graph.init(itemManager)
        itemManager.init(this)
    }

    override fun getItemCount() = itemManager.itemCount
    override fun getItemViewType(position: Int) = itemManager[position].type

    abstract fun onBindViewHolder(holder: VH, item: Item)
    override fun onBindViewHolder(holder: VH, position: Int) {
        onBindViewHolder(holder, itemManager[position])
    }

    internal var layoutManager: LayoutManager? = null

    override fun notifySectionInserted(section: Section) {
        val parent = section.parent!!
        layoutManager!!.notifySectionAdded(parent.id, section.positionInAdapter, section.configuration)
    }

    override fun notifySectionRemoved(section: Section) {
        val parent = section.parent!!
        layoutManager!!.notifySectionRemoved(section.id, parent.id, section.positionInParent)
    }

    override fun notifySectionMoved(section: Section, toParent: Section, toPosition: Int) {
        val parent = section.parent!!
        layoutManager!!.notifySectionMoved(section.id, parent.id, section.positionInParent, toParent.id, toPosition)
    }
}

internal class ItemManagerImpl : ItemManager {
    override fun init(adapter: SectionGraphAdapter<*, *>) {
        this.adapter = adapter
    }

    var adapter: SectionGraphAdapter<*, *>? = null

    private val items = ArrayList<Item>()

    override val itemCount: Int
        get() = items.size

    override operator fun get(position: Int) = items[position]

    override fun insert(position: Int, item: Item) {
        items.add(position, item)
        if (BuildConfig.DEBUG) return
        adapter?.notifyItemInserted(position)
    }

    override fun insert(start: Int, items: List<Item>) {
        this.items.addAll(start, items)
        if (BuildConfig.DEBUG) return
        adapter?.notifyItemRangeInserted(start, items.size)
    }

    override fun move(from: Int, to: Int) {
        items.add(to, items.removeAt(from))
        if (BuildConfig.DEBUG) return
        adapter?.notifyItemMoved(from, to)
    }

    override fun remove(position: Int) {
        items.removeAt(position)
        if (BuildConfig.DEBUG) return
        adapter?.notifyItemRemoved(position)
    }

    override fun removeRange(start: Int, range: Int) {
        for (i in start until start + range) {
            items.removeAt(start)
        }
        if (BuildConfig.DEBUG) return
        adapter?.notifyItemRangeRemoved(start, range)
    }

    override operator fun set(position: Int, value: Item) {
        items[position] = value
        if (BuildConfig.DEBUG) return
        adapter?.notifyItemChanged(position)
    }
}

internal class Registration<ID : Comparable<ID>>(val id: ID, val manager: SectionManager<ID>) {
    @JvmOverloads fun deregister(stripIds: Boolean = true, cascade: Boolean = true) {
        manager.deregister(id, stripIds, cascade)
    }
}

private class SectionManagerImpl<ID : Comparable<ID>> : SectionManager<ID> {
    private val sectionLookup = HashMap<ID, Section>()

    override fun getSectionWithId(id: ID): Section? = sectionLookup[id]

    override fun createSection(id: ID) = createSection(id, null, null)
    override fun createSection(id: ID, header: Item?, config: Section.Config?): Section {
        val section = Section()
        section.header = header
        section.configuration = config ?: LinearSectionConfig()
        section.configuration.hasHeader = header != null
        registerSection(id, section)
        return section
    }

    /**
     * Register a [section] to [id]. Descendant sections will be registered if [cascade] is true.
     */
    override fun registerSection(id: ID, section: Section, cascade: Boolean) {
        sectionLookup[id]?.let { if (it !== section) deregister(id) }

        sectionLookup[id] = section
        section.registration = Registration(id, this)
        if (cascade) {
            // If there are any descendants
            section.getSubsections().forEach { subsection ->
                @Suppress("UNCHECKED_CAST")
                var attemptedCast = (subsection.registration as? Registration<ID>)?.let { oldReg ->
                    registerSection(oldReg.id, subsection)
                }
                if (attemptedCast == null) {
                    // Can't understand registration, and since it can't be used, ditch it.
                    subsection.registration = null
                }
            }
        }
    }

    override fun deregister(id: ID): Section? = deregister(id, true, true)

    override fun deregister(id: ID, stripIds: Boolean, cascade: Boolean): Section? {
        return sectionLookup.remove(id)?.apply {
            if (stripIds) {
                registration = null
            }
            if (cascade) {
                getSubsections().forEach { it.registration?.deregister(stripIds, cascade) }
            }
        }
    }
}

private class SectionGraphImpl() : SectionGraph, SectionChangeWatcher {
    val root = Section(this)

    var itemManager: ItemManager?
        get() = root.itemManager
        set(value) {
            root.itemManager = value
            root.insertItemsToAdapter()
        }

    override fun init(itemManager: ItemManager) {
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

    var scw: SectionChangeWatcher? = null

    override fun notifySectionInserted(section: Section) {
        scw!!.notifySectionInserted(section)
    }

    override fun notifySectionRemoved(section: Section) {
        scw!!.notifySectionRemoved(section)
    }

    override fun notifySectionMoved(section: Section, toParent: Section, toPosition: Int) {
        scw!!.notifySectionMoved(section, toParent, toPosition)
    }
}
