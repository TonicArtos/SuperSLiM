package com.tonicartos.superslim

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.tonicartos.superslim.adapter.Item
import com.tonicartos.superslim.adapter.SuperSlimAdapter
import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.internal.layout.LinearSectionConfig
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.sameInstance
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 *
 */
class TestLayoutGraph {
    class Adapter : SuperSlimAdapter<Int, RecyclerView.ViewHolder>() {
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: Item) {
            throw UnsupportedOperationException()
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
            throw UnsupportedOperationException()
        }
    }

    class NoNullHashMap : HashMap<Int, SectionState>() {
        override fun get(key: Int): SectionState = super.get(key)!!
    }

    lateinit var root: SectionState
    val section = NoNullHashMap()

    @Before
    fun setupGraph() {
        //          0
        //    /---/ | \-----\
        //   1      2        3
        //  /|\    /|\     / | \
        // 4 5 6  7 8 9  10 11 12
        //       /  |  \
        //     13  14  15

        val config = LinearSectionConfig()
        for (i in 0..15) {
            section[i] = config.copy().testAccess.makeSection()
        }
        root = section[0]
        root.testAccess.insertSection(0, section[1])
        root.testAccess.insertSection(1, section[2])
        root.testAccess.insertSection(2, section[3])

        section[1].testAccess.insertSection(0, section[4])
        section[1].testAccess.insertSection(1, section[5])
        section[1].testAccess.insertSection(2, section[6])

        section[2].testAccess.insertSection(0, section[7])
        section[2].testAccess.insertSection(1, section[8])
        section[2].testAccess.insertSection(2, section[9])

        section[3].testAccess.insertSection(0, section[10])
        section[3].testAccess.insertSection(1, section[11])
        section[3].testAccess.insertSection(2, section[12])

        section[7].testAccess.insertSection(0, section[13])
        section[8].testAccess.insertSection(0, section[14])
        section[9].testAccess.insertSection(0, section[15])
    }

    @Test
    fun insertSections() {
        // Use setup (functions as validation of setup for all other tests).
        assertThat(root.numChildren, equalTo(3))
        (1..3).forEach { assertThat(section[it].numChildren, equalTo(3)) }
        (1..3).forEach { assertThat(section[it].testAccess.subsections.size, equalTo(3)) }

        (7..9).forEach { assertThat(section[it].numChildren, equalTo(1)) }
        (7..9).forEach { assertThat(section[it].testAccess.subsections.size, equalTo(1)) }

        ((4..6) + (10..15)).forEach { assertThat(section[it].numChildren, equalTo(0)) }
        ((4..6) + (10..15)).forEach { assertThat(section[it].testAccess.subsections.size, equalTo(0)) }

        section.values.forEach { assertThat(it.testAccess.totalItems, equalTo(0)) }
        section.values.forEach { assertThat(it.testAccess.adapterPosition, equalTo(0)) }

        // Check section order is correct.
        assertThat(root.testAccess.subsections[0], sameInstance(section[1]))
        assertThat(root.testAccess.subsections[1], sameInstance(section[2]))
        assertThat(root.testAccess.subsections[2], sameInstance(section[3]))

        assertThat(section[1].testAccess.subsections[0], sameInstance(section[4]))
        assertThat(section[1].testAccess.subsections[1], sameInstance(section[5]))
        assertThat(section[1].testAccess.subsections[2], sameInstance(section[6]))

        assertThat(section[2].testAccess.subsections[0], sameInstance(section[7]))
        assertThat(section[2].testAccess.subsections[1], sameInstance(section[8]))
        assertThat(section[2].testAccess.subsections[2], sameInstance(section[9]))

        assertThat(section[3].testAccess.subsections[0], sameInstance(section[10]))
        assertThat(section[3].testAccess.subsections[1], sameInstance(section[11]))
        assertThat(section[3].testAccess.subsections[2], sameInstance(section[12]))

        assertThat(section[7].testAccess.subsections[0], sameInstance(section[13]))
        assertThat(section[8].testAccess.subsections[0], sameInstance(section[14]))
        assertThat(section[9].testAccess.subsections[0], sameInstance(section[15]))
    }

    @Test
    fun addAndRemoveHeader() {
        val parent = section[8]
        parent.testAccess.addHeader()
        assertThat(parent.numChildren, equalTo(1))
        assertThat(parent.testAccess.totalItems, equalTo(1))

        parent.testAccess.removeHeader()
        assertThat(parent.numChildren, equalTo(1))
        assertThat(parent.testAccess.totalItems, equalTo(0))
    }

    @Test
    fun headerChangesPropagateAcrossGraph() {
        val parent = section[8]
        parent.testAccess.addHeader()

        // Unchanged
        listOf(1, 4, 5, 6, 7, 13).forEach {
            assertThat(section[it].testAccess.adapterPosition, equalTo(0))
            assertThat(section[it].testAccess.totalItems, equalTo(0))
        }
        // Total items adjusted
        listOf(0, 2, 8).forEach {
            assertThat(section[it].testAccess.adapterPosition, equalTo(0))
            assertThat(section[it].testAccess.totalItems, equalTo(1))
        }

        // Adapter position adjusted
        listOf(3, 9, 10, 11, 12, 14, 15).forEach {
            assertThat(section[it].testAccess.adapterPosition, equalTo(1))
            assertThat(section[it].testAccess.totalItems, equalTo(0))
        }

        parent.testAccess.removeHeader()
        // Unchanged
        listOf(1, 4, 5, 6, 7, 13).forEach {
            assertThat(section[it].testAccess.adapterPosition, equalTo(0))
            assertThat(section[it].testAccess.totalItems, equalTo(0))
        }
        // Total items adjusted
        listOf(0, 2).forEach {
            assertThat(section[it].testAccess.adapterPosition, equalTo(0))
            assertThat(section[it].testAccess.totalItems, equalTo(0))
        }

        // Adapter position adjusted
        listOf(3, 9, 10, 11, 12, 14, 15).forEach {
            assertThat(section[it].testAccess.adapterPosition, equalTo(0))
            assertThat(section[it].testAccess.totalItems, equalTo(0))
        }
    }

    @Test
    fun addAndRemoveSectionBeforeSubsection() {
        val parent = section[8]
        val newSection = LinearSectionConfig().testAccess.makeSection()
        parent.testAccess.insertSection(0, newSection)
        assertThat(parent.numChildren, equalTo(2))
        assertThat(parent.testAccess.adapterPosition, equalTo(0))
        assertThat(parent.testAccess.totalItems, equalTo(0))
        assertThat(parent.testAccess.subsections.size, equalTo(2))
        assertThat(parent.testAccess.subsections[0], sameInstance(newSection))
        assertThat(parent.testAccess.subsections[1], sameInstance(section[14]))

        parent.testAccess.removeSection(newSection)
        assertThat(parent.numChildren, equalTo(1))
        assertThat(parent.testAccess.adapterPosition, equalTo(0))
        assertThat(parent.testAccess.totalItems, equalTo(0))
        assertThat(parent.testAccess.subsections.size, equalTo(1))
        assertThat(parent.testAccess.subsections[0], sameInstance(section[14]))
    }

    @Test
    fun addAndRemoveSectionAfterSubsection() {
        val parent = section[8]
        val newSection = LinearSectionConfig().testAccess.makeSection()

        parent.testAccess.insertSection(1, newSection)
        assertThat(parent.numChildren, equalTo(2))
        assertThat(parent.testAccess.adapterPosition, equalTo(0))
        assertThat(parent.testAccess.totalItems, equalTo(0))
        assertThat(parent.testAccess.subsections.size, equalTo(2))
        assertThat(parent.testAccess.subsections[0], sameInstance(section[14]))
        assertThat(parent.testAccess.subsections[1], sameInstance(newSection))

        parent.testAccess.removeSection(newSection)
        assertThat(parent.numChildren, equalTo(1))
        assertThat(parent.testAccess.adapterPosition, equalTo(0))
        assertThat(parent.testAccess.totalItems, equalTo(0))
        assertThat(parent.testAccess.subsections.size, equalTo(1))
        assertThat(parent.testAccess.subsections[0], sameInstance(section[14]))
    }

    @Test
    fun addAndRemovePopulatedSection() {
        val populatedSection = LinearSectionConfig().testAccess.makeSection()
        populatedSection.testAccess.addHeader()
        populatedSection.testAccess.addItems(0, 1, 10)

        val parent = section[2]
        parent.testAccess.insertSection(1, populatedSection)

        assertThat(parent.numChildren, equalTo(4))
        assertThat(parent.testAccess.adapterPosition, equalTo(0))
        assertThat(parent.testAccess.totalItems, equalTo(11))
        assertThat(parent.testAccess.subsections.size, equalTo(4))
        assertThat(parent.testAccess.subsections[0], sameInstance(section[7]))
        assertThat(parent.testAccess.subsections[1], sameInstance(populatedSection))
        assertThat(parent.testAccess.subsections[2], sameInstance(section[8]))
        assertThat(parent.testAccess.subsections[3], sameInstance(section[9]))

        //Check rest of graph
        listOf(1, 4, 5, 6, 7, 13).forEach {
            assertThat(section[it].testAccess.adapterPosition, equalTo(0))
            assertThat(section[it].testAccess.totalItems, equalTo(0))
        }
        assertThat(root.testAccess.adapterPosition, equalTo(0))
        assertThat(root.testAccess.totalItems, equalTo(11))
        listOf(3, 8, 9, 10, 11, 12, 14, 15).forEach {
            assertThat(section[it].testAccess.adapterPosition, equalTo(11))
            assertThat(section[it].testAccess.totalItems, equalTo(0))
        }

        parent.testAccess.removeSection(populatedSection)
        assertThat(parent.numChildren, equalTo(3))
        assertThat(parent.testAccess.adapterPosition, equalTo(0))
        assertThat(parent.testAccess.totalItems, equalTo(0))
        assertThat(parent.testAccess.subsections.size, equalTo(3))
        assertThat(parent.testAccess.subsections[0], sameInstance(section[7]))
        assertThat(parent.testAccess.subsections[1], sameInstance(section[8]))
        assertThat(parent.testAccess.subsections[2], sameInstance(section[9]))

        //Check rest of graph
        listOf(1, 4, 5, 6, 7, 13).forEach {
            assertThat(section[it].testAccess.adapterPosition, equalTo(0))
            assertThat(section[it].testAccess.totalItems, equalTo(0))
        }
        assertThat(root.testAccess.adapterPosition, equalTo(0))
        assertThat(root.testAccess.totalItems, equalTo(0))
        listOf(3, 8, 9, 10, 11, 12, 14, 15).forEach {
            assertThat(section[it].testAccess.adapterPosition, equalTo(0))
            assertThat(section[it].testAccess.totalItems, equalTo(0))
        }
    }

    @Test
    fun addAndRemoveItemsBeforeSubsection() {
        // Add item before section 14
        val parent = section[8]
        parent.testAccess.addItems(0, 0, 1)
        assertThat(parent.numChildren, equalTo(2))
        assertThat(parent.testAccess.adapterPosition, equalTo(0))
        assertThat(parent.testAccess.totalItems, equalTo(1))
        assertThat(parent.testAccess.subsections.size, equalTo(1))

        assertThat(section[14].testAccess.adapterPosition, equalTo(1))

        parent.testAccess.removeItems(0, 0, 1)
        assertThat(parent.numChildren, equalTo(1))
        assertThat(parent.testAccess.adapterPosition, equalTo(0))
        assertThat(parent.testAccess.totalItems, equalTo(0))
        assertThat(parent.testAccess.subsections.size, equalTo(1))

        assertThat(section[14].testAccess.adapterPosition, equalTo(0))
    }

    @Test
    fun addAndRemoveItemsAfterZeroSizeSubsection() {
        val parent = section[8]
        parent.testAccess.addItems(1, 0, 1)
        assertThat(parent.numChildren, equalTo(2))
        assertThat(parent.testAccess.adapterPosition, equalTo(0))
        assertThat(parent.testAccess.totalItems, equalTo(1))
        assertThat(parent.testAccess.subsections.size, equalTo(1))

        assertThat(section[14].testAccess.adapterPosition, equalTo(0))

        parent.testAccess.removeItems(1, 0, 1)
        assertThat(parent.numChildren, equalTo(1))
        assertThat(parent.testAccess.adapterPosition, equalTo(0))
        assertThat(parent.testAccess.totalItems, equalTo(0))
        assertThat(parent.testAccess.subsections.size, equalTo(1))

        assertThat(section[14].testAccess.adapterPosition, equalTo(0))
    }

    @Test
    fun itemChangesPropagateAcrossGraph() {
        val parent = section[8]
        parent.testAccess.addItems(0, 0, 1)

        // Unchanged
        listOf(1, 4, 5, 6, 7, 13).forEach {
            assertThat(section[it].testAccess.adapterPosition, equalTo(0))
            assertThat(section[it].testAccess.totalItems, equalTo(0))
        }
        // Total items adjusted
        listOf(0, 2).forEach {
            assertThat(section[it].testAccess.adapterPosition, equalTo(0))
            assertThat(section[it].testAccess.totalItems, equalTo(1))
        }

        // Adapter position adjusted
        listOf(3, 9, 10, 11, 12, 14, 15).forEach {
            assertThat(section[it].testAccess.adapterPosition, equalTo(1))
            assertThat(section[it].testAccess.totalItems, equalTo(0))
        }

        parent.testAccess.removeItems(0, 0, 1)
        // Unchanged
        listOf(1, 4, 5, 6, 7, 13).forEach {
            assertThat(section[it].testAccess.adapterPosition, equalTo(0))
            assertThat(section[it].testAccess.totalItems, equalTo(0))
        }
        // Total items adjusted
        listOf(0, 2).forEach {
            assertThat(section[it].testAccess.adapterPosition, equalTo(0))
            assertThat(section[it].testAccess.totalItems, equalTo(0))
        }

        // Adapter position adjusted
        listOf(3, 9, 10, 11, 12, 14, 15).forEach {
            assertThat(section[it].testAccess.adapterPosition, equalTo(0))
            assertThat(section[it].testAccess.totalItems, equalTo(0))
        }
    }
}
