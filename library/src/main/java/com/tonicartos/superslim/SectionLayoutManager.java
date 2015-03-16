package com.tonicartos.superslim;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class SectionLayoutManager {

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
        // TODO: Layout header.
        int markerLine = 0;
        helper.updateVerticalOffset(markerLine);
        int result = onFillToEnd(anchorPosition, sectionData, helper, recycler, state);
        // TODO: Attach header.
        return result;
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
        int result = onFillToEnd(anchorPosition, sectionData, helper, recycler, state);
        // TODO: Layout and attach header if needed.
        return result;
    }

    /**
     * Compute the offset for side aligned headers. If the height of the non-visible area of the
     * section is taller than the header, then the header should be offscreen, in that case return
     * any +ve number.
     *
     * @param firstVisiblePosition Position of first visible item in section.
     * @param sectionData          Section data.
     * @param helper               Layout helper.
     * @param state                Layout state.
     * @return -ve number giving the distance the header should be offset before the anchor view. A
     * +ve number indicates the header is offscreen.
     */
    public abstract int computeHeaderOffset(int firstVisiblePosition, SectionData sectionData,
            LayoutHelper helper, Recycler state);

    /**
     * Basic implementation. Does a linear search from the super's first visible index for the first
     * visible index of the subsection.
     *
     * @param supersFirstVisibleIndex The child index that is visible. It is
     * @return -1 if none found, or
     */
    public int findSubsectionsFirstVisibleIndex(int supersFirstVisibleIndex, SectionData subSd) {
        return 0;
    }

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
            LayoutQueryHelper layout) {
        // Look from start to find children that are the highest.
        for (int i = firstIndex; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) child
                    .getLayoutParams();
            if (sectionData.containsItem(params.getViewPosition())) {
                break;
            }
            if (params.isHeader()) {
                continue;
            }
            // A more interesting layout would have to do something more here.
            return layout.getTop(child);
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
            LayoutQueryHelper layout) {
        // Look from end to find children that are the lowest.
        for (int i = lastIndex; i >= 0; i--) {
            View child = layout.getChildAt(i);
            LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) child
                    .getLayoutParams();
            if (sectionData.containsItem(params.getViewPosition())) {
                break;
            }
            if (params.isHeader()) {
                continue;
            }
            // A more interesting layout would have to do something more here.
            return layout.getBottom(child);
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

    public SectionLayoutManager init(SectionData sd, LayoutQueryHelper helper) {
        return this;
    }

    /**
     * Fill section content towards the end.
     *
     * @param anchorPosition Adapter position for the first content item in the section.
     * @param sectionData    Section data.
     * @param helper         Layout helper.
     * @param recycler       Recycler.
     * @return Line to which content has been filled.
     */
    public abstract int onFillToEnd(int anchorPosition, SectionData sectionData,
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
    public abstract int onFillToStart(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state);

    public void onPreTrimAtEndEdge(final int endEdge, final SectionData sectionData,
            final LayoutTrimHelper helper) {

    }

    /**
     * Called before items are trimmed for any section that intersects the start edge. This is the
     * opportunity to update views before they might otherwise be trimmed for being beyond the
     * edge.
     *
     * @param sectionData       Section data.
     * @param firstVisibleIndex Index of first item in this section that is visible.
     * @param helper            Layout query helper.
     */
    public void onPreTrimAtStartEdge(final int startEdge, final int firstVisibleIndex,
            final SectionData sectionData, final LayoutTrimHelper helper) {
    }

    /**
     * Called when the section is being trimmed at the end edge.
     *
     * @param edgeLine Edge line. Generally equals the height of the recycler view.
     */
    public void trimAtEndEdge(int edgeLine) {
    }

    protected int addView(View child, LayoutManager.Direction direction, LayoutHelper helper,
            Recycler recycler) {
        int addIndex;
        if (direction == LayoutManager.Direction.START) {
            addIndex = 0;
        } else {
            addIndex = helper.getChildCount();
        }

        recycler.decacheView(helper.getPosition(child));
        helper.addView(child, addIndex);

        return addIndex;
    }

    /**
     * Get a map of sections and their first visible positions that intersect the start edge.
     *
     * <p>The basic implementation looks through all attached child views for this section. You
     * should consider an implementation that constrains the search to a minimal range.</p>
     *
     * @param startEdge         Edge line. Generally 0.
     * @param firstVisibleIndex First visible index for this section.
     * @param sectionData       Section data.
     * @param helper            Layout query helper.
     * @return Map of subsection data to subsection first visible edges.
     */
    protected HashMap<SectionData, Integer> getSectionsIntersectingStartEdge(int startEdge,
            int firstVisibleIndex, SectionData sectionData, LayoutQueryHelper helper) {
        // Work out max number of items we have to check to find sections which intersect start
        // edge. Also, cap to  number of items after fvi.
        int range = Math.min(sectionData.lastPosition
                        - helper.getPosition(helper.getChildAt(firstVisibleIndex)) + 1,
                helper.getChildCount() - firstVisibleIndex);

        // Select subsections which have items overlapping or before the start edge.
        HashMap<SectionData, Integer> selectedSubsections = new HashMap<>();
        for (int i = 0; i < range; i++) {
            int childIndex = i + firstVisibleIndex;
            View child = helper.getChildAt(childIndex);
            if (helper.getTop(child) < startEdge) {
                int childPosition = helper.getPosition(child);
                for (SectionData sd : sectionData.subsections) {
                    if (selectedSubsections.get(sd) == null && sd.containsItem(childPosition)) {
                        int subsectionFvi = findFirstVisibleIndex(startEdge, childIndex, sd,
                                helper);
                        if (subsectionFvi != -1) {
                            selectedSubsections.put(sd, subsectionFvi);
                        }
                        break;
                    }
                }
            }

            if (selectedSubsections.size() == sectionData.subsections.size()) {
                // Already added every section.
                break;
            }
        }
        return selectedSubsections;
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

        return -1;
    }
}
