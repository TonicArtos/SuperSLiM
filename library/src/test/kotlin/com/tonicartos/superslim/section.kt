package com.tonicartos.superslim

import com.tonicartos.superslim.internal.SectionState
import com.tonicartos.superslim.layout.LinearSectionConfig
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.sameInstance
import org.junit.Before
import org.junit.Test

class Section {
    lateinit var section: SectionState

    @Before
    fun setup() {
        section = LinearSectionConfig().makeSection()
    }

    @Test
    fun creation() {
        assertThat("Initial number of children is zero.", section.numChildren, equalTo(0))
        assertThat("Initial total items is zero.", section.totalItems, equalTo(0))
        assertThat("Initial adapter position is zero.", section.positionInAdapter, equalTo(0))
        assertThat("No subsections in initial state.", section.subsections.size, equalTo(0))
    }

    /**
     * Check inserting an item into an empty section.
     */
    @Test
    fun insertItem_propertiesUpdated() {
        assertThat("Before: Section in unmodified initial state.", section.totalItems, equalTo(0))
        assertThat("Before: Section in unmodified initial state.", section.numChildren, equalTo(0))

        section.addItems(0, 1)

        assertThat("After: Total items is one after adding a single item.", section.totalItems, equalTo(1))
        assertThat("After: Number of children is one after adding a single item.", section.numChildren, equalTo(1))
    }

    /**
     * Check removing an item from an otherwise empty section.
     */
    @Test
    fun removeItem_propertiesUpdated() {
        section.addItems(0, 1)

        assertThat("Before: Section in modified initial state. Total items is one.", section.totalItems, equalTo(1))
        assertThat("Before: Section in modified initial state. Number of children is one.", section.numChildren, equalTo(1))

        section.removeItems(0, 1)

        assertThat("After: Total items is zero after removing item.", section.totalItems, equalTo(0))
        assertThat("After: Number of children is zero after removing item.", section.numChildren, equalTo(0))
    }

    /**
     * Check that remove items only removes items actually within the section.
     */
    @Test
    fun removeItem_constrainRemovalRange() {
        section.addItems(0, 10)
        val subsection = LinearSectionConfig().makeSection()
        subsection.addItems(0, 10)
        section.insertSection(10, subsection)

        assertThat("Before: Section in modified initial state. Adapter position initialised to 10.", subsection.positionInAdapter, equalTo(10))
        assertThat("Before: Section in modified initial state. Number of children initialised to 10.", subsection.numChildren, equalTo(10))
        assertThat("Before: Section in modified initial state. Total items initialised to 10.", subsection.totalItems, equalTo(10))

        // Remove items before the section.
        subsection.removeItems(5, 3)
        assertThat("After remove items before section, but not overlapping: Adapter position unchanged.", subsection.positionInAdapter, equalTo(10))
        assertThat("After remove items before section, but not overlapping: Total items unchanged.", subsection.totalItems, equalTo(10))
        assertThat("After remove items before section, but not overlapping: Number of children unchanged.", subsection.numChildren, equalTo(10))

        // Remove items after the section.
        subsection.removeItems(20, 5)
        assertThat("After remove items after section, but not overlapping: Adapter position unchanged.", subsection.positionInAdapter, equalTo(10))
        assertThat("After remove items after section, but not overlapping: Total items unchanged.", subsection.totalItems, equalTo(10))
        assertThat("After remove items after section, but not overlapping: Number of children unchanged.", subsection.numChildren, equalTo(10))

        // Remove items in ranges overlapping the section.
        // Overlapping at start.
        subsection.removeItems(5, 7)
        assertThat("After remove items overlapping start of section: Adapter position unchanged.", subsection.positionInAdapter, equalTo(10))
        assertThat("After remove items overlapping start of section: Total items reduced by two as five items to be removed are before section.", subsection.totalItems, equalTo(8))
        assertThat("After remove items overlapping start of section: Number of children reduced by two as two items were removed.", subsection.numChildren, equalTo(8))

        // Overlapping at end.
        subsection.removeItems(15, 10)
        assertThat("After remove items overlapping end of section: Adapter position unchanged.", subsection.positionInAdapter, equalTo(10))
        assertThat("After remove items overlapping end of section: Total items reduced by three as seven items were after the section.", subsection.totalItems, equalTo(5))
        assertThat("After remove items overlapping end of section: Number of children reduced by three as three items were removed.", subsection.numChildren, equalTo(5))

        // Overlapping across whole range.
        subsection.removeItems(0, 100)
        assertThat("After remove items overlapping whole section: Adapter position unchanged.", subsection.positionInAdapter, equalTo(10))
        assertThat("After remove items overlapping whole section: Total items is zero as entire section is within removal range.", subsection.totalItems, equalTo(0))
        assertThat("After remove items overlapping whole section: Number of children is zero as all items were removed.", subsection.numChildren, equalTo(0))
    }

