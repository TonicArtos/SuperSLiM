package com.tonicartos.superslim.util;

import com.tonicartos.superslim.GridSLM;
import com.tonicartos.superslim.LayoutManager;
import com.tonicartos.superslim.LinearSLM;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class TestAdapterBuilder {

    private List<Section> mSections = new ArrayList<>();

    public TestAdapterBuilder addGridSection(int itemCount, int itemWidth, int itemHeight,
            Header header) {
        GridSLM.LayoutParams params = new GridSLM.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setSlm(GridSLM.ID);
        params.setNumColumns(3);
        return addSection(itemCount, itemWidth, itemHeight, header, params);
    }

    public TestAdapterBuilder addLinearSection(int itemCount, int itemWidth, int itemHeight,
            Header header) {
        LayoutManager.LayoutParams params = new LayoutManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setSlm(LinearSLM.ID);
        return addSection(itemCount, itemWidth, itemHeight, header, params);
    }

    public TestAdapterBuilder addSection(int itemCount, int itemWidth, int itemHeight,
            Header header, LayoutManager.LayoutParams params) {
        mSections.add(new Section(itemCount, itemWidth, itemHeight, header, params));
        return this;
    }

    public RecyclerView.Adapter build(Context context) {
        SimplestTestAdapter adapter = new SimplestTestAdapter(context);
        int sectionStart = 0;
        for (Section section : mSections) {
            for (int i = 0; i < section.itemCount; i++) {
                SimplestTestAdapter.Item item;
                if (i == 0 && section.header != null) {
                    item = new SimplestTestAdapter.HeaderItem(section.header.flags);
                } else {
                    item = new SimplestTestAdapter.Item();
                }
                item.referenceParams = section.params;
                item.sectionStart = sectionStart;
                item.width = section.itemWidth;
                item.height = section.itemHeight;
                adapter.addItem(item);
            }
            sectionStart += section.itemCount;
        }

        return adapter;
    }

    public static class Header {

        public static int INLINE = 0;

        public static int OVERLAY = 1;

        public static int MARGIN_START = 2;

        public static int MARGIN_END = 4;

        public static int NONSTICKY = 8;

        public final int flags;

        private Header(int flags) {
            this.flags = flags;
        }

        public static Header with(int headerFlags) {
            if ((headerFlags & NONSTICKY) != 0) {
                return new Header(LayoutManager.LayoutParams.HEADER_INLINE);
            }

            int baseFlags = LayoutManager.LayoutParams.HEADER_STICKY;

            if ((headerFlags & OVERLAY) != 0) {
                baseFlags |= LayoutManager.LayoutParams.HEADER_OVERLAY;
                if ((headerFlags & MARGIN_START) != 0) {
                    return new Header(baseFlags | LayoutManager.LayoutParams.HEADER_ALIGN_START);
                } else if ((headerFlags & MARGIN_END) != 0) {
                    return new Header(baseFlags | LayoutManager.LayoutParams.HEADER_ALIGN_END);
                } else {
                    return new Header(baseFlags | LayoutManager.LayoutParams.HEADER_INLINE);
                }
            } else {
                return new Header(baseFlags | LayoutManager.LayoutParams.HEADER_INLINE);
            }
        }
    }

    private static class Section {

        public final int itemCount;

        public final int itemHeight;

        public final Header header;

        public int itemWidth;

        public LayoutManager.LayoutParams params;

        public Section(int itemCount, int itemWidth, int itemHeight, Header hasHeader,
                LayoutManager.LayoutParams params) {
            this.itemCount = itemCount;
            this.itemWidth = itemWidth;
            this.itemHeight = itemHeight;
            this.header = hasHeader;
            this.params = params;
        }
    }
}
