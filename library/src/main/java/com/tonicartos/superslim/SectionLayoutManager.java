package com.tonicartos.superslim;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

public abstract class SectionLayoutManager {

    private static final int MARGIN_UNSET = -1;

    protected LayoutManager mLayoutManager;

    public SectionLayoutManager(LayoutManager layoutManager) {
        mLayoutManager = layoutManager;
    }

    /**
     * Compute the offset for side aligned headers. If the height of the non-visible area of the
     * section is taller than the header, then the header should be offscreen, in that case return
     * any +ve number.
     *
     * @param firstVisiblePosition Position of first visible item in section.
     * @param sd                   Section data.
     * @param state                Layout state.
     * @return -ve number giving the distance the header should be offset before the anchor view. A
     * +ve number indicates the header is offscreen.
     */
    public abstract int computeHeaderOffset(int firstVisiblePosition, SectionData sd,
            LayoutState state);

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
            SectionData sd, LayoutState state);

    public abstract int fillToStart(int leadingEdge, int markerLine, int anchorPosition,
            SectionData sd, LayoutState state);

    /**
     * Find the position of the first completely visible item of this section.
     *
     * @param sectionFirstPosition First position of section being queried.
     * @return Position of first completely visible item.
     */
    public int findFirstCompletelyVisibleItemPosition(int sectionFirstPosition) {
        View item = getFirstCompletelyVisibleView(sectionFirstPosition, false);
        if (item == null) {
            return LayoutManager.INVALID_POSITON;
        }
        return mLayoutManager.getPosition(item);
    }

    /**
     * Find the position of the first visible item of the section.
     *
     * @param sectionFirstPosition First position of section being queried.
     * @return Position of first visible item.
     */
    public int findFirstVisibleItemPosition(int sectionFirstPosition) {
        View item = getFirstVisibleView(sectionFirstPosition, false);
        if (item == null) {
            return LayoutManager.INVALID_POSITON;
        }
        return mLayoutManager.getPosition(item);
    }

    /**
     * Find the position of the first visible item of this section.
     *
     * @param sectionFirstPosition First position of section being queried.
     * @return Position of first visible item.
     */
    public int findLastCompletelyVisibleItemPosition(int sectionFirstPosition) {
        View item = getLastCompletelyVisibleView(sectionFirstPosition);
        if (item == null) {
            return LayoutManager.INVALID_POSITON;
        }
        return mLayoutManager.getPosition(item);
    }

    /**
     * Find the position of the first visible item of the section.
     *
     * @param sectionFirstPosition First position of section being queried.
     * @return Position of first visible item.
     */
    public int findLastVisibleItemPosition(int sectionFirstPosition) {
        View item = getLastVisibleView(sectionFirstPosition);
        if (item == null) {
            return LayoutManager.INVALID_POSITON;
        }
        return mLayoutManager.getPosition(item);
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
    public abstract int finishFillToEnd(int leadingEdge, View anchor, SectionData sd,
            LayoutState state);

    public abstract int finishFillToStart(int leadingEdge, View anchor, SectionData sd,
            LayoutState state);

    public LayoutManager.LayoutParams generateLayoutParams(LayoutManager.LayoutParams params) {
        return params;
    }

    public LayoutManager.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutManager.LayoutParams(c, attrs);
    }

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
            if (sectionFirstPosition == lp.getTestedFirstPosition()) {
                if (topInside && bottomInside) {
                    if (!lp.isHeader || !skipHeader) {
                        return view;
                    } else {
                        candidate = view;
                    }
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

    /**
     * Find the highest displayed edge of the section. If there is no member found then return the
     * default edge instead.
     *
     * @param sectionFirstPosition Section id, position of first item in the section.
     * @param firstIndex           Child index to start looking from.
     * @param defaultEdge          Default value.
     * @return Top (attached) edge of the section.
     */
    public int getHighestEdge(int sectionFirstPosition, int firstIndex, int defaultEdge) {
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
        return defaultEdge;
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
            if (sectionFirstPosition == lp.getTestedFirstPosition()) {
                if (topInside && bottomInside) {
                    if (!lp.isHeader) {
                        return view;
                    } else {
                        candidate = view;
                    }
                }
            } else if (candidate == null) {
                sectionFirstPosition = lp.getTestedFirstPosition();
                continue;
            } else {
                return candidate;
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
     * default edge instead.
     *
     * @param sectionFirstPosition Section id, position of first item in the section.
     * @param lastIndex            Index to start looking from. Usually the index of the last
     *                             attached view in this section.
     * @param defaultEdge          Default value.
     * @return Lowest (attached) edge of the section.
     */
    public int getLowestEdge(int sectionFirstPosition, int lastIndex, int defaultEdge) {
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

    public SectionLayoutManager init(SectionData sd) {
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
