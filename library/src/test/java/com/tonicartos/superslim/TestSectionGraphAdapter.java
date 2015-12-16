package com.tonicartos.superslim;

import com.tonicartos.superslim.Util.ItemData;
import com.tonicartos.superslim.adapter.Item;
import com.tonicartos.superslim.adapter.Section;

import org.junit.Before;
import org.junit.Test;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;

public class TestSectionGraphAdapter {

    AdapterStub adapter;

    @Test
    public void adapterSectionManagement() {
        adapter.addSection(adapter.createSection("1"));
        adapter.addSection(adapter.createSection("2"));
        adapter.addSection(adapter.createSection("3"));

        adapter.insertSection(1, adapter.createSection("middle"));

        final Section section2 = adapter.getSectionWithId("2");
        assert section2 != null;
        section2.add(adapter.createSection("deep"));
        createSectionItems(adapter.getSectionWithId("deep"), 5);

        assertThat(adapter.getNumSections(), equalTo(4));
        assertThat(adapter.getSection(0), equalTo(adapter.getSectionWithId("1")));
        assertThat(adapter.getSection(1), equalTo(adapter.getSectionWithId("middle")));
        assertThat(adapter.getSection(2), equalTo(adapter.getSectionWithId("2")));
        assertThat(adapter.getSection(3), equalTo(adapter.getSectionWithId("3")));

        assertThat(adapter.getItemCount(), equalTo(5));
        Section deregistered = adapter.deregister("2");
        assert deregistered != null;
        deregistered.removeFromParent();
        assertThat(adapter.getItemCount(), equalTo(0));

        assertThat(adapter.getNumSections(), equalTo(3));
        assertThat(adapter.getSection(0), equalTo(adapter.getSectionWithId("1")));
        assertThat(adapter.getSection(1), equalTo(adapter.getSectionWithId("middle")));
        assertThat(adapter.getSection(2), equalTo(adapter.getSectionWithId("3")));

        adapter.removeSection(1).deregister();
        assertThat(adapter.getNumSections(), equalTo(2));
        assertThat(adapter.getSection(0), equalTo(adapter.getSectionWithId("1")));
        assertThat(adapter.getSection(1), equalTo(adapter.getSectionWithId("3")));
    }

    @Test
    public void addAndRemoveHeader() {
        final ItemData header = ItemData.newInstance();
        final ItemData altHeader = ItemData.newInstance();
        final Section section = adapter.createSection("root", new Item(0, header));
        final ItemData[] items = createSectionItems(section, 5);

        adapter.addSection(section);
        // Check header is first item.
        adapter.bindViewHolder(mock(RecyclerView.ViewHolder.class), 0);
        assertThat(adapter.lastBoundItem, equalTo(header));
        checkItems(items, 1);

        // Replace header.
        section.setHeader(new Item(0, altHeader));
        adapter.bindViewHolder(mock(RecyclerView.ViewHolder.class), 0);
        assertThat(adapter.lastBoundItem, equalTo(altHeader));
        checkItems(items, 1);

        // Remove header.
        section.removeHeader();
        checkItems(items, 0);

        final Section subsection = adapter.createSection("top_1");
        final ItemData[] subsectionItems = createSectionItems(subsection, 5);

        // Insert subsection without header.
        final int insertPoint = 2;
        section.insert(insertPoint, subsection);
        int offset = checkItems(items, 0, insertPoint, 0);
        offset += checkItems(subsectionItems, offset);
        checkItems(items, insertPoint, offset);

        // Add header to subsection.
        subsection.setHeader(new Item(0, header));
        offset = checkItems(items, 0, insertPoint, 0);
        adapter.bindViewHolder(mock(RecyclerView.ViewHolder.class), insertPoint);
        assertThat(adapter.lastBoundItem, equalTo(header));
        offset += 1;
        offset += checkItems(subsectionItems, offset);
        checkItems(items, insertPoint, offset);

        // Replace header.
        subsection.setHeader(new Item(0, altHeader));
        offset = checkItems(items, 0, insertPoint, 0);
        adapter.bindViewHolder(mock(RecyclerView.ViewHolder.class), insertPoint);
        assertThat(adapter.lastBoundItem, equalTo(altHeader));
        offset += 1;
        offset += checkItems(subsectionItems, offset);
        checkItems(items, insertPoint, offset);

        // Remove header from subsection.
        subsection.removeHeader();
        offset = checkItems(items, 0, insertPoint, 0);
        offset += checkItems(subsectionItems, offset);
        checkItems(items, insertPoint, offset);
    }

