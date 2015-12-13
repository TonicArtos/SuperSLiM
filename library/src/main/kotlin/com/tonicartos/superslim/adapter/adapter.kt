package com.tonicartos.superslim.adapter

import android.support.v7.widget.RecyclerView
import com.tonicartos.superslim.AdapterContract
import com.tonicartos.superslim.BuildConfig
import com.tonicartos.superslim.SuperSlimLayoutManager
import com.tonicartos.superslim.internal.AdapterContract
import com.tonicartos.superslim.internal.layout.LinearSectionConfig
import java.util.*

interface SectionGraph {
    fun getNumSections(): Int
    fun getSection(position: Int): Section
    fun addSection(section: Section)
    fun insertSection(position: Int, section: Section)
    fun removeSection(position: Int): Section
    fun moveSection(from: Int, to: Int)
}

interface WatchableSectionGraph: SectionGraph {
    fun init(itemManager: ItemManager)
    var sectionChangeWatcher: SectionChangeWatcher?
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
    fun notifySectionInserted(section: Section): Int
    fun notifySectionRemoved(section: Section)
    //    fun notifySectionMoved(section: Section, toParent: Section, toPosition: Int)
    fun notifySectionUpdated(section: Section)

    fun notifySectionHeaderInserted(section: Section)
    fun notifySectionHeaderRemoved(section: Section)

    fun notifySectionItemsInserted(section: Section, adapterPositionStart: Int, itemCount: Int)
    fun notifySectionItemsRemoved(section: Section, adapterPositionStart: Int, itemCount: Int)
    fun notifySectionItemsMoved(fromSection: Section, fromAdapterPosition: Int, toSection: Section, toAdapterPosition: Int)
}

abstract class SectionGraphAdapter<ID : Comparable<ID>, VH : RecyclerView.ViewHolder>
@JvmOverloads constructor(private val graph: WatchableSectionGraph = DefaultSectionGraph(),
            private val itemManager: ItemManager = DefaultItemManager(),
            manager: SectionManager<ID> = DefaultSectionManager()) : RecyclerView.Adapter<VH>(), AdapterContract,
        SectionManager<ID> by manager, SectionGraph by graph {

    init {
        graph.init(itemManager)
        itemManager.init(this)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        if (graph.sectionChangeWatcher != null) throw OnlySupportsOneRecyclerViewException()
        val layoutManager = recyclerView.layoutManager as? SuperSlimLayoutManager ?: return
        graph.sectionChangeWatcher = DataChangeBridge(layoutManager)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView?) {
        graph.sectionChangeWatcher = null
    }

    abstract fun onBindViewHolder(holder: VH, item: Item)
    final override fun onBindViewHolder(holder: VH, position: Int) {
        onBindViewHolder(holder, itemManager[position])
    }

    final override fun getItemCount() = itemManager.itemCount
    final override fun getItemViewType(position: Int) = itemManager[position].type

    inner private class AdapterContractc: AdapterContract {
        override fun getSections(): List<Section.Config> {
            // Todo:
        }

        override fun setSectionIds(map: List<Int>) {
            // Todo:
        }

        override fun populateSection(sectionId: Int, sectionData: AdapterContract.SectionData) {
            // Todo:
        }
    }

    inner private class DataChangeBridge(val layoutManager: SuperSlimLayoutManager): SectionChangeWatcher {
        final override fun notifySectionInserted(section: Section): Int = layoutManager.notifySectionAdded(section.parent!!.id, section.positionInAdapter, section.configuration)

        final override fun notifySectionRemoved(section: Section) {
            layoutManager.notifySectionRemoved(section.id, section.parent!!.id, section.positionInParent)
        }

        //    final override fun notifySectionMoved(section: Section, toParent: Section, toPosition: Int) {
        //        layoutManager.notifySectionMoved(section.id, section.parent!!.id, section.positionInParent, toParent.id, toPosition)
        //    }

        final override fun notifySectionUpdated(section: Section) {
            layoutManager.notifySectionUpdated(section.id, section.configuration)
            if (section.totalItems > 0) {
                notifyItemRangeChanged(section.positionInAdapter, section.totalItems)
            }
        }

        final override fun notifySectionHeaderInserted(section: Section) {
            layoutManager.notifySectionHeaderAdded(section.id, section.positionInAdapter)
        }

        final override fun notifySectionHeaderRemoved(section: Section) {
            layoutManager.notifySectionHeaderRemoved(section.id, section.positionInAdapter)
        }

        final override fun notifySectionItemsInserted(section: Section, adapterPositionStart: Int, itemCount: Int) {
            layoutManager.notifySectionItemsAdded(section.id, adapterPositionStart, itemCount)
        }

        final override fun notifySectionItemsRemoved(section: Section, adapterPositionStart: Int, itemCount: Int) {
            layoutManager.notifySectionItemsRemoved(section.id, adapterPositionStart, itemCount)
        }

        final override fun notifySectionItemsMoved(fromSection: Section, fromAdapterPosition: Int, toSection: Section, toAdapterPosition: Int) {
            layoutManager.notifySectionItemsMoved(fromSection.parent!!.id, fromAdapterPosition, toSection.parent!!.id, toAdapterPosition)
        }
    }
}

class DefaultItemManager : ItemManager {
    override fun init(adapter: SectionGraphAdapter<*, *>) {
        this.adapter = adapter
    }

    private var adapter: SectionGraphAdapter<*, *>? = null

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

class DefaultSectionManager<ID : Comparable<ID>> : SectionManager<ID> {
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
                var attemptedCast = (subsection.registration as? Registration<ID>)?.apply {
                    registerSection(this.id, subsection)
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

class DefaultSectionGraph() : WatchableSectionGraph, SectionChangeWatcher {
    private val root: Section

    init {
        root = Section(this)
    }

    private var itemManager: ItemManager?
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

    private var _scw: SectionChangeWatcher? = null
    override var sectionChangeWatcher: SectionChangeWatcher?
        get() = _scw
        set(value) {
            _scw = value
            root.id = value?.notifySectionInserted(root) ?: -1
        }

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

class OnlySupportsOneRecyclerViewException : Throwable("SuperSLiM currently only supports a single recycler view per adapter.")