    /**
     * Check inserting an empty section.
     */
    @Test
    fun insertEmptySection() {
        val emptySubsection = LinearSectionConfig().makeSection()

        assertThat("Before: Section in unmodified initial state.", section.totalItems, equalTo(0))
        assertThat("Before: Section in unmodified initial state.", section.numChildren, equalTo(0))
        assertThat("Before: Section in unmodified initial state.", section.subsections.size, equalTo(0))

        section.insertSection(0, emptySubsection)

        assertThat("After: Total items unchanged as inserted section had no items.", section.totalItems, equalTo(0))
        assertThat("After: Number of children is one as a single section was inserted.", section.numChildren, equalTo(1))
        assertThat("After: Number of subsections now one as a single section was inserted.", section.subsections.size, equalTo(1))
        assertThat("After: First, and only, subsection is same instance as that section inserted.", section.subsections[0], sameInstance(emptySubsection))
    }

    /**
     * Check inserting a populated section.
     */
    @Test
    fun insertPopulatedSubsection() {
        val populatedSubsection = LinearSectionConfig().makeSection()
        populatedSubsection.addItems(0, 10)

        assertThat("Before: Section in unmodified initial state.", section.totalItems, equalTo(0))
        assertThat("Before: Section in unmodified initial state.", section.numChildren, equalTo(0))
        assertThat("Before: Section in unmodified initial state.", section.subsections.size, equalTo(0))

        section.insertSection(0, populatedSubsection)

        assertThat("After: Total items is ten after subsection with ten items was inserted.", section.totalItems, equalTo(10))
        assertThat("After: Number of children is one after subsection was inserted.", section.numChildren, equalTo(1))
        assertThat("After: Number of subsections now one as a single section was inserted.", section.subsections.size, equalTo(1))
        assertThat("After: First, and only, subsection is same instance as that section inserted.", section.subsections[0], sameInstance(populatedSubsection))
    }

    @Test
    fun insertSubsectionBetweenItems() {
        section.addItems(0, 10)
        assertThat("Before: Section in modified initial state. Adapter position is zero.", section.positionInAdapter, equalTo(0))
        assertThat("Before: Section in modified initial state. Number of children initialised to 10.", section.numChildren, equalTo(10))
        assertThat("Before: Section in modified initial state. Total items initialised to 10.", section.totalItems, equalTo(10))

        val subsection = LinearSectionConfig().makeSection()
        section.insertSection(5, subsection)

        assertThat("After: Inserted section adapter position is five as it was inserted into the fifth position of parent section.", subsection.positionInAdapter, equalTo(5))
        assertThat("After: Number of children of parent section is 11.", section.numChildren, equalTo(11))
        assertThat("After: Total items of parent section is unchanged as inserted section was empty.", section.totalItems, equalTo(10))
    }

    @Test
    fun insertEmptySubsectionBetweenSubsections() {
        val subsection = arrayListOf(LinearSectionConfig().makeSection(), LinearSectionConfig().makeSection())
        subsection[0].addItems(0, 10)
        subsection[1].addItems(0, 5)

        section.insertSection(0, subsection[0])
        section.insertSection(1, subsection[1])

        assertThat("Before: Subsection 0, adapter position is zero.", subsection[0].positionInAdapter, equalTo(0))
        assertThat("Before: Subsection 0, total items is ten.", subsection[0].totalItems, equalTo(10))
        assertThat("Before: Subsection 1, adapter position is ten.", subsection[1].positionInAdapter, equalTo(10))
        assertThat("Before: Subsection 1, total items is five.", subsection[1].totalItems, equalTo(5))
        assertThat("Before: Parent section, number of children is two.", section.numChildren, equalTo(2))
        assertThat("Before: Parent section, number subsections is two.", section.subsections.size, equalTo(2))
        assertThat("Before: Parent section, total items is fifteen.", section.totalItems, equalTo(15))
        assertThat("Before: First item of subsection in parent is instance of subsection 0.", section.subsections[0], sameInstance(subsection[0]))
        assertThat("Before: Second item of subsections in parent is instance of subsection 1.", section.subsections[1], sameInstance(subsection[1]))

        val insertedSubsection = LinearSectionConfig().makeSection()
        section.insertSection(1, insertedSubsection)
        assertThat("After: Inserted section, adapter position is ten.", insertedSubsection.positionInAdapter, equalTo(10))
        assertThat("After: Subsection 0, adapter position is zero.", subsection[0].positionInAdapter, equalTo(0))
        assertThat("After: Subsection 0, total items is ten.", subsection[0].totalItems, equalTo(10))
        assertThat("After: Subsection 1, adapter position is ten and unchanged as section inserted before this had no items.", subsection[1].positionInAdapter, equalTo(10))
        assertThat("After: Subsection 1, total items is five.", subsection[1].totalItems, equalTo(5))
        assertThat("After: Parent section, number of children is three.", section.numChildren, equalTo(3))
        assertThat("After: Parent section, number subsections is three.", section.subsections.size, equalTo(3))
        assertThat("After: Parent section, total items is fifteen and unchanged as inserted section had no items.", section.totalItems, equalTo(15))
        assertThat("After: First item of subsection in parent is instance of subsection 0.", section.subsections[0], sameInstance(subsection[0]))
        assertThat("After: Second item of subsections in parent is instance of inserted subsection.", section.subsections[1], sameInstance(insertedSubsection))
        assertThat("After: Third item of subsections in parent is instance of subsection 1.", section.subsections[2], sameInstance(subsection[1]))
    }