    @Test
    public void addingItems() {
        final Section section = adapter.createSection("root");
        final ItemData[] items = createSectionItems(section, 5);

        adapter.addSection(section);

        assertThat(adapter.getItemCount(), equalTo(items.length));
        checkItems(items, 0);

        // Test adding in items after section is in adapter.

        final ItemData middle = ItemData.newInstance();
        final int middlePosition = 2;
        section.insert(middlePosition, new Item(0, middle));
        adapter.bindViewHolder(mock(RecyclerView.ViewHolder.class), middlePosition);
        assertThat(adapter.lastBoundItem, equalTo(middle));

        final ItemData start = ItemData.newInstance();
        final int startPosition = 0;
        section.insert(startPosition, new Item(0, start));
        adapter.bindViewHolder(mock(RecyclerView.ViewHolder.class), startPosition);
        assertThat(adapter.lastBoundItem, equalTo(start));

        final ItemData end = ItemData.newInstance();
        final int endPosition = 7;
        section.insert(endPosition, new Item(0, end));
        adapter.bindViewHolder(mock(RecyclerView.ViewHolder.class), endPosition);
        assertThat(adapter.lastBoundItem, equalTo(end));
    }

    @Test
    public void addingSubsections() {
        final Section section = adapter.createSection("root");
        final ItemData[] sectionItems = createSectionItems(section, 5);

        final Section subsection = adapter.createSection("subsection");
        final ItemData[] subsectionItems = createSectionItems(subsection, 5);

        section.add(subsection);

        final int startSize = sectionItems.length + subsectionItems.length;
        adapter.addSection(section);

        assertThat(adapter.getItemCount(), equalTo(startSize));
        checkItems(sectionItems, 0);
        checkItems(subsectionItems, sectionItems.length);

        // Now test adding in subsections after section is in adapter.

        final Section firstSubgraph = adapter.createSection("first");
        final ItemData[] firstItems = createSectionItems(firstSubgraph, 5);

        final Section middleSubgraph = adapter.createSection("middle");
        final ItemData[] middleItems = createSectionItems(middleSubgraph, 5);

        final Section lastSubgraph = adapter.createSection("last");
        final ItemData[] lastItems = createSectionItems(lastSubgraph, 5);

        // Add section to end of subsection.
        subsection.insert(5, lastSubgraph);
        int addedItems = lastItems.length;
        assertThat(adapter.getItemCount(), equalTo(startSize + addedItems));

        int offset = checkItems(sectionItems, 0);
        offset += checkItems(subsectionItems, offset);
        // Items checked above should not have changed.
        checkItems(lastItems, offset);

        // Add section to middle of subsection.
        subsection.insert(3, middleSubgraph);
        addedItems += middleItems.length;
        assertThat(adapter.getItemCount(), equalTo(startSize + addedItems));

        offset = checkItems(sectionItems, 0);
        offset += checkItems(subsectionItems, 0, 3, offset);
        // Items checked above should not have changed.
        offset += checkItems(middleItems, offset);
        // Items being checked below should not have changed.
        offset += checkItems(subsectionItems, 3, offset);
        checkItems(lastItems, offset);

        // Add section to start of subsection.
        subsection.insert(0, firstSubgraph);
        addedItems += firstItems.length;
        assertThat(adapter.getItemCount(), equalTo(startSize + addedItems));

        offset = checkItems(sectionItems, 0);
        // Items checked above should not have changed.
        offset += checkItems(firstItems, offset);
        // Items being checked below should not have changed.
        offset += checkItems(subsectionItems, 0, 3, offset);
        offset += checkItems(middleItems, offset);
        offset += checkItems(subsectionItems, 3, offset);
        checkItems(lastItems, offset);

        // Add item into a subgraph and use the adapter position lookup to find it.
        final ItemData middle = ItemData.newInstance();
        final int middlePosition = 2;
        middleSubgraph.insert(middlePosition, new Item(0, middle));
        adapter.bindViewHolder(mock(RecyclerView.ViewHolder.class),
                middleSubgraph.getAdapterPositionOfChild(middlePosition));
        assertThat(adapter.lastBoundItem, equalTo(middle));
    }

