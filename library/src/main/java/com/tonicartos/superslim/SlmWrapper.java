package com.tonicartos.superslim;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

import java.util.HashMap;

/**
 * SLM wrapper hiding handling of header logic. Used solely by LayoutManager to subtly insert header
 * handling to SLMs.
 */
class SlmWrapper extends SectionLayoutManager {

    private SectionLayoutManager mSlm;

    SlmWrapper(SectionLayoutManager slm) {
        mSlm = slm;
    }

    public int beginFillToEnd(int anchorPosition, SectionData sd, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        int markerLine = 0;
        View header = null;
        if (sd.hasHeader && anchorPosition == sd.firstPosition) {
            header = recycler.getView(sd.firstPosition);
            if (recycler.getCachedView(sd.firstPosition) == null) {
                helper.measureHeader(header);
            }
            markerLine = helper.layoutHeaderTowardsEnd(header, markerLine, state);
            helper.updateVerticalOffset(markerLine);
            anchorPosition += 1;
        }

        if (sd.subsections != null) {
            markerLine = onFillSubsectionsToEnd(anchorPosition, sd, helper, recycler,
                    state);
        } else {
            markerLine = onFillToEnd(anchorPosition, sd, helper, recycler, state);
        }

        if (sd.hasHeader && header != null) {
            addView(header, helper, recycler);
            recycler.decacheView(sd.firstPosition);
        }
        return helper.translateFillResult(markerLine);
    }

    public int beginFillToStart(int anchorPosition, SectionData sd, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        final int countBeforeFill = helper.getChildCount();
        int markerLine;
        if (sd.subsections != null) {
            markerLine = onFillSubsectionsToStart(anchorPosition, sd, helper, recycler,
                    state);
        } else {
            markerLine = onFillToStart(anchorPosition, sd, helper, recycler, state);
        }
        if (sd.hasHeader) {
            View header = recycler.getView(sd.firstPosition);
            if (recycler.getCachedView(sd.firstPosition) == null) {
                helper.measureHeader(header);
            }
            final int offset = getHeaderOffset(sd, helper, recycler, header);
            markerLine = helper.layoutHeaderTowardsStart(header, offset, markerLine, 0, state);

            int attachIndex = helper.getChildCount() - countBeforeFill;
            addView(header, attachIndex, helper, recycler);
            recycler.decacheView(sd.firstPosition);
        }
        return helper.translateFillResult(markerLine);
    }

    public int computeHeaderOffset(int firstVisiblePosition, SectionData sd, LayoutHelper helper,
            Recycler recycler) {
        return mSlm.computeHeaderOffset(firstVisiblePosition, sd, helper, recycler);
    }

    public int finishFillToEnd(int anchorPosition, SectionData sd, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        int countBeforeFill = helper.getChildCount();
        int markerLine = mSlm.onFillToEnd(anchorPosition, sd, helper, recycler, state);
        if (sd.hasHeader) {
            // Shuffle header to end of section (child index). This is the easiest way to ensure
            // the header is drawn after any other section content.
            int headerIndex = countBeforeFill - 1; // Header should always be at the end.
            if (headerIndex != Utils.INVALID_INDEX) {
                View header = helper.getChildAt(headerIndex);
                if (helper.getPosition(header) == sd.firstPosition) {
                    helper.detachView(header);
                    helper.attachView(header);

                    markerLine = Math.max(markerLine, helper.getBottom(header));
                }
            }


        }
        return helper.translateFillResult(markerLine);
    }

