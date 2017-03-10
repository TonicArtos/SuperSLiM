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
private constructor(private val graph: GraphImpl, private val itemManager: ItemManager) : RecyclerView.Adapter<VH>(),
                                                                                          AdapterContract<ID>,
                                                                                          Graph by graph {
    init {
        graph.init(itemManager)
        itemManager.init(this)
    }

    constructor() : this(GraphImpl(), ItemManager())

    abstract fun onBindViewHolder(holder: VH, item: Item)
    final override fun onBindViewHolder(holder: VH, position: Int) {
        onBindViewHolder(holder, itemManager[position])
    }

    final override fun getItemCount() = itemManager.itemCount
    final override fun getItemViewType(position: Int) = itemManager[position].type

    /****************************************************
     * Adapter contract
     ****************************************************/

    override fun onLayoutManagerAttached(layoutManager: SuperSlimLayoutManager) {
        if (graph.contract != null) throw OnlySupportsOneRecyclerViewException()
        val contract = DataChangeContract(this, layoutManager)
        graph.contract = contract
    }

    override fun onLayoutManagerDetached(layoutManager: SuperSlimLayoutManager) {
        graph.contract = null
    }


    override fun getRoot() = graph.root.configuration

    override fun setRootId(id: Int) {
        graph.root.id = id
    }

    override fun populateRoot(out: SectionData) {
        out.adapterPosition = 0
        out.hasHeader = false
        out.itemCount = graph.root.itemCount
        out.childCount = graph.root.childCount
        out.subsectionsById = graph.root.subsections.map { it.id }
    }


    override fun getSections() = sectionLookup.mapValues { it.value.configuration }

    override fun setSectionIds(idMap: Map<*, Int>) {
        idMap.forEach {
            val section = sectionLookup[it.key] ?: throw IllegalArgumentException(
                    "unknown id \"${it.key}\" for section lookup")
            section.id = it.value
        }
    }

    override fun populateSection(data: Pair<*, SectionData>) {
        val section = sectionLookup[data.first] ?: throw IllegalArgumentException(
                "unknown id \"${data.first}\" for section lookup")
        data.second.adapterPosition = section.positionInAdapter
        data.second.hasHeader = section.header != null
        data.second.itemCount = section.itemCount
        data.second.childCount = section.childCount
        data.second.subsectionsById = section.subsections.map { it.id }
    }

    /****************************************************
     * Section registry
     ****************************************************/

    private val sectionLookup = HashMap<ID, Section>()

    fun getSectionWithId(id: ID): Section? = sectionLookup[id]

    @JvmOverloads fun createSection(id: ID, header: Item? = null, footer: Item? = null,
                                    config: SectionConfig = LinearSectionConfig()): Section {
        val section = Section()
        config.headerStyle = SectionConfig.HEADER_INLINE
        config.footerStyle = SectionConfig.FOOTER_INLINE
        section.header = header
        section.footer = footer
        section.configuration = config
        registerSection(id, section)
        return section
    }

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
                var reusableRegistration = subsection.registration as? Registration<ID>
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

internal interface SectionContract {
    fun notifySectionInserted(section: Section): Int
    fun notifySectionRemoved(section: Section)
    fun notifySectionUpdated(section: Section)

    fun notifySectionHeaderInserted(section: Section)
    fun notifySectionHeaderRemoved(section: Section)

    fun notifySectionFooterInserted(section: Section, positionStart: Int)
    fun notifySectionFooterRemoved(section: Section, positionStart: Int)

    fun notifySectionItemsInserted(section: Section, positionStart: Int, adapterPositionStart: Int, itemCount: Int)
    fun notifySectionItemsRemoved(section: Section, positionStart: Int, adapterPositionStart: Int, itemCount: Int)
    fun notifySectionItemsMoved(fromSection: Section, fromPosition: Int, fromAdapterPosition: Int, toSection: Section,
                                toPosition: Int, toAdapterPosition: Int)
}

private class DataChangeContract(val adapter: SuperSlimAdapter<*, *>,
                                 val layoutManager: SuperSlimLayoutManager) : SectionContract {
    final override fun notifySectionInserted(
            section: Section): Int = layoutManager.notifySectionAdded(section.parent!!.id, section.positionInParent,
                                                                      section.configuration)

    final override fun notifySectionRemoved(section: Section) {
        layoutManager.notifySectionRemoved(section.id, section.parent!!.id)
    }

    //final override fun notifySectionMoved(section: Section, toParent: Section, toPosition: Int) {
    //    layoutManager.notifySectionMoved(section.id, section.parent!!.id, section.positionInParent, toParent.id, toPosition)
    //}

    final override fun notifySectionUpdated(section: Section) {
        layoutManager.notifySectionUpdated(section.id, section.configuration)
        if (section.itemCount > 0) {
            adapter.notifyItemRangeChanged(section.positionInAdapter, section.itemCount)
        }
    }

    final override fun notifySectionHeaderInserted(section: Section) {
        layoutManager.notifySectionHeaderAdded(section.id, section.positionInAdapter)
    }

    final override fun notifySectionHeaderRemoved(section: Section) {
        layoutManager.notifySectionHeaderRemoved(section.id, section.positionInAdapter)
    }

    final override fun notifySectionFooterInserted(section: Section, positionStart: Int) {
        layoutManager.notifySectionFooterAdded(section.id, positionStart, section.positionInAdapter)
    }

    final override fun notifySectionFooterRemoved(section: Section, positionStart: Int) {
        layoutManager.notifySectionFooterRemoved(section.id, positionStart, section.positionInAdapter)
    }

    final override fun notifySectionItemsInserted(section: Section, positionStart: Int, adapterPositionStart: Int,
                                                  itemCount: Int) {
        layoutManager.notifySectionItemsAdded(section.id, positionStart, adapterPositionStart, itemCount)
    }

    final override fun notifySectionItemsRemoved(section: Section, positionStart: Int, adapterPositionStart: Int,
                                                 itemCount: Int) {
        layoutManager.notifySectionItemsRemoved(section.id, positionStart, adapterPositionStart, itemCount)
    }

    final override fun notifySectionItemsMoved(fromSection: Section, fromPosition: Int, fromAdapterPosition: Int,
                                               toSection: Section, toPosition: Int, toAdapterPosition: Int) {
        layoutManager.notifySectionItemsMoved(fromSection.parent!!.id, fromPosition, fromAdapterPosition,
                                              toSection.parent!!.id, toPosition, toAdapterPosition)
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

@IntDef(SectionConfig.HEADER_END.toLong(),
        SectionConfig.HEADER_STICKY.toLong(),
        SectionConfig.HEADER_INLINE.toLong(),
        SectionConfig.HEADER_START.toLong(),
        SectionConfig.HEADER_END.toLong(),
        SectionConfig.HEADER_FLOAT.toLong(),
        SectionConfig.HEADER_TAIL.toLong())
@Retention(AnnotationRetention.SOURCE)
annotation class HeaderStyle

@IntDef(SectionConfig.FOOTER_END.toLong(),
        SectionConfig.FOOTER_STICKY.toLong(),
        SectionConfig.FOOTER_INLINE.toLong(),
        SectionConfig.FOOTER_START.toLong(),
        SectionConfig.FOOTER_END.toLong(),
        SectionConfig.FOOTER_FLOAT.toLong(),
        SectionConfig.FOOTER_TAIL.toLong())
@Retention(AnnotationRetention.SOURCE)
annotation class FooterStyle
