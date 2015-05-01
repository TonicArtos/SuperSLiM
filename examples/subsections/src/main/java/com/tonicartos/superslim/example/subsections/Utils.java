package com.tonicartos.superslim.example.subsections;

import com.tonicartos.superslim.GridSLM;
import com.tonicartos.superslim.LinearSLM;
import com.tonicartos.superslim.SectionAdapter;
import com.tonicartos.superslim.SectionLayoutManager;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import java.util.ArrayList;

public class Utils {

    public static final int START_ALIGNED_HEADER = 0x01;

    public static final int END_ALIGNED_HEADER = 0x02;

    public static final int INLINE_HEADER = 0x03;

    public static final int CONTENT = 0x00;

    private static final int RED = Color.parseColor("#F44336");

    private static final int PINK = Color.parseColor("#E91E63");

    private static final int PURPLE = Color.parseColor("#9C27B0");

    private static final int DEEP_PURPLE = Color.parseColor("#673AB7");

    private static final int INDIGO = Color.parseColor("#3F51B5");

    private static final int BLUE = Color.parseColor("#2196F3");

    private static final int LIGHT_BLUE = Color.parseColor("#03A9F4");

    private static final int CYAN = Color.parseColor("#00BCD4");

    private static final int TEAL = Color.parseColor("#009688");

    private static final int GREEN = Color.parseColor("#4CAF50");

    private static final int LIGHT_GREEN = Color.parseColor("#8BC34A");

    private static final int LIME = Color.parseColor("#CDDC39");

    private static final int YELLOW = Color.parseColor("#FFEB3B");

    private static final int AMBER = Color.parseColor("#FFC107");

    private static final int ORANGE = Color.parseColor("#FF9800");

    private static final int DEEP_ORANGE = Color.parseColor("#FF5722");

    private static final int BROWN = Color.parseColor("#795548");

    private static final int GREY = Color.parseColor("#757575");

    private static final int BLUE_GREY = Color.parseColor("#607D8B");

    public static Section createData(Context context) {
        Section root = new Section();

        Section.Builder b = new Section.Builder();
        final int marginStart =
                context.getResources().getDimensionPixelSize(R.dimen.default_section_marginStart);

        b.setKind(LinearSLM.ID)
                .setColumnWidth(context.getResources()
                        .getDimensionPixelSize(R.dimen.grid_column_width));
        int itemCount = 0;
        for (int i = 0; i < 20; i++) {
            Section section = createSection(b, marginStart, i);
            section.setFirstPosition(itemCount);
            itemCount += section.getCount();
            root.addSubsection(section);
        }

        return root;
    }

