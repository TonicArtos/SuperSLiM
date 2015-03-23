package com.tonicartos.superslim;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

import java.util.HashMap;

public abstract class SectionLayoutManager {

    private HashMap<SectionData, Bundle> mSavedConfiguration = new HashMap<>();

    /**
     * Start filling a new section towards the end. Might end out filling out the entire section.
     *
     * @param anchorPosition Adapter position for the first content item in the section.
     * @param sectionData    Section data.
     * @param helper         Layout helper.
     * @param recycler       Recycler.
     * @return Line to which content has been filled.
     */
    public int beginFillToEnd(int anchorPosition, SectionData sectionData, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        return 0;
    }

    /**
     * Start filling a new section towards the start. Might end out filling out the entire section.
     *
     * @param anchorPosition Adapter position for the last content item in the section.
     * @param sectionData    Section data.
     * @param helper         Layout helper.
     * @param recycler       Recycler.
     * @return Line to which content has been filled.
     */
    public int beginFillToStart(int anchorPosition, SectionData sectionData, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        return 0;
    }

    /**
     * Compute the offset for side aligned headers. If the height of the non-visible area of the
     * section is taller than the header, then the header should be offscreen, in that case return
     * any +ve number.
     *
     * @param firstVisiblePosition Position of first visible item in section.
     * @param sectionData          Section data.
     * @param helper               Layout helper.
     * @param recycler             Layout state.
     * @return -ve number giving the distance the header should be offset before the anchor view. A
     * +ve number indicates the header is offscreen.
     */
    public abstract int computeHeaderOffset(int firstVisiblePosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler);

    /**
     * Finish filling a section towards the end.
     *
     * @param anchorPosition Adapter position for the first content item in the section.
     * @param sectionData    Section data.
     * @param helper         Layout helper.
     * @param recycler       Recycler.
     * @return Line to which content has been filled.
     */
    public int finishFillToEnd(int anchorPosition, SectionData sectionData, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        return onFillToEnd(anchorPosition, sectionData, helper, recycler, state);
    }

    /**
     * Finish filling a section towards the start.
     *
     * @param anchorPosition Adapter position for the last content item in the section.
     * @param sectionData    Section data.
     * @param helper         Layout helper.
     * @param recycler       Recycler.
     * @return Line to which content has been filled.
     */
    public int finishFillToStart(int anchorPosition, SectionData sectionData, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        return onFillToEnd(anchorPosition, sectionData, helper, recycler, state);
    }

