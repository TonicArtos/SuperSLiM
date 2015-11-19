package com.tonicartos.superslim;

import com.tonicartos.superslim.adapter.Item;
import com.tonicartos.superslim.adapter.Section;

import org.junit.Before;
import org.junit.Test;

import static com.tonicartos.superslim.Util.ItemData;
import static com.tonicartos.superslim.Util.hasChildCount;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for a section independent from section graph.
 */
public class TestOrphanSection {

    AdapterStub adapter;

    Section section;

    @Test
    public void checkTypeFiltering() {
        Section subsection = adapter.createSection("subsection");
        section.add(subsection);

        assertThat(section.getSubsections().size(), is(1));
        assertThat(section.getItems().size(), is(0));

        section.remove(0);

        Item item = new Item(0, ItemData.newInstance());
        section.add(item);

        assertThat(section.getSubsections().size(), is(0));
        assertThat(section.getItems().size(), is(1));
    }

    @Test
    public void initialState() {
        assertThat(section, hasChildCount(equalTo(0)));
        assertThat(section.getHeader(), is(nullValue()));
    }

    @Before
    public void setup() {
        adapter = new AdapterStub();
        section = adapter.createSection("id");
    }
}