    public static Section createGrid(Context context) {
        Section root = new Section();
        final int marginStart =
                context.getResources().getDimensionPixelSize(R.dimen.default_section_marginStart);
        int itemCount = 0;

        Section.Builder plainGrid = new Section.Builder()
                .setKind(GridSLM.ID)
                .setColumnWidth(context.getResources()
                        .getDimensionPixelSize(R.dimen.grid_column_width))
                .setHeader(new Item(PURPLE, INLINE_HEADER));
        Section s = plainGrid.build();
        addItems(s, 10, itemCount);
        s.setFirstPosition(0);
        itemCount = s.getCount();
        root.addSubsection(s);

        {
            Section.Builder nestedListsInGrid = new Section.Builder()
                    .setKind(GridSLM.ID)
                    .setNumColumns(2)
                    .setHeader(new Item(INDIGO, INLINE_HEADER));
            Section n = nestedListsInGrid.build();
            int start = itemCount;
            itemCount += n.getCount();

            Section nested = plainGrid.build();
            addItems(nested, 0, itemCount);
            nested.setFirstPosition(itemCount);
            itemCount += nested.getCount();
            n.addSubsection(nested);

            nested = plainGrid.build();
            addItems(nested, 1, itemCount);
            nested.setFirstPosition(itemCount);
            itemCount += nested.getCount();
            n.addSubsection(nested);

            nested = plainGrid.build();
            addItems(nested, 2, itemCount);
            nested.setFirstPosition(itemCount);
            itemCount += nested.getCount();
            n.addSubsection(nested);

            nested = plainGrid.build();
            addItems(nested, 3, itemCount);
            nested.setFirstPosition(itemCount);
            itemCount += nested.getCount();
            n.addSubsection(nested);

            n.setFirstPosition(start);
            root.addSubsection(n);
        }

        s = plainGrid.build();
        addItems(s, 0, itemCount);
        s.setFirstPosition(itemCount);
        itemCount += s.getCount();
        root.addSubsection(s);

        s = plainGrid.build();
        addItems(s, 1, itemCount);
        s.setFirstPosition(itemCount);
        itemCount += s.getCount();
        root.addSubsection(s);

        s = plainGrid.build();
        addItems(s, 2, itemCount);
        s.setFirstPosition(itemCount);
        itemCount += s.getCount();
        root.addSubsection(s);

        s = plainGrid.build();
        addItems(s, 3, itemCount);
        s.setFirstPosition(itemCount);
        itemCount += s.getCount();
        root.addSubsection(s);

        s = plainGrid.build();
        addItems(s, 4, itemCount);
        s.setFirstPosition(itemCount);
        itemCount += s.getCount();
        root.addSubsection(s);

        s = plainGrid.build();
        addItems(s, 5, itemCount);
        s.setFirstPosition(itemCount);
        itemCount += s.getCount();
        root.addSubsection(s);

        s = plainGrid.build();
        addItems(s, 6, itemCount);
        s.setFirstPosition(itemCount);
        itemCount += s.getCount();
        root.addSubsection(s);

        Log.d("create grid", root.toString());

        return root;
    }

    private static void addItems(Section section, int numItems, int numBefore) {
        for (int i = 0; i < numItems; i++) {
            section.addItem(new Item(getColor(i + numBefore)));
        }
    }

    private static void addSubsections(Section section, int seed, Section.Builder b,
            boolean forceItems) {
        final int numSubsections = (seed * 3 + seed) % 7 + 1;
        int numAdded = 0;
        for (int i = 0; i < numSubsections; i++) {
            final int subsectionSeed = (i * 3 + seed) % 11 + 1;
            b.setHeader(new Item(getColor(subsectionSeed), INLINE_HEADER))
                    .setKind(subsectionSeed % 2 == 0 ? LinearSLM.ID : GridSLM.ID);

            final Section subsection = b.build();
            if (forceItems || subsectionSeed > 6) {
                addItems(subsection, subsectionSeed, numAdded);
            } else {
                addSubsections(subsection, subsectionSeed, b, true);
            }
            section.addSubsection(subsection);

            numAdded += subsectionSeed;
        }
    }

    private static Section createSection(Section.Builder b, int marginStart, int i) {
        b.setMarginStart(marginStart)
                .setHeader(new Item(getColor(i), START_ALIGNED_HEADER))
                .setKind(LinearSLM.ID);
        final Section section = b.build();
        b.setMarginStart(0);
        final int seed = (i * 3 + i) % 41 + 1;
        if (5 < seed && seed < 13) {
            addItems(section, seed, i);
        } else {
            addSubsections(section, i, b, false);
        }
        return section;
    }

    private static int getColor(int i) {
        switch ((i * 3 + i) % 19) {
            case 0:
                return RED;
            case 1:
                return PINK;
            case 2:
                return PURPLE;
            case 3:
                return DEEP_PURPLE;
            case 4:
                return INDIGO;
            case 5:
                return BLUE;
            case 6:
                return LIGHT_BLUE;
            case 7:
                return CYAN;
            case 8:
                return TEAL;
            case 9:
                return GREEN;
            case 10:
                return LIGHT_GREEN;
            case 11:
                return LIME;
            case 12:
                return YELLOW;
            case 13:
                return AMBER;
            case 14:
                return ORANGE;
            case 15:
                return DEEP_ORANGE;
            case 16:
                return BROWN;
            case 17:
                return GREY;
            case 18:
            default:
                return BLUE_GREY;
        }
    }

