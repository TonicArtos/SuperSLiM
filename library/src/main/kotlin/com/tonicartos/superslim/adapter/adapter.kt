package com.tonicartos.superslim.adapter

import android.support.annotation.IntDef
import android.support.v7.widget.RecyclerView
import com.tonicartos.superslim.*
import com.tonicartos.superslim.layout.LinearSectionConfig
import java.util.*

interface Graph {
    fun getNumSections(): Int
    fun getSection(position: Int): Section
    fun addSection(section: Section)
    fun insertSection(position: Int, section: Section)
    fun removeSection(position: Int): Section
    fun moveSection(from: Int, to: Int)
}

abstract class SuperSlimAdapter<ID : Comparable<ID>, VH : RecyclerView.ViewHolder>
private constructor(internal val graph: GraphImpl, internal val itemManager: ItemManager,
                    private val adapterContract: AdapterContractImpl<ID>) : RecyclerView.Adapter<VH>(),
                                                                            AdapterContract<ID> by adapterContract,
                                                                            Graph by graph {
    init {
        graph.init(itemManager)
        itemManager.init(this)
        adapterContract.init(this)
    }

    constructor() : this(GraphImpl(), ItemManager(), AdapterContractImpl())

    abstract fun onBindViewHolder(holder: VH, item: Item)
    final override fun onBindViewHolder(holder: VH, position: Int) {
        onBindViewHolder(holder, itemManager[position])
    }

    final override fun getItemCount() = itemManager.itemCount
    final override fun getItemViewType(position: Int) = itemManager[position].type

    /****************************************************
     * Section registry
     ****************************************************/

    internal val sectionLookup = HashMap<ID, Section>()

    fun getSectionWithId(id: ID): Section? = sectionLookup[id]

    @JvmOverloads
    fun createSection(id: ID, config: SectionConfig, header: Item? = null, footer: Item? = null): Section {
        val section = Section()
        section.header = header
        section.footer = footer
        section.configuration = config
        registerSection(id, section)
        return section
    }

    @JvmOverloads
    fun createSection(id: ID, header: Item? = null, footer: Item? = null): Section
            = createSection(id, LinearSectionConfig(), header, footer)

    /**
     * Register a [section] to [id]. Descendant sections will be registered if [cascade] is true.
     */
    @JvmOverloads fun registerSection(id: ID, section: Section, cascade: Boolean = true) {
        sectionLookup[id]?.let { if (it !== section) deregister(id) }

        sectionLookup[id] = section
        section.registration = Registration(id, this)
        if (cascade) {
            // If there are any descendants
            section.subsections.forEach { subsection ->
                @Suppress("UNCHECKED_CAST")
                val reusableRegistration = subsection.registration as? Registration<ID>
                if (reusableRegistration == null) {
                    // Can't understand registration, and since it can't be used, ditch it.
                    subsection.registration = null
                } else {
                    registerSection(reusableRegistration.id, subsection, cascade)
                }
            }
        }
    }

    @JvmOverloads fun deregister(id: ID, stripIds: Boolean = true, cascade: Boolean = true): Section? {
        return sectionLookup.remove(id)?.apply {
            if (stripIds) {
                registration = null
            }
            if (cascade) {
                subsections.forEach { it.registration?.deregister(stripIds, cascade) }
            }
        }
    }
}

private class AdapterContractImpl<ID : Comparable<ID>> : AdapterContract<ID> {
    private lateinit var adapter: SuperSlimAdapter<ID, *>

    fun init(adapter: SuperSlimAdapter<ID, *>) {
        this.adapter = adapter
    }

    override fun onLayoutManagerAttached(layoutManager: SuperSlimLayoutManager) {
        if (adapter.graph.contract != null) throw OnlySupportsOneRecyclerViewException()
        val contract = DataChangeContract(adapter, layoutManager)
        adapter.graph.contract = contract
    }

    override fun onLayoutManagerDetached(layoutManager: SuperSlimLayoutManager) {
        adapter.graph.contract = null
    }


    override fun getRoot() = adapter.graph.root.configuration

    override fun setRootId(id: Int) {
        adapter.graph.root.id = id
    }

    override fun populateRoot(out: SectionData) {
        out.adapterPosition = 0
        out.hasHeader = false
        out.itemCount = adapter.graph.root.itemCount
        out.childCount = adapter.graph.root.childCount
        out.subsectionsById = adapter.graph.root.subsections.map { it.id }
    }