    public RecyclerView.LayoutParams generateLayoutParams(RecyclerView.LayoutParams params) {
        return params;
    }

    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutManager.LayoutParams(c, attrs);
    }

    /**
     * Tell decorators which edges are internal and external. The default implementation assumes a
     * linear list.
     *
     * @param outRect     Rect to load with ege states.
     * @param child       Child to look at.
     * @param sectionData Section data.
     */
    public void getEdgeStates(Rect outRect, View child, SectionData sectionData,
            int layoutDirection) {
        outRect.left = ItemDecorator.EXTERNAL;
        outRect.right = ItemDecorator.EXTERNAL;
        LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) child.getLayoutParams();
        final int position = params.getViewPosition();
        int firstContentPosition = (sectionData.hasHeader ?
                Math.min(sectionData.firstPosition + 1, sectionData.lastPosition) :
                sectionData.firstPosition);
        outRect.top = position == firstContentPosition ?
                ItemDecorator.EXTERNAL : ItemDecorator.INTERNAL;
        outRect.bottom = position == sectionData.lastPosition ?
                ItemDecorator.EXTERNAL : ItemDecorator.INTERNAL;
    }

    /**
     * Find the highest displayed edge of the section. If there is no member found then return the
     * default edge instead.
     *
     * @param firstIndex  Child index to start looking from.
     * @param defaultEdge Default value.
     * @return Top (attached) edge of the section.
     */
    public int getHighestEdge(int firstIndex, int defaultEdge, SectionData sectionData,
            LayoutQueryHelper helper) {
        // Look from start to find children that are the highest.
        for (int i = firstIndex; i < helper.getChildCount(); i++) {
            View child = helper.getChildAt(i);
            LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) child
                    .getLayoutParams();
            if (!sectionData.containsItem(params.getViewPosition())) {
                break;
            }
            if (params.isHeader()) {
                continue;
            }
            // A more interesting layout would have to do something more here.
            return helper.getTop(child);
        }
        return defaultEdge;
    }

    /**
     * Find the lowest displayed edge of the section. If there is no member found then return the
     * default edge instead.
     *
     * @param lastIndex   Index to start looking from. Usually the index of the last attached view
     *                    in this section.
     * @param defaultEdge Default value.
     * @return Lowest (attached) edge of the section.
     */
    public int getLowestEdge(int lastIndex, int defaultEdge, SectionData sectionData,
            LayoutQueryHelper helper) {
        // Look from end to find children that are the lowest.
        for (int i = lastIndex; i >= 0; i--) {
            View child = helper.getChildAt(i);
            LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) child
                    .getLayoutParams();
            if (!sectionData.containsItem(params.getViewPosition())) {
                break;
            }
            if (params.isHeader()) {
                continue;
            }
            // A more interesting layout would have to do something more here.
            return helper.getBottom(child);
        }
        return defaultEdge;
    }

    public int howManyMissingAbove(int firstPosition, SparseArray<Boolean> positionsOffscreen) {
        int itemsSkipped = 0;
        int itemsFound = 0;
        for (int i = firstPosition; itemsFound < positionsOffscreen.size(); i++) {
            if (positionsOffscreen.get(i, false)) {
                itemsFound += 1;
            } else {
                itemsSkipped += 1;
            }
        }

        return itemsSkipped;
    }

    public int howManyMissingBelow(int lastPosition, SparseArray<Boolean> positionsOffscreen) {
        int itemsSkipped = 0;
        int itemsFound = 0;
        for (int i = lastPosition;
                itemsFound < positionsOffscreen.size(); i--) {
            if (positionsOffscreen.get(i, false)) {
                itemsFound += 1;
            } else {
                itemsSkipped += 1;
            }
        }

        return itemsSkipped;
    }

    protected void addView(View child, LayoutHelper helper, Recycler recycler) {
        recycler.decacheView(helper.getPosition(child));
        helper.addView(child);
    }

    protected void addView(View child, int index, LayoutHelper helper, Recycler recycler) {
        recycler.decacheView(helper.getPosition(child));
        helper.addView(child, index);
    }

    protected abstract int onFillSubsectionsToEnd(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state);

    protected abstract int onFillSubsectionsToStart(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state);

    /**
     * Fill section content towards the end.
     *
     * @param anchorPosition Adapter position for the first content item in the section.
     * @param sectionData    Section data.
     * @param helper         Layout helper.
     * @param recycler       Recycler.
     * @return Line to which content has been filled.
     */
    protected abstract int onFillToEnd(int anchorPosition, SectionData sectionData,
            LayoutHelper helper,
            Recycler recycler, RecyclerView.State state);

    /**
     * Fill section content towards the start.
     *
     * @param anchorPosition Adapter position for the last content item in the section.
     * @param sectionData    Section data.
     * @param helper         Layout helper.
     * @param recycler       Recycler.
     * @return Line to which content has been filled.
     */
    protected abstract int onFillToStart(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state);

    protected void onInit(Bundle savedConfiguration, SectionData sectionData,
            LayoutQueryHelper helper) {
    }

    /**
     * Called before items are trimmed for any section that intersects the end edge. This is the
     * opportunity to update views before they might otherwise be trimmed for being beyond the
     * edge.
     *
     * @param lastVisibleIndex Index of last item in this section that is visible.
     * @param sectionData      Section data.
     * @param helper           Layout query helper.
     */
    protected void onPreTrimAtEndEdge(final int lastVisibleIndex, final SectionData sectionData,
            final LayoutTrimHelper helper) {

    }

    /**
     * Called before items are trimmed for any section that intersects the start edge. This is the
     * opportunity to update views before they might otherwise be trimmed for being beyond the
     * edge.
     *
     * @param firstVisibleIndex Index of first item in this section that is visible.
     * @param sectionData       Section data.
     * @param helper            Layout query helper.
     */
    protected void onPreTrimAtStartEdge(final int firstVisibleIndex, final SectionData sectionData,
            final LayoutTrimHelper helper) {
    }

    protected void onReset() {
    }

    protected void saveConfiguration(SectionData sectionData, Bundle configuration) {
        mSavedConfiguration.put(sectionData, configuration);
    }

    SectionLayoutManager init(SectionData sectionData, LayoutQueryHelper helper) {
        onInit(mSavedConfiguration.get(sectionData), sectionData, helper);
        return this;
    }

    /**
     * There exists a problem in that when filling towards the start edge, that headers can only be
     * added after the section content has been placed. However, a subsection's sticky header's
     * position is dependent on the supersection's sticky header position and height. So that means
     * we have to make a second pass to make sure all the sticky headers are properly positioned.
     *
     * In other words, this method exists only to fix nested sticky headers and is implemented
     * solely by the SlmWrapper, but needs to be called by the LayoutManager, which is why it is
     * package private.
     */
    void onPostFinishFillToStart(SectionData sectionData, LayoutTrimHelper helper) {
    }

    void reset() {
        mSavedConfiguration.clear();
        onReset();
    }

    public class SlmConfig {

        int marginStart;

        int marginEnd;

        @LayoutManager.SectionManager
        int sectionManagerKind;

        String sectionManager;

        public SlmConfig(int marginStart, int marginEnd, String sectionManager) {
            this.marginStart = marginStart;
            this.marginEnd = marginEnd;
            this.sectionManagerKind = LayoutManager.SECTION_MANAGER_CUSTOM;
            this.sectionManager = sectionManager;
        }

        public SlmConfig(int marginStart, int marginEnd,
                @LayoutManager.SectionManager int sectionManager) {
            this.marginStart = marginStart;
            this.marginEnd = marginEnd;
            this.sectionManagerKind = sectionManager;
        }
    }

}
