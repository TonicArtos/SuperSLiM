package com.tonicartos.superslim

import android.util.SparseArray
import com.tonicartos.superslim.adapter.Section

internal class GraphManager(private val root: SectionState) {
    /*************************
     * Layout
     *************************/

    fun layout(helper: LayoutHelper) {
        if (helper.isPreLayout) {
            initPreLayout()
        } else {
            initPostLayout()
        }

        root.layout(helper, 0, 0, helper.layoutWidth)

        if (helper.isPreLayout) {
            cleanupPreLayout()
        } else {
            cleanupPostLayout()
        }
    }

    fun initPreLayout() {
    }

    fun cleanupPreLayout() {
    }

    fun initPostLayout() {
        doSectionMoves()
        doSectionUpdates()
    }

    fun cleanupPostLayout() {
        doSectionRemovals()
    }

    /*************************
     * Scheduling section changes
     *************************/
    private data class ScheduledSectionRemoval(val section: Int, val parent: Int, val position: Int)

    private data class ScheduledSectionMove(val section: Int, val fromParent: Int, val fromPosition: Int, val toParent: Int, val toPosition: Int)
    private data class ScheduledSectionUpdate(val section: Int, val config: Section.Config)

    private val sectionsToRemove = arrayListOf<ScheduledSectionRemoval>()
    private val sectionsToMove = arrayListOf<ScheduledSectionMove>()
    private val sectionsToUpdate = arrayListOf<ScheduledSectionUpdate>()

    fun sectionAdded(parent: Int, position: Int, config: Section.Config): Int {
        val newSection = config.makeSection()
        getSection(parent).insert(position, newSection)
        return indexSection(newSection)
    }

    fun queueSectionRemoved(section: Int, parent: Int, position: Int) {
        sectionsToRemove.add(ScheduledSectionRemoval(section, parent, position))
    }

    fun queueSectionMoved(section: Int, fromParent: Int, fromPosition: Int, toParent: Int, toPosition: Int) {
        sectionsToMove.add(ScheduledSectionMove(section, fromParent, fromPosition, toParent, toPosition))
    }

    fun queueSectionUpdated(section: Int, config: Section.Config) {
        sectionsToUpdate.add(ScheduledSectionUpdate(section, config))
    }

    /*************************
     * Performing scheduled section changes
     *************************/

    private fun doSectionRemovals() {
        for (remove in sectionsToRemove) {
            getSection(remove.parent).remove(remove.position)
            deIndexSection(remove.section)
        }
        sectionsToRemove.clear()
    }

    private fun doSectionMoves() {
        for (move in sectionsToMove) {
            getSection(move.fromParent).remove(move.fromPosition)
            getSection(move.toParent).add(move.toPosition, getSection(move.section))
        }
        sectionsToMove.clear()
    }

    private fun doSectionUpdates() {
        for (update in sectionsToUpdate) {
            replaceSection(update.section, update.config.makeSection(getSection(update.section)))
        }
    }

    /*************************
     * Section management
     *************************/

    private var numSectionsSeen = 0
    private val sectionIndex = SparseArray<SectionState>()

    private fun indexSection(section: SectionState): Int {
        val id = numSectionsSeen
        numSectionsSeen += 1
        sectionIndex.put(id, section)
        return id
    }

    private fun deIndexSection(section: Int) {
        sectionIndex.remove(section)
    }

    private fun getSection(id: Int) = sectionIndex[id]
    private fun replaceSection(id: Int, newSection: SectionState) {
        sectionIndex.put(id, newSection)
    }

    /*************************
     * Item events
     *************************/
    fun addItems(eventData: EventData, positionStart: Int, itemCount: Int) {
        val section = getSection(eventData.section)
        if (eventData.action and EventData.HEADER > 0) {
            section.baseConfig.hasHeader = true
        }
        section.addItems(positionStart, itemCount)
    }

    fun removeItems(eventData: EventData, positionStart: Int, itemCount: Int) {
        val section = getSection(eventData.section)
        if (eventData.action and EventData.HEADER > 0) {
            section.baseConfig.hasHeader = false
        }
        section.removeItems(positionStart, itemCount)
    }

    fun moveItems(fromSection: Int, from: Int, toSection: Int, to: Int) {
        getSection(fromSection).removeItems(from, 1)
        getSection(toSection).addItems(to, 1)
    }
}
