package com.tonicartos.superslim.adapter

import android.support.v7.widget.RecyclerView
import com.tonicartos.superslim.BuildConfig
import java.util.*

interface SectionManager<ID : Comparable<ID>> {
    fun createSection(id: ID): Section
    fun createSection(id: ID, header: Item? = null, config: Section.Config? = null): Section
    fun registerSection(id: ID, section: Section, cascade: Boolean = true)
    fun deregister(id: ID): Section?
    fun deregister(id: ID, stripIds: Boolean = true, cascade: Boolean = true): Section?
    fun getSectionWithId(id: ID): Section?
}

interface SectionGraph {
    fun getNumSections(): Int
    fun getSection(position: Int): Section
    fun addSection(section: Section)
    fun insertSection(position: Int, section: Section)
    fun removeSection(position: Int): Section
    fun moveSection(from: Int, to: Int)
}

abstract class SectionGraphAdapter<ID : Comparable<ID>, VH : RecyclerView.ViewHolder> private constructor(private val manager: SectionManager<ID>, private val graph: SectionGraphImpl) :
        RecyclerView.Adapter<VH>(), SectionManager<ID> by manager, SectionGraph by graph {

    private val itemManager = ItemManager(this)

    init {
        graph.itemManager = itemManager
    }

    constructor() : this(SectionManagerImpl<ID>(), SectionGraphImpl())

    override fun getItemCount() = itemManager.itemCount
    override fun getItemViewType(position: Int) = itemManager[position].type

    abstract fun onBindViewHolder(holder: VH, item: Item)
    override fun onBindViewHolder(holder: VH, position: Int) {
        onBindViewHolder(holder, itemManager[position])
    }
}

internal class Registration<ID : Comparable<ID>>(val id: ID, val manager: SectionManager<ID>) {
    @JvmOverloads fun deregister(stripIds: Boolean = true, cascade: Boolean = true) {
        manager.deregister(id, stripIds, cascade)
    }
}

internal class ItemManager(private val adapter: SectionGraphAdapter<*, *>) {
    val items = ArrayList<Item>()

    val itemCount: Int
        get() = items.size

    operator fun get(position: Int) = items[position]

    fun insert(position: Int, item: Item) {
        items.add(position, item)
        if (BuildConfig.DEBUG) return
        adapter.notifyItemInserted(position)
    }

    fun insert(start: Int, items: List<Item>) {
        this.items.addAll(start, items)
        if (BuildConfig.DEBUG) return
        adapter.notifyItemRangeInserted(start, items.size)
    }

    fun move(from: Int, to: Int) {
        items.add(to, items.removeAt(from))
        if (BuildConfig.DEBUG) return
        adapter.notifyItemMoved(from, to)
    }

    fun remove(position: Int) {
        items.removeAt(position)
        if (BuildConfig.DEBUG) return
        adapter.notifyItemRemoved(position)
    }

    fun removeRange(start: Int, range: Int) {
        for (i in start until start + range) {
            items.removeAt(start)
        }
        if (BuildConfig.DEBUG) return
        adapter.notifyItemRangeRemoved(start, range)
    }

    operator fun set(position: Int, value: Item) {
        items[position] = value
        if (BuildConfig.DEBUG) return
        adapter.notifyItemChanged(position)
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

    /**
     * Deregister section with [id].
     *
     * @param[id] Id of section to deregister.
     * @param[stripIds] Strip id from section. Behaviour propagates with cascade. Defaults to true.
     * @param[cascade] Deregister descendant sections. Defaults to true.
     */
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

private class SectionGraphImpl() : SectionGraph {
    val root = Section()

    internal var itemManager: ItemManager?
        get() = root.itemManager
        set(value) {
            root.itemManager = value
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
}