    @Test
    public void incrementalBuild() {
        final Section section = adapter.createSection("root");
        adapter.addSection(section);

        assertThat(adapter.getItemCount(), equalTo(0));

        final ItemData[] items = createSectionItems(section, 10);
        assertThat(adapter.getItemCount(), equalTo(items.length));
        checkItems(items, 0);

        final Section subsection = adapter.createSection("subsection");
        final int insertPos = 3;
        section.insert(insertPos, subsection);
        assertThat(adapter.getItemCount(), equalTo(items.length));
        checkItems(items, 0);

        final ItemData[] subsectionItems = createSectionItems(subsection, 6);
        assertThat(adapter.getItemCount(), equalTo(items.length + subsectionItems.length));
        int offset = checkItems(items, 0, insertPos, 0);
        offset += checkItems(subsectionItems, offset);
        checkItems(items, insertPos, offset);
    }

    @Test
    public void sectionAndItemRemoval() {
        final ItemData header = ItemData.newInstance();
        final Section section = adapter.createSection("root");
        ItemData[] sItems = createSectionItems(section, 5);

        final Section subsection = adapter.createSection("subsection", new Item(0, header));
        createSectionItems(subsection, 5);
        final Section subsection2 = adapter.createSection("subsection2");
        ItemData[] ss2Items = createSectionItems(subsection2, 5);

        final int insertPos = 3;
        final int insertPos2 = 4;
        section.insert(insertPos2, subsection2);
        section.insert(insertPos, subsection);

        adapter.addSection(section);
        assertThat(section.getItemCount(), equalTo(16));
        assertThat(adapter.getItemCount(), equalTo(16));

        // Remove a subsection.
        section.remove(insertPos);
        assertThat(section.getItemCount(), equalTo(10));
        assertThat(adapter.getItemCount(), equalTo(10));

        int offset = checkItems(sItems, 0, insertPos2, 0);
        offset += checkItems(ss2Items, offset);
        checkItems(sItems, insertPos2, offset);

        // Remove an item.
        subsection2.remove(2);
        assertThat(subsection2.getItemCount(), equalTo(4));
        assertThat(section.getItemCount(), equalTo(9));
        assertThat(adapter.getItemCount(), equalTo(9));
        // Match removal in local item array to make it easy to check.
        ss2Items = new ItemData[]{ss2Items[0], ss2Items[1], ss2Items[3], ss2Items[4]};
        offset = checkItems(sItems, 0, insertPos2, 0);
        offset += checkItems(ss2Items, offset);
        checkItems(sItems, insertPos2, offset);

        // Remove last item to ensure all item mappings were tracked properly.
        section.remove(5);
        assertThat(section.getItemCount(), equalTo(8));
        assertThat(adapter.getItemCount(), equalTo(8));
        // Match removal in local item array to make it easy to check.
        sItems = new ItemData[]{sItems[0], sItems[1], sItems[2], sItems[3]};
        offset = checkItems(sItems, 0, insertPos2, 0);
        offset += checkItems(ss2Items, offset);
        checkItems(sItems, insertPos2, offset);

        adapter.removeSection(0);
        assertThat(adapter.getItemCount(), equalTo(0));
    }

    @Before
    public void setup() {
        adapter = new AdapterStub();
    }

    @Test
    public void updateAnItem() {
        Section section = adapter.createSection("section");
        ItemData update = ItemData.newInstance();
        createSectionItems(section, 5);

        section.set(3, new Item(0, update));
        adapter.addSection(section);
        section.set(3, new Item(0, update));
        adapter.bindViewHolder(mock(RecyclerView.ViewHolder.class), 3);
        assertThat(adapter.lastBoundItem, equalTo(update));
    }

    private int checkItems(ItemData[] items, int offset) {
        return checkItems(items, 0, items.length, offset);
    }

    private int checkItems(ItemData[] items, int start, int offset) {
        return checkItems(items, start, items.length, offset);
    }

    private int checkItems(ItemData[] items, int start, int end, int offset) {
        for (int i = start; i < end; i++) {
            adapter.bindViewHolder(mock(RecyclerView.ViewHolder.class), i + offset - start);
            assertThat(adapter.lastBoundItem, equalTo(items[i]));
        }
        return end - start;
    }

    @NonNull
    private ItemData[] createSectionItems(Section firstSubgraph, int numItems) {
        final ItemData[] firstItems = ItemData.newInstances(numItems);
        for (ItemData item : firstItems) {
            firstSubgraph.add(new Item(0, item));
        }
        return firstItems;
    }

    private void printAdapter(AdapterStub adapter) {
        RecyclerView.ViewHolder mock = mock(RecyclerView.ViewHolder.class);
        System.out.println("Print adapter");
        for (int i = 0, size = adapter.getItemCount(); i < size; i++) {
            adapter.bindViewHolder(mock, i);
            System.out.println("\tpos: " + i + "  item: " + adapter.lastBoundItem);
        }
    }
}