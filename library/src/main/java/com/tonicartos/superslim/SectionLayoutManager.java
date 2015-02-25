package com.tonicartos.superslim;

import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;

import java.util.ArrayList;

public abstract class SectionLayoutManager {

    private static final int MARGIN_UNSET = -1;

    protected RecyclerView.LayoutManager mLayoutManager;

    public SectionLayoutManager(LayoutManager layoutManager) {
        mLayoutManager = layoutManager;
    }

    /**
     * Compute the offset for side aligned headers. If the height of the non-visible area of the
     * section is taller than the header, then the header should be offscreen, in that case return
     * any +ve number.
     *
     * @param anchor View to compute offset against. The top of the view is the reference point.
     * @param sd     Section data.
     * @param state  Layout state.
     * @return -ve number giving the distance the header should be offset before the anchor view. A
     * +ve number indicates the header is offscreen.
     */
    public abstract int computeHeaderOffset(View anchor, SectionData2 sd, LayoutState state);

    /**
     * Measure and layout children. Make sure to only lay out views belonging to this mSection,
     * excepting headers, which are laid out by the wrapping layout manager.
     */
    public abstract FillResult fill(LayoutState state, SectionData sectionData);

    /**
     * Fill section content towards the end.
     *
     * @param leadingEdge    Line to fill up to. Content will not be wholly beyond this line.
     * @param markerLine     Start of the section content area.
     * @param anchorPosition Adapter position for the first content item in the section.
     * @param sd             Section data.
     * @param state          Layout state.
     * @return Line to which content has been filled.
     */
    public abstract int fillToEnd(int leadingEdge, int markerLine, int anchorPosition,
            SectionData2 sd, LayoutState state);

    public abstract int fillToStart(int leadingEdge, int markerLine, int anchorPosition,
            SectionData2 sd, LayoutState state);

    /**
     * Find the position of the first completely visible item of this section.
     *
     * @param sectionFirstPosition First position of section being queried.
     * @return Position of first completely visible item.
     */
    public int findFirstCompletelyVisibleItemPosition(int sectionFirstPosition) {
        return mLayoutManager
                .getPosition(getFirstCompletelyVisibleView(sectionFirstPosition, false));
    }

    /**
     * Find the position of the first visible item of the section.
     *
     * @param sectionFirstPosition First position of section being queried.
     * @return Position of first visible item.
     */
    public int findFirstVisibleItemPosition(int sectionFirstPosition) {
        return mLayoutManager.getPosition(getFirstVisibleView(sectionFirstPosition, false));
    }

    /**
     * Find the position of the first visible item of this section.
     *
     * @param sectionFirstPosition First position of section being queried.
     * @return Position of first visible item.
     */
    public int findLastCompletelyVisibleItemPosition(int sectionFirstPosition) {
        return mLayoutManager.getPosition(getLastCompletelyVisibleView(sectionFirstPosition));
    }

    /**
     * Find the position of the first visible item of the section.
     *
     * @param sectionFirstPosition First position of section being queried.
     * @return Position of first visible item.
     */
    public int findLastVisibleItemPosition(int sectionFirstPosition) {
        return mLayoutManager.getPosition(getLastVisibleView(sectionFirstPosition));
    }

    /**
     * Finish filling an already partially filled section.
     *
     * @param leadingEdge Line to fill up to. Content will not be wholly beyond this line.
     * @param anchor      Last attached content item in this section.
     * @param sd          Section data.
     * @param state       Layout state.
     * @return Line to which content has been filled.
     */
    public abstract int finishFillToEnd(int leadingEdge, View anchor, SectionData2 sd,
            LayoutState state);

    public abstract int finishFillToStart(int leadingEdge, View anchor, SectionData2 sd,
            LayoutState state);

    public int getAnchorPosition(LayoutState state, SectionData params, int position) {
        return position;
    }

    /**
     * Locate the first view in this section that is completely visible. Will skip headers unless
     * they are the only one visible.
     *
     * @param sectionFirstPosition First position of section being queried.
     * @param skipHeader           Do not include the section header if it has one.
     * @return First completely visible item or null.
     */
    public View getFirstCompletelyVisibleView(int sectionFirstPosition, boolean skipHeader) {
        final int topEdge = mLayoutManager.getClipToPadding() ? mLayoutManager.getPaddingTop() : 0;
        final int bottomEdge = mLayoutManager.getClipToPadding() ?
                mLayoutManager.getHeight() - mLayoutManager.getPaddingBottom() :
                mLayoutManager.getHeight();

        int lookAt = 0;
        int childCount = mLayoutManager.getChildCount();
        View candidate = null;
        while (true) {
            if (lookAt >= childCount) {
                return candidate;
            }

            final View view = mLayoutManager.getChildAt(lookAt);

            final boolean topInside = mLayoutManager.getDecoratedTop(view) >= topEdge;
            final boolean bottomInside = mLayoutManager.getDecoratedBottom(view) <= bottomEdge;

            LayoutManager.LayoutParams lp = (LayoutManager.LayoutParams) view.getLayoutParams();
            if (sectionFirstPosition == lp.getTestedFirstPosition() && topInside && bottomInside) {
                if (!lp.isHeader || !skipHeader) {
                    return view;
                } else {
                    candidate = view;
                }
            } else {
                // Skipped past section.
                return candidate;
            }

            lookAt += 1;
        }
    }

