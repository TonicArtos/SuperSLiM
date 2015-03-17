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

    public int beginFillToEnd(int anchorPosition, SectionData sectionData, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        int markerLine = 0;
        if (sectionData.hasHeader) {
            // Markerline gets shifted here if the header is inline.
            // TODO: Layout header.
        }
        helper.updateVerticalOffset(markerLine);
        int result;
        if (sectionData.subsections != null) {
            result = onFillSubsectionsToEnd(anchorPosition, sectionData, helper, recycler,
                    state);
        } else {
            result = onFillToEnd(anchorPosition, sectionData, helper, recycler, state);
        }
        if (sectionData.hasHeader) {
            // TODO: Attach header.
        }
        return result;
    }

    public int beginFillToStart(int anchorPosition, SectionData sectionData, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        int result;
        if (sectionData.subsections != null) {
            result = onFillSubsectionsToStart(anchorPosition, sectionData, helper, recycler,
                            state);
        } else {
            result = onFillToStart(anchorPosition, sectionData, helper, recycler, state);
        }
        if (sectionData.hasHeader) {
            // TODO: Layout and attach header if needed.
        }
        return result;
    }

    public int computeHeaderOffset(int firstVisiblePosition, SectionData sectionData,
            LayoutHelper helper, Recycler state) {
        return mSlm.computeHeaderOffset(firstVisiblePosition, sectionData, helper, state);
    }

    public int finishFillToEnd(int anchorPosition, SectionData sectionData, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        int result = mSlm.finishFillToEnd(anchorPosition, sectionData, helper, recycler, state);
        if (sectionData.hasHeader) {
            // TODO: Update header attach index.
        }
        return result;
    }

    public int finishFillToStart(int anchorPosition, SectionData sectionData, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        int result = mSlm.onFillToEnd(anchorPosition, sectionData, helper, recycler, state);
        if (sectionData.hasHeader) {
            // TODO: Layout and attach header if needed.
        }
        return result;
    }

    public RecyclerView.LayoutParams generateLayoutParams(LayoutManager.LayoutParams params) {
        return mSlm.generateLayoutParams(params);
    }

    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return mSlm.generateLayoutParams(c, attrs);
    }

    public void getEdgeStates(Rect outRect, View child, SectionData sectionData,
            int layoutDirection) {
        mSlm.getEdgeStates(outRect, child, sectionData, layoutDirection);
    }

    public int getHighestEdge(int firstIndex, int defaultEdge, SectionData sectionData,
            LayoutQueryHelper helper) {
        return mSlm.getHighestEdge(firstIndex, defaultEdge, sectionData, helper);
    }

    public int getLowestEdge(int lastIndex, int defaultEdge, SectionData sectionData,
            LayoutQueryHelper helper) {
        return mSlm.getLowestEdge(lastIndex, defaultEdge, sectionData, helper);
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
    protected int onFillSubsectionsToEnd(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state) {
        return mSlm.onFillSubsectionsToEnd(anchorPosition, sectionData, helper, recycler, state);
    }

    @Override
    protected int onFillSubsectionsToStart(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state) {
        return mSlm.onFillSubsectionsToStart(anchorPosition, sectionData, helper, recycler, state);
    }

    public int onFillToEnd(int anchorPosition, SectionData sectionData, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        return mSlm.onFillToEnd(anchorPosition, sectionData, helper, recycler, state);
    }

    public int onFillToStart(int anchorPosition, SectionData sectionData, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        return mSlm.onFillToStart(anchorPosition, sectionData, helper, recycler, state);
    }

    @Override
    public void onPreTrimAtStartEdge(int startEdge, int firstVisibleIndex,
            SectionData sectionData, LayoutTrimHelper helper) {
        mSlm.onPreTrimAtStartEdge(startEdge, firstVisibleIndex, sectionData, helper);
        updateHeaderForStartEdgeTrim(startEdge, firstVisibleIndex, sectionData, helper);

        if (sectionData.subsections == null) {
            return;
        }
        HashMap<SectionData, Integer> selectedSubsections =
                getSectionsIntersectingStartEdge(startEdge, firstVisibleIndex, sectionData, helper);

        for (SectionData sd : selectedSubsections.keySet()) {
            LayoutTrimHelper subsectionHelper = helper.getSubsectionLayoutTrimHelper();
            subsectionHelper.init(sd);
            helper.getSlm(sd).onPreTrimAtStartEdge(
                    startEdge, selectedSubsections.get(sd), sd, subsectionHelper);
            subsectionHelper.recycle();
        }
    }

    protected int addView(View child, LayoutManager.Direction direction, LayoutHelper helper,
            Recycler recycler) {
        return mSlm.addView(child, direction, helper, recycler);
    }

    /**
     * Find header or, if it cannot be found, the first view for a section.
     *
     * @param anchorIndex Index to start looking from.
     * @return Null if no header or first item found, otherwise the found view.
     */
    View findAttachedHeaderOrFirstViewFor(final int anchorIndex, LayoutQueryHelper helper) {
        final int count = helper.getChildCount();

        View anchor = helper.getChildAt(anchorIndex);
        int anchorPosition = helper.getPosition(anchor);
        SectionData sd = helper.getSectionData(helper.getPosition(anchor));

        // Most likely attachment point for header. It will be here or earlier, if at all.
        int headerIndex = Math.min(sd.lastPosition - anchorPosition + 1, count - 1) + anchorIndex;
        for (int childIndex = headerIndex; anchorIndex <= childIndex; childIndex--) {
            View child = helper.getChildAt(childIndex);

            int childPosition = helper.getPosition(child);
            if (childPosition == sd.firstPosition) {
                return child;
            }
            if (!sd.containsItem(childPosition)) {
                break;
            }
        }

        return null;
    }

    private int getHeaderViewIndex(int fvi, SectionData sectionData, LayoutQueryHelper helper) {
        final int count = helper.getChildCount();
        int fvp = helper.getPosition(helper.getChildAt(fvi));
        // Header is always attached after other section items. So start looking from there, and
        // back towards the current fvi.
        for (int i = Math.min(sectionData.lastPosition - fvp + 1 + fvi, count - 1); i >= 0; i--) {
            View check = helper.getChildAt(i);
            if (helper.getPosition(check) == sectionData.firstPosition) {
                return i;
            }
        }
        return -1;
    }

//    private int getSectionBottom(int headerIndex, SectionData sectionData,
// LayoutQueryHelper helper) {
//        final int count = helper.getChildCount();
//        View header = helper.getChildAt(headerIndex);
//        int sectionBottom = helper.getBottom(header);
//        if (headerIndex + 1 < count) {
//            View nextInNextSection = helper.getChildAt(headerIndex + 1);
//            if (!sectionData.containsItem(helper.getPosition(nextInNextSection))) {
//                View firstItemInNextSection = findAttachedHeaderOrFirstViewFor(
//                        headerIndex + 1, helper);
//                if (firstItemInNextSection == null) {
//                    sectionBottom = helper.getTop(nextInNextSection);
//                } else {
//                    sectionBottom = helper.getTop(firstItemInNextSection);
//                }
//            }
//        }
//        return sectionBottom;
//    }

    private void updateHeaderForStartEdgeTrim(int startEdge, int fvi, SectionData sectionData,
            LayoutTrimHelper helper) {
        if (!sectionData.hasHeader) {
            return;
        }

        int headerIndex = getHeaderViewIndex(fvi, sectionData, helper);
        if (headerIndex == -1) {
            // No header found to update. It must not need to be updated.
            return;
        }

        final View header = helper.getChildAt(headerIndex);
        final BaseLayoutManager.LayoutParams headerParams =
                (BaseLayoutManager.LayoutParams) header.getLayoutParams();
        if (!headerParams.isHeaderSticky()) {
            // Only need to update stickied headers.
            return;
        }

        final int headerTop = helper.getTop(header);
        if (headerTop >= startEdge) {
            // Only need to update sticky headers if they are above the start edge.
            return;
        }

        SectionLayoutManager slm = helper.getSlm(sectionData);
        final int sectionBottom = slm.getLowestEdge(
                headerIndex, helper.getBottom(header), sectionData, helper);

        final int headerHeight = helper.getMeasuredHeight(header);
        int top = headerHeight + startEdge > sectionBottom ?
                sectionBottom - headerHeight : startEdge;

        int delta = headerTop - top;
        header.offsetTopAndBottom(-delta);
    }
}
