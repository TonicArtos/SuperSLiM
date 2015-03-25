package com.tonicartos.superslim;

import android.support.v7.widget.RecyclerView;
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

    public SectionLayoutManager.SlmConfig slmConfig;

    boolean recentlyFinishFilledToStart = false;

    private boolean mIsInitialised = false;

    private LayoutManager.LayoutParams mSectionParams;

    private int mTempHeaderIndex;

    private SectionData(int firstPosition, int lastPosition) {
        this.firstPosition = firstPosition;
        this.lastPosition = lastPosition;
    }

    static ArrayList<SectionData> processSectionGraph(int lastPosition,
            List<? extends SectionAdapter.Section> sections) {
        if (sections == null || sections.size() == 0) {
            return null;
        }

        ArrayList<SectionData> sectionData = new ArrayList<>();
        for (int i = sections.size() - 1; i >= 0; i--) {
            SectionAdapter.Section s = sections.get(i);
            if (s.end != SectionAdapter.Section.NO_POSITION) {
                // Doing this will allow intermingling subsections and items.
                lastPosition = s.end;
            }

            SectionData sd = new SectionData(s.start, lastPosition);
            sd.subsections = processSectionGraph(sd.lastPosition, s.getSubsections());
            sd.slmConfig = s.getSlmConfig();
            sectionData.add(0, sd);

            // Prime for next iteration.
            lastPosition = sd.firstPosition - 1;
        }

        return sectionData;
    }

    public void clearTempHeaderIndex() {
        mTempHeaderIndex = -1;
    }

    public boolean containsItem(int viewPosition) {
        return firstPosition <= viewPosition && viewPosition <= lastPosition;
    }

    public boolean containsItem(View child) {
        return containsItem(
                ((RecyclerView.LayoutParams) child.getLayoutParams()).getViewPosition());
    }

    public boolean isInitialised() {
        return mIsInitialised;
    }

    public LayoutManager.LayoutParams getSectionParams() {
        return mSectionParams;
    }

    public void init(LayoutHelper helper, View first) {
        init(helper, first, false);
    }

    public void init(LayoutHelper helper, View first, boolean forceInitialisation) {
        if (mIsInitialised && !forceInitialisation) {
            return;
        }

        mSectionParams = (LayoutManager.LayoutParams) first.getLayoutParams();

        if (slmConfig == null) {
            sectionManagerKind = mSectionParams.sectionManagerKind;
            sectionManager = mSectionParams.sectionManager;
            startMarginWidth = mSectionParams.marginStart;
            endMarginWidth = mSectionParams.marginEnd;
        } else {
            sectionManagerKind = slmConfig.sectionManagerKind;
            sectionManager = slmConfig.sectionManager;
            startMarginWidth = slmConfig.marginStart;
            endMarginWidth = slmConfig.marginEnd;
        }

        hasHeader = mSectionParams.isHeader();
        if (hasHeader) {
            helper.measureHeader(first);
            headerWidth = helper.getMeasuredWidth(first);
            headerHeight = helper.getMeasuredHeight(first);

            if (!mSectionParams.isHeaderInline() || mSectionParams.isHeaderOverlay()) {
                minimumHeight = headerHeight;
            } else {
                minimumHeight = 0;
            }
        } else {
            minimumHeight = 0;
            headerHeight = 0;
            headerWidth = 0;
        }

        if (startMarginWidth == LayoutManager.LayoutParams.MARGIN_AUTO) {
            if (mSectionParams.isHeaderStartAligned() && !mSectionParams.isHeaderOverlay()) {
                startMarginWidth = headerWidth;
            } else {
                startMarginWidth = 0;
            }
        }

        if (endMarginWidth == LayoutManager.LayoutParams.MARGIN_AUTO) {
            if (mSectionParams.isHeaderEndAligned() && !mSectionParams.isHeaderOverlay()) {
                endMarginWidth = headerWidth;
            } else {
                endMarginWidth = 0;
            }
        }

        // Check subsection sanity.
        if (subsections != null) {
            int firstSubsectionPosition = subsections.get(0).firstPosition;
            if (hasHeader && firstSubsectionPosition != firstPosition + 1) {
                throw new SubsectionValidationRuntimeException(
                        "Subsection content should start at first item after header.");
            } else if (firstSubsectionPosition != firstPosition) {
                throw new SubsectionValidationRuntimeException(
                        "Subsection content should start at first item.");
            }
        }

        mIsInitialised = true;
    }

    int getTempHeaderIndex() {
        return mTempHeaderIndex;
    }

    void setTempHeaderIndex(int headerIndex) {
        mTempHeaderIndex = headerIndex;
    }

    static class SubsectionValidationRuntimeException extends RuntimeException {

        SubsectionValidationRuntimeException(String error) {
            super(error);
        }
    }

    public void updateInitStatus(int position, int range) {
        if (slmConfig == null) {
            if (position <= firstPosition && firstPosition < position + range) {
                mIsInitialised = false;
            }
        }

        if (subsections != null) {
            for (SectionData subSd : subsections) {
                subSd.updateInitStatus(position, range);
            }
        }
    }

    // TODO: insertion, moving, changing, and removal
//
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
//
//        return count - (itemsBeforeSection + itemsInSection);
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