    public int finishFillToStart(int anchorPosition, SectionData sd, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        int markerLine = mSlm.onFillToStart(anchorPosition, sd, helper, recycler, state);
        if (sd.hasHeader) {
            final int headerIndex = Utils.findHeaderIndexFromFirstIndex(0, sd, helper);
            final View header;
            if (headerIndex == Utils.INVALID_INDEX) {
                header = recycler.getView(sd.firstPosition);
                if (recycler.getCachedView(sd.firstPosition) == null) {
                    helper.measureHeader(header);
                }
            } else {
                header = helper.getChildAt(headerIndex);
                helper.detachViewAt(headerIndex);
            }

            final int offset = getHeaderOffset(sd, helper, recycler, header);

            final int sectionBottom = mSlm.getLowestEdge(
                    Utils.findLastIndexForSection(sd, helper), helper.getHeight(), sd, helper);

            markerLine = helper
                    .layoutHeaderTowardsStart(header, offset, markerLine, sectionBottom, state);

            // Make sure to attach after section content and to clean up any caching.
            final int attachIndex = Utils.findLastIndexForSection(sd, helper) + 1;
            if (headerIndex == Utils.INVALID_INDEX) {
                helper.addView(header, attachIndex);
            } else {
                helper.attachView(header, attachIndex);
            }
            recycler.decacheView(sd.firstPosition);
            sd.setTempHeaderIndex(attachIndex);
        }
        sd.recentlyFinishFilledToStart = true;
        return helper.translateFillResult(markerLine);
    }

