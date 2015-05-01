package com.tonicartos.superslim;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public class LinearSLM extends SectionLayoutManager {

    public static final int ID = LayoutManager.SECTION_MANAGER_LINEAR;

    @Override
    public SectionLayoutManager newInstance() {
        return new LinearSLM();
    }

    @Override
    public int onComputeHeaderOffset(int firstVisiblePosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler) {
        /*
         * Work from an assumed overlap and add heights from the start until the overlap is zero or
         * less, or the current position (or max items) is reached.
         */

        int areaAbove = 0;
        for (int position = sectionData.firstPosition + 1;
                areaAbove < sectionData.headerHeight && position < firstVisiblePosition;
                position++) {
            // Look to see if the header overlaps with the displayed area of the mSection.
            View child = recycler.getView(position);
            measureChild(child, helper);

            areaAbove += helper.getMeasuredHeight(child);
            recycler.cacheView(position, child);
        }

        if (areaAbove == sectionData.headerHeight) {
            return 0;
        } else if (areaAbove > sectionData.headerHeight) {
            return 1;
        } else {
            return -areaAbove;
        }
    }

    @Override
    protected int onFillSubsectionsToEnd(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state) {
        final int leadingEdge = helper.getLeadingEdge();
        final int stickyEdge = helper.getStickyEdge();
        int markerLine = 0;
        if (markerLine >= leadingEdge) {
            return markerLine;
        }

        // Find anchor section.
        SectionData anchorSd = null;
        int nextSd = 0;
        for (int i = 0; i < sectionData.subsections.size(); i++) {
            final SectionData sd = sectionData.subsections.get(i);
            if (sd.containsItem(anchorPosition)) {
                anchorSd = sd;
                nextSd = i + 1;
                break;
            }
        }

        if (anchorSd == null) {
            return markerLine;
        }

        final View anchorSectionFirst;
        if (anchorPosition == anchorSd.firstPosition) {
            anchorSectionFirst = recycler.getView(anchorSd.firstPosition);
            recycler.cacheView(anchorSd.firstPosition, anchorSectionFirst);
        } else {
            int subsectionHeaderIndex = Utils
                    .findHeaderIndexFromLastIndex(helper.getChildCount() - 1, anchorSd, helper);
            if (subsectionHeaderIndex == Utils.INVALID_INDEX) {
                anchorSectionFirst = recycler.getView(anchorSd.firstPosition);
                recycler.cacheView(anchorSd.firstPosition, anchorSectionFirst);
            } else {
                anchorSectionFirst = helper.getChildAt(subsectionHeaderIndex);
            }
        }

        LayoutHelper subHelper = helper.getSubsectionLayoutHelper();
        anchorSd.init(subHelper, anchorSectionFirst);
        subHelper.init(anchorSd, markerLine, leadingEdge, stickyEdge);
        SectionLayoutManager slm = helper.getSlm(anchorSd, subHelper);
        if (anchorPosition == anchorSd.firstPosition) {
            markerLine = slm.beginFillToEnd(anchorPosition, anchorSd, subHelper, recycler, state);
        } else {
            markerLine = slm.finishFillToEnd(anchorPosition, anchorSd, subHelper, recycler, state);
        }

        for (int i = nextSd; markerLine < leadingEdge && i < sectionData.subsections.size(); i++) {
            SectionData sd = sectionData.subsections.get(i);
            sd.init(subHelper, recycler);
            subHelper.init(sd, markerLine, leadingEdge, stickyEdge);
            markerLine = slm.beginFillToEnd(sd.firstPosition, sd, subHelper, recycler, state);
        }
        subHelper.recycle();

        return markerLine;
    }

    @Override
    protected int onFillSubsectionsToStart(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state) {
        final int leadingEdge = helper.getLeadingEdge();
        final int stickyEdge = helper.getStickyEdge();
        int markerLine = 0;
        if (markerLine <= leadingEdge) {
            return markerLine;
        }

        // Find anchor section.
        SectionData anchorSd = null;
        int nextSd = 0;
        for (int i = sectionData.subsections.size() - 1; i >= 0; i--) {
            final SectionData sd = sectionData.subsections.get(i);
            if (sd.containsItem(anchorPosition)) {
                anchorSd = sd;
                nextSd = i - 1;
                break;
            }
        }

        if (anchorSd == null) {
            return markerLine;
        }

        final View anchorSectionFirst;
        if (anchorPosition == anchorSd.lastPosition) {
            anchorSectionFirst = recycler.getView(anchorSd.firstPosition);
            recycler.cacheView(anchorSd.firstPosition, anchorSectionFirst);
        } else {
            int subsectionHeaderIndex = Utils.findHeaderIndexFromFirstIndex(0, anchorSd, helper);
            if (subsectionHeaderIndex == Utils.INVALID_INDEX) {
                anchorSectionFirst = recycler.getView(anchorSd.firstPosition);
                recycler.cacheView(anchorSd.firstPosition, anchorSectionFirst);
            } else {
                anchorSectionFirst = helper.getChildAt(subsectionHeaderIndex);
            }
        }

        LayoutHelper subHelper = helper.getSubsectionLayoutHelper();
        anchorSd.init(subHelper, anchorSectionFirst);
        subHelper.init(anchorSd, markerLine, leadingEdge, stickyEdge);
        SectionLayoutManager slm = helper.getSlm(anchorSd, subHelper);
        if (anchorPosition == anchorSd.lastPosition) {
            markerLine = slm.beginFillToStart(anchorPosition, anchorSd, subHelper, recycler, state);
        } else {
            markerLine = slm
                    .finishFillToStart(anchorPosition, anchorSd, subHelper, recycler, state);
        }

        for (int i = nextSd; markerLine > leadingEdge && i >= 0; i--) {
            SectionData sd = sectionData.subsections.get(i);
            sd.init(subHelper, recycler);
            subHelper.init(sd, markerLine, leadingEdge, stickyEdge);
            markerLine = slm.beginFillToStart(sd.lastPosition, sd, subHelper, recycler, state);
        }
        subHelper.recycle();

        return markerLine;
    }

    @Override
    protected int onFillToEnd(int anchorPosition, SectionData sectionData, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        final int itemCount = state.getItemCount();
        final int leadingEdge = helper.getLeadingEdge();
        int markerLine = 0;

        for (int i = anchorPosition; i < itemCount; i++) {
            if (markerLine >= leadingEdge) {
                break;
            }

            View next = recycler.getView(i);
            if (!sectionData.containsItem(helper.getPosition(next))) {
                recycler.cacheView(i, next);
                break;
            }

            measureChild(next, helper);
            markerLine = layoutChild(next, markerLine, LayoutManager.DIRECTION_END, sectionData,
                    helper);
            addView(next, helper, recycler);
        }

        return markerLine;
    }

    @Override
    protected int onFillToStart(int anchorPosition, SectionData sectionData, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        int markerLine = 0;
        final int leadingEdge = helper.getLeadingEdge();
        if (markerLine < leadingEdge) {
            return markerLine;
        }

        // Check to see if we have to adjust for minimum section height. We don't if there is an
        // attached non-header view in this section.
        boolean applyMinHeight = false;
        for (int i = 0; i < state.getItemCount(); i++) {
            View check = helper.getChildAt(0);
            if (check == null) {
                applyMinHeight = false;
                break;
            }

            LayoutManager.LayoutParams checkParams =
                    (LayoutManager.LayoutParams) check.getLayoutParams();
            if (!sectionData.containsItem(checkParams.getViewPosition())) {
                applyMinHeight = true;
                break;
            }

            if (!checkParams.isHeader()) {
                applyMinHeight = false;
                break;
            }
        }

        // Work out offset to marker line by measuring items from the end. If section height is less
        // than min height, then adjust marker line and then lay out items.
        int measuredPositionsMarker = -1;
        int sectionHeight = 0;
        int minHeightOffset = 0;
        if (applyMinHeight) {
            for (int i = anchorPosition; i >= 0; i--) {
                View measure = recycler.getView(i);
                recycler.cacheView(i, measure);
                LayoutManager.LayoutParams params =
                        (LayoutManager.LayoutParams) measure.getLayoutParams();
                if (!sectionData.containsItem(params.getViewPosition())) {
                    break;
                }

                if (params.isHeader()) {
                    continue;
                }

                measureChild(measure, helper);
                sectionHeight += helper.getMeasuredHeight(measure);
                measuredPositionsMarker = i;
                if (sectionHeight >= sectionData.minimumHeight) {
                    break;
                }
            }

            if (sectionHeight < sectionData.minimumHeight) {
                minHeightOffset = sectionHeight - sectionData.minimumHeight;
                markerLine += minHeightOffset;
            }
        }

        for (int i = anchorPosition; i >= 0; i--) {
            if (markerLine - minHeightOffset < leadingEdge) {
                break;
            }

            View next = recycler.getView(i);
            LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) next.getLayoutParams();
            if (params.isHeader()) {
                recycler.cacheView(i, next);
                break;
            }
            if (!sectionData.containsItem(params.getViewPosition())) {
                recycler.cacheView(i, next);
                break;
            }

            if (!applyMinHeight || i < measuredPositionsMarker) {
                measureChild(next, helper);
            } else {
                recycler.decacheView(i);
            }
            markerLine = layoutChild(next, markerLine, LayoutManager.DIRECTION_START, sectionData,
                    helper);
            addView(next, 0, helper, recycler);
        }

        return markerLine;
    }

    private int layoutChild(View child, int markerLine, @LayoutManager.Direction int direction,
            SectionData sd, LayoutHelper helper) {
        final int height = helper.getMeasuredHeight(child);
        final int width = helper.getMeasuredWidth(child);

        int left = 0;
        int right = left + width;
        int top;
        int bottom;

        if (direction == LayoutManager.DIRECTION_END) {
            top = markerLine;
            bottom = top + height;
        } else {
            bottom = markerLine;
            top = bottom - height;
        }
        helper.layoutChild(child, left, top, right, bottom);

        if (direction == LayoutManager.DIRECTION_END) {
            markerLine = helper.getBottom(child);
        } else {
            markerLine = helper.getTop(child);
        }

        return markerLine;
    }

    private void measureChild(View child, LayoutHelper helper) {
        helper.measureChild(child, 0, 0);
    }
}