    @Test
    fun insertPopulatedSubsectionBetweenSubsections() {
        val subsection = arrayListOf(LinearSectionConfig().makeSection(), LinearSectionConfig().makeSection())
        subsection[0].addItems(0, 10)
        subsection[1].addItems(0, 5)

        section.insertSection(0, subsection[0])
        section.insertSection(1, subsection[1])

        assertThat("Before: Subsection 0, adapter position is zero.", subsection[0].positionInAdapter, equalTo(0))
        assertThat("Before: Subsection 0, total items is ten.", subsection[0].totalItems, equalTo(10))
        assertThat("Before: Subsection 1, adapter position is ten.", subsection[1].positionInAdapter, equalTo(10))
        assertThat("Before: Subsection 1, total items is five.", subsection[1].totalItems, equalTo(5))
        assertThat("Before: Parent section, number of children is two.", section.numChildren, equalTo(2))
        assertThat("Before: Parent section, number subsections is two.", section.subsections.size, equalTo(2))
        assertThat("Before: Parent section, total items is fifteen.", section.totalItems, equalTo(15))
        assertThat("Before: First item of subsection in parent is instance of subsection 0.", section.subsections[0], sameInstance(subsection[0]))
        assertThat("Before: Second item of subsections in parent is instance of subsection 1.", section.subsections[1], sameInstance(subsection[1]))

        val insertedSubsection = LinearSectionConfig().makeSection()
        insertedSubsection.addItems(0, 7)
        section.insertSection(1, insertedSubsection)
        assertThat("After: Inserted section, adapter position is ten.", insertedSubsection.positionInAdapter, equalTo(10))
        assertThat("After: Subsection 0, adapter position is zero.", subsection[0].positionInAdapter, equalTo(0))
        assertThat("After: Subsection 0, total items is ten.", subsection[0].totalItems, equalTo(10))
        assertThat("After: Subsection 1, adapter position is seventeen and was changed because section inserted before this had seven items.", subsection[1].positionInAdapter, equalTo(17))
        assertThat("After: Subsection 1, total items is five.", subsection[1].totalItems, equalTo(5))
        assertThat("After: Parent section, number of children is three.", section.numChildren, equalTo(3))
        assertThat("After: Parent section, number subsections is three.", section.subsections.size, equalTo(3))
        assertThat("After: Parent section, total items is twenty two as inserted section had seven items and increased total count in parent.", section.totalItems, equalTo(22))
        assertThat("After: First item of subsection in parent is instance of subsection 0.", section.subsections[0], sameInstance(subsection[0]))
        assertThat("After: Second item of subsections in parent is instance of inserted subsection.", section.subsections[1], sameInstance(insertedSubsection))
        assertThat("After: Third item of subsections in parent is instance of subsection 1.", section.subsections[2], sameInstance(subsection[1]))
    }

    @Test
    fun insertItemsAfterSections() {
        val subsections = (0..1).map { LinearSectionConfig().makeSection() }
        subsections.forEach { section.insertSection(-1, it) }

        assertThat("Before: Number of children is two.", section.numChildren, equalTo(2))
        assertThat("Before: Total items is zero.", section.totalItems, equalTo(0))
        assertThat("Before: Subsection 0 adapter position is zero.", subsections[0].positionInAdapter, equalTo(0))
        assertThat("Before: Subsection 1 adapter position is zero.", subsections[1].positionInAdapter, equalTo(0))

        section.addItems(2, 5)
        assertThat("After: Number of children is seven.", section.numChildren, equalTo(7))
        assertThat("After: Total items is five.", section.totalItems, equalTo(5))
        assertThat("After: Subsection 0 adapter position is still zero.", subsections[0].positionInAdapter, equalTo(0))
        assertThat("After: Subsection 1 adapter position is still zero.", subsections[1].positionInAdapter, equalTo(0))

        section.addItems(-1, 6)
        assertThat("After: Number of children is thirteen.", section.numChildren, equalTo(13))
        assertThat("After: Total items is eleven.", section.totalItems, equalTo(11))
        assertThat("After: Subsection 0 adapter position is still zero.", subsections[0].positionInAdapter, equalTo(0))
        assertThat("After: Subsection 1 adapter position is still zero.", subsections[1].positionInAdapter, equalTo(0))
    }

    @Test
    fun insertItemsBeforeSections() {
        val subsections = (0..1).map { LinearSectionConfig().makeSection() }
        subsections.forEach { section.insertSection(-1, it) }

        assertThat("Before: Number of children is two.", section.numChildren, equalTo(2))
        assertThat("Before: Total items is zero.", section.totalItems, equalTo(0))
        assertThat("Before: Subsection 0 adapter position is zero.", subsections[0].positionInAdapter, equalTo(0))
        assertThat("Before: Subsection 1 adapter position is zero.", subsections[1].positionInAdapter, equalTo(0))

        section.addItems(0, 5)
        assertThat("After: Number of children is seven.", section.numChildren, equalTo(7))
        assertThat("After: Total items is five.", section.totalItems, equalTo(5))
        assertThat("After: Subsection 0 adapter position is now five.", subsections[0].positionInAdapter, equalTo(5))
        assertThat("After: Subsection 1 adapter position is now five.", subsections[1].positionInAdapter, equalTo(5))
    }

