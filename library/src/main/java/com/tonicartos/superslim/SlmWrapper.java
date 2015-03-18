package com.tonicartos.superslim;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * SLM wrapper hiding handling of header logic. Used solely by LayoutManager to subtly insert header
 * handling to SLMs.
 */
class SlmWrapper extends SectionLayoutManager {

    public static final int INVALID_INDEX = -1;

    private SectionLayoutManager mSlm;

    SlmWrapper(SectionLayoutManager slm) {
        mSlm = slm;
    }

    public int beginFillToEnd(int anchorPosition, SectionData sd, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        int markerLine = 0;
        if (sd.hasHeader) {
            // Markerline gets shifted here if the header is inline.
            // TODO: Layout header.
        }
        helper.updateVerticalOffset(markerLine);
        int result;
        if (sd.subsections != null) {
            result = onFillSubsectionsToEnd(anchorPosition, sd, helper, recycler,
                    state);
        } else {
            result = onFillToEnd(anchorPosition, sd, helper, recycler, state);
        }
        if (sd.hasHeader) {
            // TODO: Attach header.
        }
        return result;
    }

    public int beginFillToStart(int anchorPosition, SectionData sd, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        int result;
        if (sd.subsections != null) {
            result = onFillSubsectionsToStart(anchorPosition, sd, helper, recycler,
                    state);
        } else {
            result = onFillToStart(anchorPosition, sd, helper, recycler, state);
        }
        if (sd.hasHeader) {
            // TODO: Layout and attach header if needed.
        }
        return result;
    }

    public int computeHeaderOffset(int firstVisiblePosition, SectionData sd, LayoutHelper helper,
            Recycler recycler) {
        return mSlm.computeHeaderOffset(firstVisiblePosition, sd, helper, recycler);
    }

    public int finishFillToEnd(int anchorPosition, SectionData sd, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        int markerLine = mSlm.onFillToEnd(anchorPosition, sd, helper, recycler, state);
        if (sd.hasHeader) {
            // Shuffle header to end of section (child index). This is the easiest way to ensure
            // the header is drawn after any other section content.
            int headerIndex = findHeaderIndexFromLastIndex(helper.getChildCount() - 1, sd, helper);
            if (headerIndex != INVALID_INDEX) {
                View header = helper.getChildAt(headerIndex);
                helper.detachView(header);
                helper.attachView(header);

                return Math.max(markerLine, helper.getBottom(header));
            }
        }
        return markerLine;
    }

    public int finishFillToStart(int anchorPosition, SectionData sd, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        int markerLine = mSlm.onFillToStart(anchorPosition, sd, helper, recycler, state);
        if (sd.hasHeader) {
            final int headerIndex = findHeaderIndexFromFirstIndex(0, sd, helper);
            final View header;
            if (headerIndex == INVALID_INDEX) {
                header = recycler.getView(sd.firstPosition);
            } else {
                header = helper.getChildAt(headerIndex);
                helper.detachViewAt(headerIndex);
            }

            final int offset;
            if (needHeaderOffset(header)) {
                offset = mSlm.computeHeaderOffset(0, sd, helper, recycler);
            } else {
                offset = 0;
            }

            final int sectionBottom = mSlm.getLowestEdge(
                    findLastIndexForSection(sd, helper), helper.getHeight(), sd, helper);

            markerLine = helper
                    .layoutHeaderTowardsStart(header, offset, markerLine, sectionBottom, state);

            // Make sure to attach after section content and to clean up any caching.
            final int attachIndex = findLastIndexForSection(sd, helper) + 1;
            if (headerIndex == INVALID_INDEX) {
                helper.addView(header, attachIndex);
            } else {
                helper.attachView(header, attachIndex);
            }
            recycler.decacheView(sd.firstPosition);
            sd.setTempHeaderIndex(attachIndex);
        }
        sd.recentlyFinishFilledToStart = true;
        return markerLine;
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

    public SlmWrapper init(SectionData sd, LayoutQueryHelper helper) {
        mSlm.init(sd, helper);
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
            helper.getSlm(subSd).
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
            helper.getSlm(subSd).
                    onPreTrimAtStartEdge(selectedSubsections.get(subSd), subSd, subsectionHelper);
            subsectionHelper.recycle();
        }
    }

