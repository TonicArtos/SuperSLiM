package com.tonicartos.superslim;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

public abstract class SectionLayoutManager {

    private static final int MARGIN_UNSET = -1;

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
     * Fill section content towards the end.
     *
     * @param anchorPosition Adapter position for the first content item in the section.
     * @param sectionData    Section data.
     * @param helper         Layout helper.
     * @param recycler       Layout recycler.
     * @return Line to which content has been filled.
     */
    public abstract int fillToEnd(int anchorPosition, SectionData sectionData, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state);

    /**
     * Fill section content towards the start.
     *
     * @param anchorPosition Adapter position for the last content item in the section.
     * @param sectionData    Section data.
     * @param helper         Layout helper.
     * @param recycler       Layout recycler.
     * @return Line to which content has been filled.
     */
    public abstract int fillToStart(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state);

    public LayoutManager.LayoutParams generateLayoutParams(LayoutManager.LayoutParams params) {
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
            LayoutHelper helper) {
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
            LayoutHelper helper) {
        // Look from start to find children that are the highest.
        for (int i = firstIndex; i < helper.getChildCount(); i++) {
            View child = helper.getChildAt(i);
            LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) child
                    .getLayoutParams();
            if (sectionData.containsItem(params.getViewPosition())) {
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
            LayoutHelper helper) {
        // Look from end to find children that are the lowest.
        for (int i = lastIndex; i >= 0; i--) {
            View child = helper.getChildAt(i);
            LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) child
                    .getLayoutParams();
            if (sectionData.containsItem(params.getViewPosition())) {
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

    public SectionLayoutManager init(SectionData sd, LayoutHelper helper) {
        return this;
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
}
