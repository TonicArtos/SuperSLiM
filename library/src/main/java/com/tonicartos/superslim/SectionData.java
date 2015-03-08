package com.tonicartos.superslim;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class SectionData {

    public int firstPosition;

    public int lastPosition;

    public int startMarginWidth;

    public int endMarginWidth;

    public boolean hasHeader;

    public int minimumHeight;

    public String sectionManager;

    public int sectionManagerKind;

    public int headerWidth;

    public int headerHeight;

    public ArrayList<SectionData> subsections;

    private boolean mIsInitialised = false;

    private SectionData(int firstPosition, int lastPosition) {
        this.firstPosition = firstPosition;
        this.lastPosition = lastPosition;
    }

    static ArrayList<SectionData> processSections(List<Integer> sectionStartPositions,
            int lastPosition) {
        if (sectionStartPositions == null || sectionStartPositions.size() == 0) {
            return null;
        }
        ArrayList<SectionData> sections = new ArrayList<>();
        int lastStart = sectionStartPositions.get(0);
        for (int i = 1; i < sectionStartPositions.size(); i++) {
            int nextStart = sectionStartPositions.get(i);
            sections.add(new SectionData(lastStart, nextStart - 1));
            lastStart = nextStart;
        }
        sections.add(new SectionData(lastStart, lastPosition));

        return sections;
    }

    public boolean getIsInitialised() {
        return mIsInitialised;
    }

    public void init(LayoutHelperImpl helper, View first) {
        LayoutManager.LayoutParams sectionParams =
                (LayoutManager.LayoutParams) first.getLayoutParams();

        hasHeader = sectionParams.isHeader();
        if (hasHeader) {
            helper.measureHeader(first);
            headerWidth = helper.getMeasuredWidth(first);
            headerHeight = helper.getMeasuredHeight(first);

            if (!sectionParams.isHeaderInline() || sectionParams.isHeaderOverlay()) {
                minimumHeight = headerHeight;
            } else {
                minimumHeight = 0;
            }
        } else {
            minimumHeight = 0;
            headerHeight = 0;
            headerWidth = 0;
            startMarginWidth = sectionParams.marginStart;
            endMarginWidth = sectionParams.marginEnd;
        }

        if (startMarginWidth == LayoutManager.LayoutParams.MARGIN_AUTO) {
            if (sectionParams.isHeaderStartAligned() && !sectionParams.isHeaderOverlay()) {
                startMarginWidth = headerWidth;
            } else {
                startMarginWidth = 0;
            }
        }

        if (endMarginWidth == LayoutManager.LayoutParams.MARGIN_AUTO) {
            if (sectionParams.isHeaderEndAligned() && !sectionParams.isHeaderOverlay()) {
                endMarginWidth = headerWidth;
            } else {
                endMarginWidth = 0;
            }
        }

        subsections = processSections(sectionParams.getSections(), lastPosition);

        mIsInitialised = true;
    }


    // TODO: insertion, moving, changing, and removal

//    public int itemChanged(int position) {
//        return itemsChanged(position, 1);
//    }
//
//    public int itemInserted(int position) {
//        return itemsInserted(position, 1);
//    }
//
//    public int itemRemoved(int position) {
//        return itemsRemoved(position, 1);
//    }
//
//    /**
//     *
//     * @param position Start position of items changed.
//     * @param count Range of items changed
//     * @return Number of items not yet consumed (after this section).
//     */
//    public int itemsChanged(int position, int count) {
//        int itemsInSection = 0;
//        if (position < firstPosition) {
//            final int itemsAfterFirst = Math.max(0, position + count - firstPosition);
//            itemsInSection = Math.min(itemsAfterFirst, lastPosition - firstPosition + 1);
//            position = firstPosition;
//        } else if (position <= lastPosition) {
//            final int maxItemsInSection = Math.max(0, lastPosition - position + 1);
//            itemsInSection = Math.min(count, maxItemsInSection);
//        }
//
//        int unconsumed = handleItemsChanged(position, itemsInSection);
//
//        for (int i = 0; i < subsections.size() && unconsumed > 0; i++) {
//            SectionData sectionData = subsections.get(i);
//            position = sectionData.firstPosition;
//            unconsumed = sectionData.itemsChanged(position, unconsumed);
//        }
//
//        return unconsumed;
//    }
//
//    public int itemsInserted(int position, int count) {
//        int itemsInSection = 0;
//        int itemsBeforeSection = 0;
//        if (position < firstPosition) {
//            final int itemsAfterFirst = Math.max(0, position + count - firstPosition);
//            itemsBeforeSection = count - itemsAfterFirst;
//            itemsInSection = Math.min(itemsAfterFirst, lastPosition - firstPosition + 1);
//        } else if (position <= lastPosition) {
//            final int maxItemsInSection = Math.max(0, lastPosition - position + 1);
//            itemsInSection = Math.min(count, maxItemsInSection);
//        }
//
//        firstPosition += itemsBeforeSection;
//        lastPosition += itemsBeforeSection + itemsInSection;
//
//        for (SectionData subsection : subsections) {
//            subsection.itemsInserted(position, count);
//        }
//    }
//
//    public int itemsRemoved(int position, int count) {
//        int itemsInSection = 0;
//        int itemsBeforeSection = 0;
//        if (position < firstPosition) {
//            final int itemsAfterFirst = Math.max(0, position + count - firstPosition);
//            itemsBeforeSection = count - itemsAfterFirst;
//            itemsInSection = Math.min(itemsAfterFirst, lastPosition - firstPosition + 1);
//        } else if (position <= lastPosition) {
//            final int maxItemsInSection = Math.max(0, lastPosition - position + 1);
//            itemsInSection = Math.min(count, maxItemsInSection);
//        }
//
//        firstPosition -= itemsBeforeSection;
//        lastPosition -= itemsBeforeSection + itemsInSection;
//
//        for (int i = 0; i < subsections.size(); i++) {
//            SectionData sectionData = subsections.get(i);
//            sectionData.itemsRemoved(position, count);
//            if (sectionData.lastPosition < sectionData.firstPosition) {
//                subsections.remove(i);
//                i--;
//            }
//        }
//    }
}