    @Test
    fun insertItemsBetweenSections() {
        val subsections = (0..1).map { LinearSectionConfig().makeSection() }
        subsections.forEach { section.insertSection(-1, it) }

        assertThat("Before: Number of children is two.", section.numChildren, equalTo(2))
        assertThat("Before: Total items is zero.", section.totalItems, equalTo(0))
        assertThat("Before: Subsection 0 adapter position is zero.", subsections[0].positionInAdapter, equalTo(0))
        assertThat("Before: Subsection 1 adapter position is zero.", subsections[1].positionInAdapter, equalTo(0))

        section.addItems(1, 5)
        assertThat("After: Number of children is seven.", section.numChildren, equalTo(7))
        assertThat("After: Total items is five.", section.totalItems, equalTo(5))
        assertThat("After: Subsection 0 adapter position is still zero.", subsections[0].positionInAdapter, equalTo(0))
        assertThat("After: Subsection 1 adapter position is now five.", subsections[1].positionInAdapter, equalTo(5))
    }

    /**
     * Check most complex form of section insertion. This involves a section with a mix of item and subsections. Checks
     * a variety of possible cases.
     */
    @Test
    fun insertSectionBetweenItemsAndSubsections() {
        val subsection = (0..3).map { i -> LinearSectionConfig().makeSection().apply { addItems(0, i) } }

        // Want to throw a zero size section after insertion point, so the subsections are put in out of order.
        section.insertSection(-1, subsection[1])
        section.addItems(-1, 5)
        section.insertSection(-1, subsection[2])
        section.addItems(-1, 5)
        section.insertSection(-1, subsection[0])
        section.addItems(-1, 5)
        section.insertSection(-1, subsection[3])

        assertThat("Before: Section has 4 subsections.", section.subsections.size, equalTo(4))
        assertThat("Before: Section has nineteen children.", section.numChildren, equalTo(19))
        assertThat("Before: Section has twenty one items.", section.totalItems, equalTo(21))
        assertThat("Before: Subsection 1 adapter position is zero.", subsection[1].positionInAdapter, equalTo(0))
        assertThat("Before: Subsection 2 adapter position is six.", subsection[2].positionInAdapter, equalTo(6))
        assertThat("Before: Subsection 0 adapter position is thirteen.", subsection[0].positionInAdapter, equalTo(13))
        assertThat("Before: Subsection 3 adapter position is eighteen.", subsection[3].positionInAdapter, equalTo(18))

        val insertedSubsection = LinearSectionConfig().makeSection()
        insertedSubsection.addItems(0, 15)

        section.insertSection(9, insertedSubsection)
        assertThat("After: Section has 5 subsections.", section.subsections.size, equalTo(5))
        assertThat("After: Section has twenty children.", section.numChildren, equalTo(20))
        assertThat("After: Section has thirty six items.", section.totalItems, equalTo(36))
        assertThat("After: Subsection 1 adapter position is zero.", subsection[1].positionInAdapter, equalTo(0))
        assertThat("After: Subsection 2 adapter position is six.", subsection[2].positionInAdapter, equalTo(6))
        assertThat("After: InsertedSubsection adapter position is ten.", insertedSubsection.positionInAdapter, equalTo(10))
        assertThat("After: Subsection 0 adapter position is twenty eight.", subsection[0].positionInAdapter, equalTo(28))
        assertThat("After: Subsection 3 adapter position is thirty three.", subsection[3].positionInAdapter, equalTo(33))
        assertThat("After: Subsection 1 is in position zero of subsection list.", section.subsections[0], sameInstance(subsection[1]))
        assertThat("After: Subsection 2 is in position one of subsection list.", section.subsections[1], sameInstance(subsection[2]))
        assertThat("After: InsertedSubsection is in position two of subsection list.", section.subsections[2], sameInstance(insertedSubsection))
        assertThat("After: Subsection 0 is in position three of subsection list.", section.subsections[3], sameInstance(subsection[0]))
        assertThat("After: Subsection 3 is in position four of subsection list.", section.subsections[4], sameInstance(subsection[3]))
    }

    /**
     * Check removing an empty section.
     */
    @Test
    fun removeEmptySubsection() {
        val emptySubsection = LinearSectionConfig().makeSection()

        section.insertSection(0, emptySubsection)

        assertThat("Before: Total items is zero.", section.totalItems, equalTo(0))
        assertThat("Before: Number of children is one.", section.numChildren, equalTo(1))
        assertThat("Before: Subsections in section is one.", section.subsections.size, equalTo(1))

        section.removeSection(emptySubsection)

        assertThat("After: Total items is zero.", section.totalItems, equalTo(0))
        assertThat("After: Number of children is zero.", section.numChildren, equalTo(0))
        assertThat("After: Subsections in section is zero.", section.subsections.size, equalTo(0))
    }