    public static class Section extends SectionAdapter.Section<Section> {

        private int mMarginEnd;

        private int mMarginStart;

        private Item mHeader;

        private ArrayList<Item> mItems = new ArrayList<>();

        private int mCount;

        private int mKind;

        private int mColumnWidth;

        private int mNumColumns;

        public void addItem(Item item) {
            mItems.add(item);
            mCount += 1;
        }

        public void addSubsection(Section subsection) {
            mSubsections.add(subsection);
            mCount += subsection.getCount();
        }

        public int getCount() {
            return mCount;
        }

        public Item getItem(int position) {
            if (mHeader != null) {
                if (mStart == position) {
                    return mHeader;
                }
            }

            final int itemIndex = position - mStart - (mHeader == null ? 0 : 1);
            if (itemIndex < 0) {
                return null;
            }

            if (mSubsections.size() > 0) {
                for (Section sub : mSubsections) {
                    Item item = sub.getItem(position);
                    if (item != null) {
                        return item;
                    }
                }
            }

            if (itemIndex < mItems.size()) {
                return mItems.get(itemIndex);
            }

            return null;
        }

        @Override
        public SectionLayoutManager.SlmConfig getSlmConfig() {
            switch (mKind) {
                case GridSLM.ID:
                    GridSLM.SlmConfig c = new GridSLM.SlmConfig(mMarginStart, mMarginEnd, mKind);
                    c.setColumnWidth(mColumnWidth);
                    c.setNumColumns(mNumColumns);
                    return c;
                default:
                case LinearSLM.ID:
                    return new SectionLayoutManager.SlmConfig(mMarginStart, mMarginEnd, mKind);
            }
        }

        public void setColumnWidth(int width) {
            mColumnWidth = width;
        }

        public void setFirstPosition(int position) {
            mStart = position;
            mEnd = position + mCount - 1;
            int offset = mHeader == null ? 0 : 1;
            for (Section sub : mSubsections) {
                sub.setFirstPosition(position + offset);
                offset += sub.getCount();
            }
        }

        public void setHeader(Item item) {
            mHeader = item;
            mCount += 1;
        }

        public void setKind(int kind) {
            mKind = kind;
        }

        public void setMarginEnd(int margin) {
            mMarginEnd = margin;
        }

        public void setMarginStart(int margin) {
            mMarginStart = margin;
        }

        public void setNumColumns(int numColumns) {
            mNumColumns = numColumns;
        }

        @Override
        public String toString() {
            String out = "kind: " + mKind + " start: " + mStart + " end: " + mEnd + " count: " + mCount + " subsections: "
                    + mSubsections.size();
            for (Section sub : mSubsections) {
                out += "\n" + sub.toString();
            }
            return out.replaceAll("\\n", "\n    ");
        }

        public static class Builder {

            private int mKind;

            private Item mHeader;

            private int mMarginEnd;

            private int mMarginStart;

            private int mColumnWidth;

            private int mNumColumns = -1;

            public Section build() {
                Section s = new Section();
                s.setHeader(mHeader);
                s.setColumnWidth(mColumnWidth);
                s.setNumColumns(mNumColumns);
                s.setMarginStart(mMarginStart);
                s.setMarginEnd(mMarginEnd);
                s.setKind(mKind);
                return s;
            }

            public Builder setColumnWidth(int width) {
                mColumnWidth = width;
                return this;
            }

            public Builder setHeader(Item header) {
                mHeader = header;
                return this;
            }

            public Builder setKind(int kind) {
                mKind = kind;
                return this;
            }

            public Builder setMarginEnd(int margin) {
                mMarginEnd = margin;
                return this;
            }

            public Builder setMarginStart(int margin) {
                mMarginStart = margin;
                return this;
            }

            public Builder setNumColumns(int numColumns) {
                mNumColumns = numColumns;
                return this;
            }
        }
    }

    public static class Item {

        public int itemKind;

        public int background;

        public Item(int color) {
            background = color;
            itemKind = CONTENT;
        }

        public Item(int color, int kind) {
            background = color;
            itemKind = kind;
        }
    }
}