    protected HashMap<SectionData, Integer> getSectionsIntersectingEndEdge(int endEdge,
            int lastVisibleIndex, SectionData sd, LayoutQueryHelper helper) {
        // Work out max number of items we have to check to find sections which intersect start
        // edge. Also, cap to  number of items after fvi.
        int range = Math.min(helper.getPosition(helper.getChildAt(lastVisibleIndex)) -
                sd.firstPosition + 1, lastVisibleIndex + 1);

        // Select subsections which have items overlapping or before the start edge.
        HashMap<SectionData, Integer> selectedSubsections = new HashMap<>();
        for (int i = 0; i < range; i++) {
            int childIndex = lastVisibleIndex - i;
            View child = helper.getChildAt(childIndex);
            if (endEdge < helper.getBottom(child)) {
                int childPosition = helper.getPosition(child);
                for (SectionData subSd : sd.subsections) {
                    if (selectedSubsections.get(subSd) == null && subSd
                            .containsItem(childPosition)) {
                        int subsectionLvi = findLastVisibleIndex(endEdge, childIndex, subSd,
                                helper);
                        if (subsectionLvi != INVALID_INDEX) {
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
                        int subsectionFvi = findFirstVisibleIndex(startEdge, childIndex, subSd,
                                helper);
                        if (subsectionFvi != INVALID_INDEX) {
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
                helper.getSlm(subSd).onPostFinishFillToStart(sd, subsectionHelper);
                subsectionHelper.recycle();
            }
        }
    }

    private int binarySearchForLastPosition(int min, int max, SectionData sd,
            LayoutQueryHelper helper) {
        if (max < min) {
            return INVALID_INDEX;
        }

        final int count = helper.getChildCount();

        int mid = min + (max - min) / 2;

        View candidate = helper.getChildAt(mid);
        BaseLayoutManager.LayoutParams params = (BaseLayoutManager.LayoutParams) candidate
                .getLayoutParams();
        int candidatePosition = params.getViewPosition();
        if (candidatePosition < sd.firstPosition) {
            return binarySearchForLastPosition(mid + 1, max, sd, helper);
        }

        if (candidatePosition > sd.lastPosition || params.isHeader()) {
            return binarySearchForLastPosition(min, mid - 1, sd, helper);
        }

        if (mid == count - 1) {
            return mid;
        }

        View next = helper.getChildAt(mid + 1);
        BaseLayoutManager.LayoutParams lp = (BaseLayoutManager.LayoutParams) next.getLayoutParams();
        if (!sd.containsItem(lp.getViewPosition())) {
            return mid;
        }

        if (lp.isHeader()) {
            if (mid + 1 == count - 1) {
                return mid;
            }

            next = helper.getChildAt(mid + 2);
            if (sd.containsItem(next)) {
                return mid;
            }
        }

        return binarySearchForLastPosition(mid + 1, max, sd, helper);
    }

    private int findFirstVisibleIndex(int edge, int anchorIndex, SectionData sd,
            LayoutQueryHelper helper) {
        final int childCount = helper.getChildCount();
        for (int i = anchorIndex; i < childCount; i++) {
            View child = helper.getChildAt(i);
            if (!sd.containsItem(helper.getPosition(child))) {
                break;
            }

            if (helper.getBottom(child) > edge) {
                return i;
            }
        }

        return INVALID_INDEX;
    }

    private int findHeaderIndexFromFirstIndex(int fvi, SectionData sd, LayoutQueryHelper helper) {
        final int count = helper.getChildCount();
        int fvp = helper.getPosition(helper.getChildAt(fvi));
        // Header is always attached after other section items. So start looking from there, and
        // back towards the current fvi.
        for (int i = Math.min(sd.lastPosition - fvp + 1 + fvi, count - 1); i >= fvi; i--) {
            View check = helper.getChildAt(i);
            if (helper.getPosition(check) == sd.firstPosition) {
                return i;
            }
        }
        return INVALID_INDEX;
    }

    /**
     * The header is almost guaranteed to be at the end so just use look there.
     *
     * @param sd Section data.
     * @return Header, or null if not found.
     */
    private int findHeaderIndexFromLastIndex(int lastIndex, SectionData sd,
            LayoutQueryHelper helper) {
        for (int i = lastIndex; i >= 0; i--) {
            View child = helper.getChildAt(i);
            int position = helper.getPosition(child);
            if (!sd.containsItem(position)) {
                break;
            } else if (sd.firstPosition == position) {
                return i;
            }
        }
        return INVALID_INDEX;
    }

    private int findLastIndexForSection(SectionData sd, LayoutQueryHelper helper) {
        return binarySearchForLastPosition(0, helper.getChildCount() - 1, sd, helper);
    }

    private int findLastVisibleIndex(int edge, int anchorIndex, SectionData sd,
            LayoutQueryHelper helper) {
        for (int i = anchorIndex; i >= 0; i--) {
            View child = helper.getChildAt(i);
            if (!sd.containsItem(helper.getPosition(child))) {
                break;
            }

            if (helper.getTop(child) < edge) {
                return i;
            }
        }

        return INVALID_INDEX;
    }

    private boolean needHeaderOffset(View header) {
        final BaseLayoutManager.LayoutParams layoutParams =
                (BaseLayoutManager.LayoutParams) header.getLayoutParams();
        return !layoutParams.isHeaderSticky() || !layoutParams.isHeaderInline();
    }

    private int updateHeader(int headerIndex, SectionData sd, LayoutQueryHelper helper) {
        final int stickyEdge = helper.getStickyEdge();
        if (headerIndex == INVALID_INDEX) {
            // No header found to update. It must not need to be updated.
            return stickyEdge;
        }

        final View header = helper.getChildAt(headerIndex);
        final BaseLayoutManager.LayoutParams headerParams =
                (BaseLayoutManager.LayoutParams) header.getLayoutParams();
        if (!headerParams.isHeaderSticky()) {
            // Only need to update stickied headers.
            return stickyEdge;
        }

        final int headerTop = helper.getTop(header);
        if (headerTop >= stickyEdge) {
            // Only need to update sticky headers if they are above the sticky edge.
            return stickyEdge;
        }

        SectionLayoutManager slm = helper.getSlm(sd);
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

        int headerIndex = findHeaderIndexFromFirstIndex(fvi, sd, helper);
        return updateHeader(headerIndex, sd, helper);
    }
}