    @Test
    fun removeEmptySubsectionBetweenSubsections() {
        val subsection = (0..1).map { LinearSectionConfig().makeSection().apply { addItems(0, 5) } }
        subsection.forEach { section.insertSection(-1, it) }
        val emptySubsection = LinearSectionConfig().makeSection()

        section.insertSection(1, emptySubsection)

        assertThat("Before: Total items is ten.", section.totalItems, equalTo(10))
        assertThat("Before: Number of children is three.", section.numChildren, equalTo(3))
        assertThat("Before: Subsections in section is three.", section.subsections.size, equalTo(3))
        assertThat("Before: Subsection 0 adapter position is zero.", subsection[0].positionInAdapter, equalTo(0))
        assertThat("Before: EmptySubsection position is five.", emptySubsection.positionInAdapter, equalTo(5))
        assertThat("Before: Subsection 1 adapter position is five.", subsection[1].positionInAdapter, equalTo(5))
        assertThat("Before: Subsection in position 0 in section is subsection 0.", section.subsections[0], sameInstance(subsection[0]))
        assertThat("Before: Subsection in position 1 in section is emptySubsection.", section.subsections[1], sameInstance(emptySubsection))
        assertThat("Before: Subsection in position 2 in section is subsection 1.", section.subsections[2], sameInstance(subsection[1]))

        section.removeSection(emptySubsection)

        assertThat("After: Total items is ten.", section.totalItems, equalTo(10))
        assertThat("After: Number of children is two.", section.numChildren, equalTo(2))
        assertThat("After: Subsections in section is two.", section.subsections.size, equalTo(2))
        assertThat("After: Subsection 0 adapter position is zero.", subsection[0].positionInAdapter, equalTo(0))
        assertThat("After: Subsection 1 adapter position is five.", subsection[1].positionInAdapter, equalTo(5))
        assertThat("After: Subsection in position 0 in section is subsection 0.", section.subsections[0], sameInstance(subsection[0]))
        assertThat("After: Subsection in position 1 in section is subsection 1.", section.subsections[1], sameInstance(subsection[1]))
    }

    @Test
    fun removeEmptySubsectionBetweenItemsAndSubsections() {
        val subsection = (0..1).map { LinearSectionConfig().makeSection().apply { addItems(0, 5) } }
        section.insertSection(-1, subsection[0])
        section.addItems(-1, 7)
        section.insertSection(-1, subsection[1])

        val emptySubsection = LinearSectionConfig().makeSection()
        section.insertSection(4, emptySubsection)

        assertThat("Before: Total items is seventeen.", section.totalItems, equalTo(17))
        assertThat("Before: Number of children is ten.", section.numChildren, equalTo(10))
        assertThat("Before: Subsections in section is three.", section.subsections.size, equalTo(3))
        assertThat("Before: Subsection 0 adapter position is zero.", subsection[0].positionInAdapter, equalTo(0))
        assertThat("Before: EmptySubsection adapter position is nine.", emptySubsection.positionInAdapter, equalTo(8))
        assertThat("Before: Subsection 1 adapter position is twelve.", subsection[1].positionInAdapter, equalTo(12))
        assertThat("Before: Subsection in position 0 in section is subsection 0.", section.subsections[0], sameInstance(subsection[0]))
        assertThat("Before: Subsection in position 1 in section is emptySubsection.", section.subsections[1], sameInstance(emptySubsection))
        assertThat("Before: Subsection in position 2 in section is subsection 1.", section.subsections[2], sameInstance(subsection[1]))

        section.removeSection(emptySubsection)

        assertThat("After: Total items is seventeen.", section.totalItems, equalTo(17))
        assertThat("After: Number of children is nine.", section.numChildren, equalTo(9))
        assertThat("After: Subsections in section is two.", section.subsections.size, equalTo(2))
        assertThat("After: Subsection 0 adapter position is zero.", subsection[0].positionInAdapter, equalTo(0))
        assertThat("After: Subsection 1 adapter position is twelve.", subsection[1].positionInAdapter, equalTo(12))
        assertThat("After: Subsection in position 0 in section is subsection 0.", section.subsections[0], sameInstance(subsection[0]))
        assertThat("After: Subsection in position 1 in section is subsection 1.", section.subsections[1], sameInstance(subsection[1]))
    }

    @Test
    fun removePopulatedSubsectionBetweenSubsections() {
        val subsection = (0..1).map { LinearSectionConfig().makeSection().apply { addItems(0, 5) } }
        subsection.forEach { section.insertSection(-1, it) }
        val populatedSubsection = LinearSectionConfig().makeSection().apply { addItems(0, 10) }

        section.insertSection(1, populatedSubsection)

        assertThat("Before: Total items is twenty.", section.totalItems, equalTo(20))
        assertThat("Before: Number of children is three.", section.numChildren, equalTo(3))
        assertThat("Before: Subsections in section is three.", section.subsections.size, equalTo(3))
        assertThat("Before: Subsection 0 adapter position is zero.", subsection[0].positionInAdapter, equalTo(0))
        assertThat("Before: PopulatedSubsection position is five.", populatedSubsection.positionInAdapter, equalTo(5))
        assertThat("Before: Subsection 1 adapter position is fifteen.", subsection[1].positionInAdapter, equalTo(15))
        assertThat("Before: Subsection in position 0 in section is subsection 0.", section.subsections[0], sameInstance(subsection[0]))
        assertThat("Before: Subsection in position 1 in section is emptySubsection.", section.subsections[1], sameInstance(populatedSubsection))
        assertThat("Before: Subsection in position 2 in section is subsection 1.", section.subsections[2], sameInstance(subsection[1]))

        section.removeSection(populatedSubsection)

        assertThat("After: Total items is ten.", section.totalItems, equalTo(10))
        assertThat("After: Number of children is two.", section.numChildren, equalTo(2))
        assertThat("After: Subsections in section is two.", section.subsections.size, equalTo(2))
        assertThat("After: Subsection 0 adapter position is zero.", subsection[0].positionInAdapter, equalTo(0))
        assertThat("After: Subsection 1 adapter position is five.", subsection[1].positionInAdapter, equalTo(5))
        assertThat("After: Subsection in position 0 in section is subsection 0.", section.subsections[0], sameInstance(subsection[0]))
        assertThat("After: Subsection in position 1 in section is subsection 1.", section.subsections[1], sameInstance(subsection[1]))
    }