    override fun getSections() = adapter.sectionLookup.mapValues { it.value.configuration }

    override fun setSectionIds(idMap: Map<*, Int>) {
        idMap.forEach {
            val section = adapter.sectionLookup[it.key] ?: throw IllegalArgumentException(
                    "unknown id \"${it.key}\" for section lookup")
            section.id = it.value
        }
    }

    override fun populateSection(data: Pair<*, SectionData>) {
        val section = adapter.sectionLookup[data.first] ?: throw IllegalArgumentException(
                "unknown id \"${data.first}\" for section lookup")
        data.second.adapterPosition = section.positionInAdapter
        data.second.hasHeader = section.header != null
        data.second.itemCount = section.itemCount
        data.second.childCount = section.childCount
        data.second.subsectionsById = section.subsections.map { it.id }
    }

    override fun getData(position: Int): AdapterContract.Data {
        val item = adapter.itemManager[position]
        val parent = item.parent!!
        return AdapterContract.data.pack(parent.id, item.positionInParent, when (item) {
            parent.header -> AdapterContract.Data.HEADER
            parent.footer -> AdapterContract.Data.FOOTER
            else          -> AdapterContract.Data.OTHER
        })
    }
}


internal interface SectionContract {
    fun notifySectionInserted(section: Section): Int
    fun notifySectionRemoved(section: Section)
    fun notifySectionUpdated(section: Section)
}

private class DataChangeContract(val adapter: SuperSlimAdapter<*, *>,
                                 val layoutManager: SuperSlimLayoutManager) : SectionContract {
    override fun notifySectionInserted(
            section: Section): Int = layoutManager.notifySectionAdded(section.parent!!.id, section.positionInParent,
                                                                      section.configuration)

    override fun notifySectionRemoved(section: Section) {
        layoutManager.notifySectionRemoved(section.id, section.parent!!.id)
    }

    //final override fun notifySectionMoved(section: Section, toParent: Section, toPosition: Int) {
    //    layoutManager.notifySectionMoved(section.id, section.parent!!.id, section.positionInParent, toParent.id, toPosition)
    //}

    override fun notifySectionUpdated(section: Section) {
        layoutManager.notifySectionUpdated(section.id, section.configuration)
        if (section.itemCount > 0) {
            adapter.notifyItemRangeChanged(section.positionInAdapter, section.itemCount)
        }
    }
}

internal class ItemManager {
    fun init(adapter: SuperSlimAdapter<*, *>) {
        this.adapter = adapter
    }

    private var adapter: SuperSlimAdapter<*, *>? = null

    private val items = ArrayList<Item>()

    val itemCount: Int
        get() = items.size

    operator fun get(position: Int) = items[position]

    fun insert(position: Int, item: Item) {
        items.add(position, item)
        if (BuildConfig.UNIT_TEST) return
        adapter?.notifyItemInserted(position)
    }

    fun insert(start: Int, items: List<Item>) {
        this.items.addAll(start, items)
        if (BuildConfig.UNIT_TEST) return
        adapter?.notifyItemRangeInserted(start, items.size)
    }

    fun move(from: Int, to: Int) {
        items.add(to, items.removeAt(from))
        if (BuildConfig.UNIT_TEST) return
        adapter?.notifyItemMoved(from, to)
    }

    fun remove(position: Int) {
        items.removeAt(position)
        if (BuildConfig.UNIT_TEST) return
        adapter?.notifyItemRemoved(position)
    }

    fun removeRange(start: Int, range: Int) {
        for (i in start until start + range) {
            items.removeAt(start)
        }
        if (BuildConfig.UNIT_TEST) return
        adapter?.notifyItemRangeRemoved(start, range)
    }

    operator fun set(position: Int, value: Item) {
        items[position] = value
        if (BuildConfig.UNIT_TEST) return
        adapter?.notifyItemChanged(position)
    }
}

internal class Registration<ID : Comparable<ID>>(val id: ID, val manager: SuperSlimAdapter<ID, *>) {
    @JvmOverloads fun deregister(stripIds: Boolean = true, cascade: Boolean = true) {
        manager.deregister(id, stripIds, cascade)
    }
}

class OnlySupportsOneRecyclerViewException : RuntimeException(
        "SuperSLiM currently only supports a single recycler view per adapter.")