    public RecyclerView.LayoutParams generateLayoutParams(LayoutManager.LayoutParams params) {
        return mSlm.generateLayoutParams(params);
    }

    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return mSlm.generateLayoutParams(c, attrs);
    }

    public void getEdgeStates(Rect outRect, View child, SectionData sd, int layoutDirection) {
        mSlm.getEdgeStates(outRect, child, sd, layoutDirection);
    }

    public int getHighestEdge(int firstIndex, int defaultEdge, SectionData sd,
            LayoutQueryHelper helper) {
        return mSlm.getHighestEdge(firstIndex, defaultEdge, sd, helper);
    }

    public int getLowestEdge(int lastIndex, int defaultEdge, SectionData sd,
            LayoutQueryHelper helper) {
        return mSlm.getLowestEdge(lastIndex, defaultEdge, sd, helper);
    }

    public int howManyMissingAbove(int firstPosition, SparseArray<Boolean> positionsOffscreen) {
        return mSlm.howManyMissingAbove(firstPosition, positionsOffscreen);
    }

    public int howManyMissingBelow(int lastPosition, SparseArray<Boolean> positionsOffscreen) {
        return mSlm.howManyMissingBelow(lastPosition, positionsOffscreen);
    }

    public SlmWrapper init(SectionData sectionData, LayoutQueryHelper helper) {
        mSlm.init(sectionData, helper);
        return this;
    }

    @Override
    public void onPreTrimAtEndEdge(int lvi, SectionData sd, LayoutTrimHelper helper) {
        if (sd.subsections == null) {
            return;
        }
        HashMap<SectionData, Integer> selectedSubsections =
                getSectionsIntersectingEndEdge(helper.getTrimEdge(), lvi, sd, helper);

        for (SectionData subSd : selectedSubsections.keySet()) {
            LayoutTrimHelper subsectionHelper = helper.getSubsectionLayoutTrimHelper();
            subsectionHelper.init(subSd, helper.getTrimEdge(), helper.getStickyEdge());
            helper.getSlm(subSd, subsectionHelper).
                    onPreTrimAtEndEdge(selectedSubsections.get(subSd), subSd, subsectionHelper);
            subsectionHelper.recycle();
        }
    }

    @Override
    public void onPreTrimAtStartEdge(int firstVisibleIndex, SectionData sd,
            LayoutTrimHelper helper) {
        mSlm.onPreTrimAtStartEdge(firstVisibleIndex, sd, helper);
        int subsectionStickyEdge = updateHeaderForStartEdgeTrim(firstVisibleIndex, sd, helper);

        if (sd.subsections == null) {
            return;
        }
        HashMap<SectionData, Integer> selectedSubsections =
                getSectionsIntersectingStartEdge(subsectionStickyEdge, firstVisibleIndex, sd,
                        helper);

        for (SectionData subSd : selectedSubsections.keySet()) {
            LayoutTrimHelper subsectionHelper = helper.getSubsectionLayoutTrimHelper();
            subsectionHelper.init(subSd, helper.getTrimEdge(), subsectionStickyEdge);
            helper.getSlm(subSd, subsectionHelper).
                    onPreTrimAtStartEdge(selectedSubsections.get(subSd), subSd, subsectionHelper);
            subsectionHelper.recycle();
        }
    }

    /**
     * Get a map of sections and their first visible positions that intersect the start edge.
     *
     * <p>The basic implementation looks through all attached child views for this section. You
     * should consider an implementation that constrains the search to a minimal range.</p>
     *
     * @param startEdge         Edge line. Generally 0.
     * @param firstVisibleIndex First visible index for this section.
     * @param sd                Section data.
     * @param helper            Layout query helper.
     * @return Map of subsection data to subsection first visible edges.
     */
    protected HashMap<SectionData, Integer> getSectionsIntersectingStartEdge(int startEdge,
            int firstVisibleIndex, SectionData sd, LayoutQueryHelper helper) {
        // Work out max number of items we have to check to find sections which intersect start
        // edge. Also, cap to  number of items after fvi.
        int range = Math.min(sd.lastPosition
                        - helper.getPosition(helper.getChildAt(firstVisibleIndex)) + 1,
                helper.getChildCount() - firstVisibleIndex);

        // Select subsections which have items overlapping or before the start edge.
        HashMap<SectionData, Integer> selectedSubsections = new HashMap<>();
        for (int i = 0; i < range; i++) {
            int childIndex = i + firstVisibleIndex;
            View child = helper.getChildAt(childIndex);
            if (helper.getTop(child) < startEdge) {
                int childPosition = helper.getPosition(child);
                for (SectionData subSd : sd.subsections) {
                    if (selectedSubsections.get(subSd) == null && subSd
                            .containsItem(childPosition)) {
                        int subsectionFvi = Utils
                                .findFirstVisibleIndex(startEdge, childIndex, subSd,
                                        helper);
                        if (subsectionFvi != Utils.INVALID_INDEX) {
                            selectedSubsections.put(subSd, subsectionFvi);
                        }
                        break;
                    }
                }
            }

            if (selectedSubsections.size() == sd.subsections.size()) {
                // Already added every section.
                break;
            }
        }
        return selectedSubsections;
    }

    @Override
    protected int onFillSubsectionsToEnd(int anchorPosition, SectionData sd, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        return mSlm.onFillSubsectionsToEnd(anchorPosition, sd, helper, recycler, state);
    }

    @Override
    protected int onFillSubsectionsToStart(int anchorPosition, SectionData sd, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        return mSlm.onFillSubsectionsToStart(anchorPosition, sd, helper, recycler, state);
    }

    protected int onFillToEnd(int anchorPosition, SectionData sd, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        return mSlm.onFillToEnd(anchorPosition, sd, helper, recycler, state);
    }

    protected int onFillToStart(int anchorPosition, SectionData sd, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        return mSlm.onFillToStart(anchorPosition, sd, helper, recycler, state);
    }

    @Override
    void onPostFinishFillToStart(SectionData sd, LayoutTrimHelper helper) {
        final int headerIndex = sd.getTempHeaderIndex();
        sd.clearTempHeaderIndex();
        final int subsectionStickyEdge = updateHeader(headerIndex, sd, helper);

        if (sd.subsections == null) {
            return;
        }

        for (SectionData subSd : sd.subsections) {
            if (sd.recentlyFinishFilledToStart) {
                sd.recentlyFinishFilledToStart = false;
                LayoutTrimHelper subsectionHelper = helper.getSubsectionLayoutTrimHelper();
                subsectionHelper.init(subSd, helper.getTrimEdge(), subsectionStickyEdge);
                helper.getSlm(subSd, subsectionHelper)
                        .onPostFinishFillToStart(sd, subsectionHelper);
                subsectionHelper.recycle();
            }
        }
    }

    private int getHeaderOffset(SectionData sd, LayoutHelper helper, Recycler recycler,
            View header) {
        final int offset;
        final LayoutManager.LayoutParams layoutParams =
                (LayoutManager.LayoutParams) header.getLayoutParams();
        if (!layoutParams.isHeaderSticky() || !layoutParams.isHeaderInline()) {
            offset = mSlm.computeHeaderOffset(0, sd, helper, recycler);
        } else {
            offset = 0;
        }
        return offset;
    }

    private HashMap<SectionData, Integer> getSectionsIntersectingEndEdge(int endEdge,
            int lastVisibleIndex, SectionData sd, LayoutQueryHelper helper) {
        // Work out max number of items we have to check to find sections which intersect start
        // edge. Also, cap to  number of items after fvi.
        int range = Math.min(helper.getPosition(helper.getChildAt(lastVisibleIndex)) -
                sd.firstPosition + 1, lastVisibleIndex + 1);

        // Select subsections which have items overlapping or before the edge.
        HashMap<SectionData, Integer> selectedSubsections = new HashMap<>();
        for (int i = 0; i < range; i++) {
            int childIndex = lastVisibleIndex - i;
            View child = helper.getChildAt(childIndex);
            if (endEdge < helper.getBottom(child)) {
                int childPosition = helper.getPosition(child);
                for (SectionData subSd : sd.subsections) {
                    if (selectedSubsections.get(subSd) == null && subSd
                            .containsItem(childPosition)) {
                        int subsectionLvi = Utils
                                .findLastVisibleIndex(endEdge, childIndex, subSd,
                                        helper);
                        if (subsectionLvi != Utils.INVALID_INDEX) {
                            selectedSubsections.put(subSd, subsectionLvi);
                        }
                        break;
                    }
                }
            }

            if (selectedSubsections.size() == sd.subsections.size()) {
                // Already added every section.
                break;
            }
        }
        return selectedSubsections;
    }

    private int updateHeader(int headerIndex, SectionData sd, LayoutQueryHelper helper) {
        final int stickyEdge = helper.getStickyEdge();
        if (headerIndex == Utils.INVALID_INDEX) {
            // No header found to update. It must not need to be updated.
            return stickyEdge;
        }

        final View header = helper.getChildAt(headerIndex);
        final LayoutManager.LayoutParams headerParams =
                (LayoutManager.LayoutParams) header.getLayoutParams();
        if (!headerParams.isHeaderSticky()) {
            // Only need to update stickied headers.
            return stickyEdge;
        }

        final int headerTop = helper.getTop(header);
        if (headerTop >= stickyEdge) {
            // Only need to update sticky headers if they are above the sticky edge.
            return stickyEdge;
        }

        SectionLayoutManager slm = helper.getSlm(sd, helper);
        final int sectionBottom = slm.getLowestEdge(
                headerIndex, helper.getBottom(header), sd, helper);

        final int headerHeight = helper.getMeasuredHeight(header);
        int top = headerHeight + stickyEdge > sectionBottom ?
                sectionBottom - headerHeight : stickyEdge;

        int delta = headerTop - top;
        header.offsetTopAndBottom(-delta);

        if (headerParams.isHeaderInline()) {
            return stickyEdge - delta + headerHeight;
        }
        return stickyEdge;
    }

    private int updateHeaderForStartEdgeTrim(int fvi, SectionData sd, LayoutQueryHelper helper) {
        if (!sd.hasHeader) {
            return helper.getStickyEdge();
        }

        int headerIndex = Utils.findHeaderIndexFromFirstIndex(fvi, sd, helper);
        return updateHeader(headerIndex, sd, helper);
    }
}