    @Test
    fun removePopulatedSubsectionBetweenItemsAndSubsections() {
        val subsection = (0..1).map { LinearSectionConfig().makeSection().apply { addItems(0, 5) } }
        section.insertSection(-1, subsection[0])
        section.addItems(-1, 7)
        section.insertSection(-1, subsection[1])

        val populatedSubsection = LinearSectionConfig().makeSection().apply { addItems(0, 10) }
        section.insertSection(4, populatedSubsection)

        assertThat("Before: Total items is twenty seven.", section.totalItems, equalTo(27))
        assertThat("Before: Number of children is ten.", section.numChildren, equalTo(10))
        assertThat("Before: Subsections in section is three.", section.subsections.size, equalTo(3))
        assertThat("Before: Subsection 0 adapter position is zero.", subsection[0].positionInAdapter, equalTo(0))
        assertThat("Before: PopulatedSubsection adapter position is nine.", populatedSubsection.positionInAdapter, equalTo(8))
        assertThat("Before: Subsection 1 adapter position is twenty two.", subsection[1].positionInAdapter, equalTo(22))
        assertThat("Before: Subsection in position 0 in section is subsection 0.", section.subsections[0], sameInstance(subsection[0]))
        assertThat("Before: Subsection in position 1 in section is emptySubsection.", section.subsections[1], sameInstance(populatedSubsection))
        assertThat("Before: Subsection in position 2 in section is subsection 1.", section.subsections[2], sameInstance(subsection[1]))

        section.removeSection(populatedSubsection)

        assertThat("After: Total items is seventeen.", section.totalItems, equalTo(17))
        assertThat("After: Number of children is nine.", section.numChildren, equalTo(9))
        assertThat("After: Subsections in section is two.", section.subsections.size, equalTo(2))
        assertThat("After: Subsection 0 adapter position is zero.", subsection[0].positionInAdapter, equalTo(0))
        assertThat("After: Subsection 1 adapter position is twelve.", subsection[1].positionInAdapter, equalTo(12))
        assertThat("After: Subsection in position 0 in section is subsection 0.", section.subsections[0], sameInstance(subsection[0]))
        assertThat("After: Subsection in position 1 in section is subsection 1.", section.subsections[1], sameInstance(subsection[1]))
    }

    /**
     * Check removing a populated section.
     */
    @Test
    fun removePopulatedSubsection() {
        val populatedSubsection = LinearSectionConfig().makeSection()
        populatedSubsection.addItems(0, 10)

        assertThat("Before: Total items is zero.", section.totalItems, equalTo(0))
        assertThat("Before: Number of children is zero.", section.numChildren, equalTo(0))
        assertThat("Before: Number of subsections in section is zero.", section.subsections.size, equalTo(0))

        section.insertSection(0, populatedSubsection)

        assertThat("After: Total items is ten.", section.totalItems, equalTo(10))
        assertThat("After: Number of children is one.", section.numChildren, equalTo(1))
        assertThat("After: Number of subsections in section is one.", section.subsections.size, equalTo(1))
        assertThat("After: Subsection at position 0 in section is populatedSubsection.", section.subsections[0], sameInstance(populatedSubsection))
    }

    /**
     * Check items change in subsection correctly updates total items in this section.
     */
    @Test
    fun totalItemsChangedInSubsection_updatesTotalItems() {
        // Test by adding a subsection, and then add an item to the subsection and verify the total item count in this
        // section was correctly updated.
        val subsection = LinearSectionConfig().makeSection()
        section.insertSection(0, subsection)

        assertThat("Before: Total items is zero.", section.totalItems, equalTo(0))

        subsection.addItems(0, 1)

        assertThat("After: Total items is one.", section.totalItems, equalTo(1))
    }
}

/**
 * Verify changes to a section results in changes being propagated outside of that section.
 */
class PropagationToParent {
    lateinit var parent: SectionState
    lateinit var section: SectionState

    @Before
    fun setup() {
        section = LinearSectionConfig().makeSection()
        parent = LinearSectionConfig().makeSection().apply {
            addItems(0, 5)
            insertSection(0, section)
        }
    }

    @Test
    fun insertItem() {
        assertThat("Before: Total items in parent is five.", parent.totalItems, equalTo(5))

        section.addItems(0, 15)
        assertThat("After: Total items in parent is twenty.", parent.totalItems, equalTo(20))
    }

    @Test
    fun removeItem() {
        section.addItems(0, 15)
        assertThat("Before: Total items in parent is twenty.", parent.totalItems, equalTo(20))

        section.removeItems(0, 15)
        assertThat("After: Total items in parent is five.", parent.totalItems, equalTo(5))
    }