    /**
     * Locate the visible view which has the earliest adapter position. Will skip headers unless
     * they are the only one visible.
     *
     * @param sectionFirstPosition Position of first position of section..
     * @param skipHeader           Do not include the section header if it has one.
     * @return View.
     */
    public View getFirstVisibleView(int sectionFirstPosition, boolean skipHeader) {
        int lookAt = 0;
        int childCount = mLayoutManager.getChildCount();
        View candidate = null;
        while (true) {
            if (lookAt >= childCount) {
                return candidate;
            }

            final View view = mLayoutManager.getChildAt(lookAt);
            LayoutManager.LayoutParams lp = (LayoutManager.LayoutParams) view.getLayoutParams();
            if (sectionFirstPosition == lp.getTestedFirstPosition()) {
                if (!lp.isHeader || !skipHeader) {
                    return view;
                } else {
                    candidate = view;
                }
            } else {
                // Skipped past section.
                return candidate;
            }

            lookAt += 1;
        }
    }

    public int getHeaderEndMargin() {
        return MARGIN_UNSET;
    }

    public int getHeaderStartMargin() {
        return MARGIN_UNSET;
    }

    /**
     * Find the highest displayed edge of the section. If there is no member found then return the
     * start edge instead.
     */
    public int getHighestEdge(int sectionFirstPosition, int firstIndex, int startEdge) {
        // Look from start to find children that are the highest.
        for (int i = firstIndex; i < mLayoutManager.getChildCount(); i++) {
            View child = mLayoutManager.getChildAt(i);
            LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) child
                    .getLayoutParams();
            if (params.getTestedFirstPosition() != sectionFirstPosition) {
                break;
            }
            if (params.isHeader) {
                continue;
            }
            // A more interesting layout would have to do something more here.
            return mLayoutManager.getDecoratedTop(child);
        }
        return startEdge;
    }

    /**
     * Locate the last view in this section that is completely visible. Will skip headers unless
     * they are the only one visible.
     *
     * @param sectionFirstPosition First position of section being queried.
     * @return Last completely visible item or null.
     */
    public View getLastCompletelyVisibleView(int sectionFirstPosition) {
        final int topEdge = mLayoutManager.getClipToPadding() ? mLayoutManager.getPaddingTop() : 0;
        final int bottomEdge = mLayoutManager.getClipToPadding() ?
                mLayoutManager.getHeight() - mLayoutManager.getPaddingBottom() :
                mLayoutManager.getHeight();

        int lookAt = mLayoutManager.getChildCount() - 1;
        View candidate = null;
        while (true) {
            if (lookAt < 0) {
                return candidate;
            }

            final View view = mLayoutManager.getChildAt(lookAt);

            final boolean topInside = mLayoutManager.getDecoratedTop(view) >= topEdge;
            final boolean bottomInside = mLayoutManager.getDecoratedBottom(view) <= bottomEdge;

            LayoutManager.LayoutParams lp = (LayoutManager.LayoutParams) view.getLayoutParams();
            if (sectionFirstPosition == lp.getTestedFirstPosition() && topInside && bottomInside) {
                if (!lp.isHeader) {
                    return view;
                } else {
                    candidate = view;
                }
            }

            lookAt -= 1;
        }
    }

    /**
     * Locate the visible view which has the latest adapter position.
     *
     * @param sectionFirstPosition Section id.
     * @return View.
     */
    public View getLastVisibleView(int sectionFirstPosition) {
        int lookAt = mLayoutManager.getChildCount() - 1;
        View candidate = null;
        while (true) {
            if (lookAt < 0) {
                return candidate;
            }

            View view = mLayoutManager.getChildAt(lookAt);
            LayoutManager.LayoutParams lp = (LayoutManager.LayoutParams) view.getLayoutParams();
            if (sectionFirstPosition == lp.getTestedFirstPosition()) {
                if (!lp.isHeader) {
                    return view;
                } else {
                    candidate = view;
                }
            } else {
                // Skipped past section.
                return candidate;
            }

            lookAt -= 1;
        }
    }

    /**
     * Find the lowest displayed edge of the section. If there is no member found then return the
     * end edge instead.
     */
    public int getLowestEdge(int sectionFirstPosition, int lastIndex, int endEdge) {
        // Look from end to find children that are the lowest.
        for (int i = lastIndex; i >= 0; i--) {
            View child = mLayoutManager.getChildAt(i);
            LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) child
                    .getLayoutParams();
            if (params.getTestedFirstPosition() != sectionFirstPosition) {
                break;
            }
            if (params.isHeader) {
                continue;
            }
            // A more interesting layout would have to do something more here.
            return mLayoutManager.getDecoratedBottom(child);
        }
        return endEdge;
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

    public SectionLayoutManager init(SectionData2 sd) {
        return this;
    }

    protected int addView(LayoutState.View child, int position, LayoutManager.Direction direction,
            LayoutState state) {
        int addIndex;
        if (direction == LayoutManager.Direction.START) {
            addIndex = 0;
        } else {
            addIndex = mLayoutManager.getChildCount();
        }

        state.decacheView(position);
        mLayoutManager.addView(child.view, addIndex);

        return addIndex;
    }
}