    @Test
    fun insertSubsection() {
        val subsection = LinearSectionConfig().makeSection().apply { addItems(0, 15) }

        assertThat("Before: Total items in parent is five.", parent.totalItems, equalTo(5))

        section.insertSection(0, subsection)
        assertThat("After: Total items in parent is twenty.", parent.totalItems, equalTo(20))

    }

    @Test
    fun removeSubsection() {
        val subsection = LinearSectionConfig().makeSection().apply { addItems(0, 15) }
        section.insertSection(0, subsection)
        assertThat("Before: Total items in parent is twenty.", parent.totalItems, equalTo(20))

        section.removeSection(subsection)
        assertThat("After: Total items in parent is five.", parent.totalItems, equalTo(5))
    }
}

class PropagationToSiblings {
    lateinit var parent: SectionState
    lateinit var section: SectionState
    lateinit var sibling: List<SectionState>

    @Before
    fun setup() {
        section = LinearSectionConfig().makeSection()
        sibling = (0..3).map { LinearSectionConfig().makeSection().apply { addItems(0, 5) } }
        parent = LinearSectionConfig().makeSection().apply {
            sibling.forEach { insertSection(-1, it) }
        }
    }

    @Test
    fun totalItemsChangeInSubsection_updatesSubsectionSiblingAdapterPositions() {
        parent.insertSection(2, section)

        assertThat("Before: Sibling 0 has adapter position zero.", sibling[0].positionInAdapter, equalTo(0))
        assertThat("Before: Sibling 1 has adapter position five.", sibling[1].positionInAdapter, equalTo(5))
        assertThat("Before: Sibling 2 has adapter position ten.", sibling[2].positionInAdapter, equalTo(10))
        assertThat("Before: Sibling 3 has adapter position fifteen.", sibling[3].positionInAdapter, equalTo(15))

        // Trigger total items change by adding items to section.
        section.addItems(0, 6)

        assertThat("Before: Sibling 0 has adapter position zero.", sibling[0].positionInAdapter, equalTo(0))
        assertThat("Before: Sibling 1 has adapter position five.", sibling[1].positionInAdapter, equalTo(5))
        assertThat("Before: Sibling 2 has adapter position sixteen.", sibling[2].positionInAdapter, equalTo(16))
        assertThat("Before: Sibling 3 has adapter position twenty one.", sibling[3].positionInAdapter, equalTo(21))
    }

    @Test
    fun insertEmptySubsection_noChangesToSiblings() {
        assertThat("Before: Sibling 0 has adapter position zero.", sibling[0].positionInAdapter, equalTo(0))
        assertThat("Before: Sibling 1 has adapter position five.", sibling[1].positionInAdapter, equalTo(5))
        assertThat("Before: Sibling 2 has adapter position ten.", sibling[2].positionInAdapter, equalTo(10))
        assertThat("Before: Sibling 3 has adapter position fifteen.", sibling[3].positionInAdapter, equalTo(15))

        parent.insertSection(2, section)

        assertThat("After: Sibling 0 has adapter position zero.", sibling[0].positionInAdapter, equalTo(0))
        assertThat("After: Sibling 1 has adapter position five.", sibling[1].positionInAdapter, equalTo(5))
        assertThat("After: Sibling 2 has adapter position ten.", sibling[2].positionInAdapter, equalTo(10))
        assertThat("After: Sibling 3 has adapter position fifteen.", sibling[3].positionInAdapter, equalTo(15))
    }

    @Test
    fun insertPopulatedSubsection_siblingsAdapterPositionsUpdated() {
        section.addItems(0, 6)

        assertThat("Before: Sibling 0 has adapter position zero.", sibling[0].positionInAdapter, equalTo(0))
        assertThat("Before: Sibling 1 has adapter position five.", sibling[1].positionInAdapter, equalTo(5))
        assertThat("Before: Sibling 2 has adapter position ten.", sibling[2].positionInAdapter, equalTo(10))
        assertThat("Before: Sibling 3 has adapter position fifteen.", sibling[3].positionInAdapter, equalTo(15))

        parent.insertSection(2, section)

        assertThat("Before: Sibling 0 has adapter position zero.", sibling[0].positionInAdapter, equalTo(0))
        assertThat("Before: Sibling 1 has adapter position five.", sibling[1].positionInAdapter, equalTo(5))
        assertThat("Before: Sibling 2 has adapter position sixteen.", sibling[2].positionInAdapter, equalTo(16))
        assertThat("Before: Sibling 3 has adapter position twenty one.", sibling[3].positionInAdapter, equalTo(21))
    }

    @Test
    fun removeEmptySubsection_noChangesToSiblings() {
        parent.insertSection(2, section)

        assertThat("Before: Sibling 0 has adapter position zero.", sibling[0].positionInAdapter, equalTo(0))
        assertThat("Before: Sibling 1 has adapter position five.", sibling[1].positionInAdapter, equalTo(5))
        assertThat("Before: Sibling 2 has adapter position ten.", sibling[2].positionInAdapter, equalTo(10))
        assertThat("Before: Sibling 3 has adapter position fifteen.", sibling[3].positionInAdapter, equalTo(15))

        parent.removeSection(section)

        assertThat("After: Sibling 0 has adapter position zero.", sibling[0].positionInAdapter, equalTo(0))
        assertThat("After: Sibling 1 has adapter position five.", sibling[1].positionInAdapter, equalTo(5))
        assertThat("After: Sibling 2 has adapter position ten.", sibling[2].positionInAdapter, equalTo(10))
        assertThat("After: Sibling 3 has adapter position fifteen.", sibling[3].positionInAdapter, equalTo(15))
    }

    @Test
    fun removePopulatedSubsection_siblingsAdapterPositionsUpdated() {
        section.addItems(0, 6)
        parent.insertSection(2, section)

        assertThat("Before: Sibling 0 has adapter position zero.", sibling[0].positionInAdapter, equalTo(0))
        assertThat("Before: Sibling 1 has adapter position five.", sibling[1].positionInAdapter, equalTo(5))
        assertThat("Before: Sibling 2 has adapter position sixteen.", sibling[2].positionInAdapter, equalTo(16))
        assertThat("Before: Sibling 3 has adapter position twenty one.", sibling[3].positionInAdapter, equalTo(21))

        parent.removeSection(section)

        assertThat("After: Sibling 0 has adapter position zero.", sibling[0].positionInAdapter, equalTo(0))
        assertThat("After: Sibling 1 has adapter position five.", sibling[1].positionInAdapter, equalTo(5))
        assertThat("After: Sibling 2 has adapter position ten.", sibling[2].positionInAdapter, equalTo(10))
        assertThat("After: Sibling 3 has adapter position fifteen.", sibling[3].positionInAdapter, equalTo(15))

    }
}

class PropagationToDescendants {
    lateinit var parent: SectionState
    lateinit var child: List<SectionState>
    lateinit var grandChild: List<SectionState>

    @Before
    fun setup() {
        child = (0..3).map { LinearSectionConfig().makeSection().apply { addItems(0, 5) } }
        grandChild = (0..3).map { LinearSectionConfig().makeSection().apply { addItems(0, 5) } }
        parent = LinearSectionConfig().makeSection().apply {
            child.forEach { insertSection(-1, it) }
        }
        grandChild.forEachIndexed { i, it -> child[2].insertSection(2 * i, it) }
    }

    @Test
    fun removeItems_cascadesToDescendants() {
        assertThat("Before: Parent has forty items.", parent.totalItems, equalTo(40))
        assertThat("Before: Parent has four children.", parent.numChildren, equalTo(4))
        assertThat("Before: Child 2 has twenty five items.", child[2].totalItems, equalTo(25))
        assertThat("Before: Child 2 has nine children.", child[2].numChildren, equalTo(9))
        assertThat("Before: Child 2 has adapter position ten.", child[2].positionInAdapter, equalTo(10))
        grandChild.forEachIndexed { i, it -> assertThat("Before: Grandchild $i has five items.", it.totalItems, equalTo(5)) }
        grandChild.forEachIndexed { i, it -> assertThat("Before: Grandchild $i has five children.", it.numChildren, equalTo(5)) }
        grandChild.forEachIndexed { i, it -> assertThat("Before: Grandchild $i has adapter position ${10 + i * 6}.", it.positionInAdapter, equalTo(10 + i * 6)) }

        // From parent level, remove all items in child 2.
        parent.removeItems(10, 25)

        grandChild.forEachIndexed { i, it -> assertThat("After: Grandchild $i has zero items.", it.totalItems, equalTo(0)) }
        grandChild.forEachIndexed { i, it -> assertThat("After: Grandchild $i has zero children.", it.totalItems, equalTo(0)) }
        assertThat("After: Child 2 has zero items.", child[2].totalItems, equalTo(0))
        assertThat("After: Child 2 has four children.", child[2].numChildren, equalTo(4))
        assertThat("After: Child 2 has adapter position ten.", child[2].positionInAdapter, equalTo(10))
        assertThat("After: Parent has fifteen items.", parent.totalItems, equalTo(15))
        assertThat("After: Parent has four children.", parent.numChildren, equalTo(4))
        grandChild.forEachIndexed { i, it -> assertThat("After: Grandchild $i has adapter position ten.", it.positionInAdapter, equalTo(10)) }
    }

    @Test
    fun positionInAdapterChange_updatesDescendantsAdapterPositions() {
        assertThat("Before: Child 0 has adapter position zero.", child[0].positionInAdapter, equalTo(0))
        assertThat("Before: Child 1 has adapter position five.", child[1].positionInAdapter, equalTo(5))
        assertThat("Before: Child 2 has adapter position ten.", child[2].positionInAdapter, equalTo(10))
        assertThat("Before: Child 3 has adapter position fifteen.", child[3].positionInAdapter, equalTo(35))

        parent.positionInAdapter += 8

        assertThat("After: Child 0 has adapter position eight.", child[0].positionInAdapter, equalTo(8))
        assertThat("After: Child 1 has adapter position thirteen.", child[1].positionInAdapter, equalTo(13))
        assertThat("After: Child 2 has adapter position eighteen.", child[2].positionInAdapter, equalTo(18))
        assertThat("After: Child 3 has adapter position twenty three.", child[3].positionInAdapter, equalTo(43))
    }
}