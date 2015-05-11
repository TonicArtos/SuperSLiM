package com.tonicartos.superslim;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

/**
 * Lays out views in a grid. The number of columns can be set directly, or a minimum size can be
 * requested. If you request a 100dip minimum column size and there is 330dip available, the layout
 * with calculate there to be 3 columns each 130dip across.
 */
public class GridSLM extends SectionLayoutManager {

    private static final int AUTO_FIT = -1;

    private static final int DEFAULT_NUM_COLUMNS = 1;

    public static int ID = LayoutManager.SECTION_MANAGER_GRID;

    private final Context mContext;

    private int mMinimumWidth = 0;

    private int mNumColumns = 0;

    private int mColumnWidth;

    private boolean mColumnsSpecified;

    public GridSLM(LayoutManager layoutManager, Context context) {
        super(layoutManager);
        mContext = context;
    }

    @Override
    public int computeHeaderOffset(int firstVisiblePosition, SectionData sd, LayoutState state) {
        final int itemCount = state.getRecyclerState().getItemCount();

        /*
         * Work from an assumed overlap and add heights from the start until the overlap is zero or
         * less, or the current position (or max items) is reached.
         */
        int areaAbove = 0;
        for (int position = sd.firstPosition + 1;
                areaAbove < sd.headerHeight && position < firstVisiblePosition;
                position += mNumColumns) {
            // Look to see if the header overlaps with the displayed area of the mSection.
            int rowHeight = 0;
            for (int col = 0; col < mNumColumns && position + col < itemCount; col++) {
                LayoutState.View child = state.getView(position + col);
                measureChild(child, sd);
                rowHeight =
                        Math.max(rowHeight, mLayoutManager.getDecoratedMeasuredHeight(child.view));
                state.cacheView(position + col, child.view);
            }
            areaAbove += rowHeight;
        }

        if (areaAbove == sd.headerHeight) {
            return 0;
        } else if (areaAbove > sd.headerHeight) {
            return 1;
        } else {
            return -areaAbove;
        }
    }

    @Override
    public int fillToEnd(int leadingEdge, int markerLine, int anchorPosition, SectionData sd,
            LayoutState state) {
        if (markerLine >= leadingEdge) {
            return markerLine;
        }

        final int itemCount = state.getRecyclerState().getItemCount();
        if (anchorPosition >= itemCount) {
            return markerLine;
        }

        LayoutState.View anchor = state.getView(anchorPosition);
        state.cacheView(anchorPosition, anchor.view);
        if (anchor.getLayoutParams().getTestedFirstPosition() != sd.firstPosition) {
            return markerLine;
        }

        final int firstContentPosition = sd.hasHeader ? sd.firstPosition + 1 : sd.firstPosition;

        // Ensure the anchor is the first item in the row.
        final int col = (anchorPosition - firstContentPosition) % mNumColumns;
        for (int i = 1; i <= col; i++) {
            // Detach and scrap attached items in this row, so we can re-lay them again. The last
            // child view in the index can be the header so we just skip past it if it last.
            for (int j = 1; j <= mLayoutManager.getChildCount(); j++) {
                View child = mLayoutManager.getChildAt(mLayoutManager.getChildCount() - j);
                if (mLayoutManager.getPosition(child) == anchorPosition - i) {
                    markerLine = mLayoutManager.getDecoratedTop(child);
                    mLayoutManager.detachAndScrapViewAt(j, state.recycler);
                    break;
                }

                LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) child
                        .getLayoutParams();
                if (params.getTestedFirstPosition() != sd.firstPosition) {
                    break;
                }
            }
        }
        anchorPosition = anchorPosition - col;

        // Lay out rows to end.
        for (int i = anchorPosition; i < itemCount; i += mNumColumns) {
            if (markerLine > leadingEdge) {
                break;
            }

            LayoutState.View view = state.getView(i);
            if (view.getLayoutParams().getTestedFirstPosition() != sd.firstPosition) {
                state.cacheView(i, view.view);
                break;
            }

            int rowHeight = fillRow(markerLine, i, LayoutManager.Direction.END, true, sd, state);
            markerLine += rowHeight;
        }

        return markerLine;
    }

    @Override
    public int fillToStart(int leadingEdge, int markerLine, int anchorPosition, SectionData sd,
            LayoutState state) {
        final int firstContentPosition = sd.hasHeader ? sd.firstPosition + 1 : sd.firstPosition;

        // Check to see if we have to adjust for minimum section height. We don't if there is an
        // attached non-header view in this section.
        boolean applyMinHeight = false;
        for (int i = 0; i < mLayoutManager.getChildCount(); i++) {
            View check = mLayoutManager.getChildAt(0);
            LayoutManager.LayoutParams checkParams =
                    (LayoutManager.LayoutParams) check.getLayoutParams();
            if (checkParams.getTestedFirstPosition() != sd.firstPosition) {
                applyMinHeight = true;
                break;
            }

            if (!checkParams.isHeader) {
                applyMinHeight = false;
                break;
            }
        }

        // _ _ ^ a b
        final int col = (anchorPosition - firstContentPosition) % mNumColumns;
        for (int i = 1; i < mNumColumns - col; i++) {
            // Detach and scrap attached items in this row, so we can re-lay them again. The last
            // child view in the index can be the header so we just skip past it if it last.
            for (int j = 0; j < mLayoutManager.getChildCount(); j++) {
                View child = mLayoutManager.getChildAt(j);
                LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) child
                        .getLayoutParams();
                if (params.getTestedFirstPosition() != sd.firstPosition) {
                    break;
                }

                if (mLayoutManager.getPosition(child) == anchorPosition + i) {
                    mLayoutManager.detachAndScrapViewAt(j, state.recycler);
                    break;
                }
            }
        }
        // Ensure the anchor is the first item in the row.
        int columnAnchorPosition = anchorPosition - col;

        // Work out offset to marker line by measuring rows from the end. If section height is less
        // than min height, then adjust marker line and then lay out items.
        int measuredPositionsMarker = -1;
        int sectionHeight = 0;
        int minHeightOffset = 0;
        if (applyMinHeight) {
            for (int i = columnAnchorPosition; i >= 0; i -= mNumColumns) {
                LayoutState.View check = state.getView(i);
                state.cacheView(i, check.view);
                LayoutManager.LayoutParams checkParams = check.getLayoutParams();
                if (checkParams.getTestedFirstPosition() != sd.firstPosition) {
                    break;
                }

                int rowHeight = 0;
                for (int j = 0; j < mNumColumns && i + j <= anchorPosition; j++) {
                    LayoutState.View measure = state.getView(i + j);
                    state.cacheView(i + j, measure.view);
                    LayoutManager.LayoutParams measureParams = measure.getLayoutParams();
                    if (measureParams.getTestedFirstPosition() != sd.firstPosition) {
                        break;
                    }

                    if (measureParams.isHeader) {
                        continue;
                    }

                    measureChild(measure, sd);
                    rowHeight = Math.max(rowHeight,
                            mLayoutManager.getDecoratedMeasuredHeight(measure.view));
                }

                sectionHeight += rowHeight;
                measuredPositionsMarker = i;
                if (sectionHeight >= sd.minimumHeight) {
                    break;
                }
            }

            if (sectionHeight < sd.minimumHeight) {
                minHeightOffset = sectionHeight - sd.minimumHeight;
                markerLine += minHeightOffset;
            }
        }

        // Lay out rows to end.
        for (int i = columnAnchorPosition; i >= 0; i -= mNumColumns) {
            if (markerLine - minHeightOffset <= leadingEdge) {
                break;
            }

            LayoutState.View rowAnchor = state.getView(i);
            state.cacheView(i, rowAnchor.view);
            LayoutManager.LayoutParams params = rowAnchor.getLayoutParams();
            if (params.isHeader || params.getTestedFirstPosition() != sd.firstPosition) {
                break;
            }

            boolean measureRowItems = !applyMinHeight || i < measuredPositionsMarker;
            int rowHeight = fillRow(markerLine, i, LayoutManager.Direction.START, measureRowItems,
                    sd, state);
            markerLine -= rowHeight;
        }

        return markerLine;
    }

    @Override
    public int finishFillToEnd(int leadingEdge, View anchor, SectionData sd, LayoutState state) {
        final int anchorPosition = mLayoutManager.getPosition(anchor);
        final int markerLine = getLowestEdge(sd.firstPosition, mLayoutManager.getChildCount() - 1,
                mLayoutManager.getDecoratedBottom(anchor));

        return fillToEnd(leadingEdge, markerLine, anchorPosition + 1, sd, state);
    }

    @Override
    public int finishFillToStart(int leadingEdge, View anchor, SectionData sd, LayoutState state) {
        final int anchorPosition = mLayoutManager.getPosition(anchor);
        final int markerLine = mLayoutManager.getDecoratedTop(anchor);

        return fillToStart(leadingEdge, markerLine, anchorPosition - 1, sd, state);
    }

    @Override
    public LayoutManager.LayoutParams generateLayoutParams(LayoutManager.LayoutParams params) {
        return LayoutParams.from(params);
    }

    @Override
    public LayoutManager.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

    @Override
    public int getAnchorPosition(LayoutState state, SectionData sd, int position) {
        calculateColumnWidthValues(sd);

        int firstPosition = sd.firstPosition;
        LayoutState.View first = state.getView(firstPosition);
        if (first.getLayoutParams().isHeader) {
            firstPosition += 1;
        }
        state.cacheView(sd.firstPosition, first.view);
        return position - ((position - firstPosition) % mNumColumns);
    }

    @Override
    public int getLowestEdge(int sectionFirstPosition, int lastIndex, int defaultEdge) {
        int bottomMostEdge = 0;
        int leftPosition = mLayoutManager.getWidth();
        boolean foundItems = false;
        // Look from end to find children that are the lowest.
        for (int i = lastIndex; i >= 0; i--) {
            View look = mLayoutManager.getChildAt(i);
            LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) look.getLayoutParams();
            if (params.getTestedFirstPosition() != sectionFirstPosition) {
                break;
            }

            if (params.isHeader) {
                continue;
            }

            if (look.getLeft() < leftPosition) {
                leftPosition = look.getLeft();
            } else {
                break;
            }

            foundItems = true;
            bottomMostEdge = Math.max(bottomMostEdge, mLayoutManager.getDecoratedBottom(look));
        }

        return foundItems ? bottomMostEdge : defaultEdge;
    }

    public GridSLM init(SectionData sd) {
        super.init(sd);

        if (sd.headerParams instanceof LayoutParams) {
            LayoutParams params = (LayoutParams) sd.headerParams;
            int columnWidth = params.getColumnWidth();
            int numColumns = params.getNumColumns();
            if (columnWidth < 0 && numColumns < 0) {
                numColumns = DEFAULT_NUM_COLUMNS;
            }

            if (numColumns == AUTO_FIT) {
                setColumnWidth(columnWidth);
            } else {
                setNumColumns(numColumns);
            }
        }

        calculateColumnWidthValues(sd);

        return this;
    }

    /**
     * Fill a row.
     *
     * @param markerLine      Line indicating the top edge of the row.
     * @param anchorPosition  Position of the first view in the row.
     * @param direction       Direction of edge to fill towards.
     * @param measureRowItems Measure the row items.
     * @param sd              Section data.
     * @param state           Layout state.
     * @return The height of the new row.
     */
    public int fillRow(int markerLine, int anchorPosition, LayoutManager.Direction direction,
            boolean measureRowItems, SectionData sd, LayoutState state) {
        int rowHeight = 0;
        LayoutState.View[] views = new LayoutState.View[mNumColumns];
        for (int i = 0; i < mNumColumns; i++) {
            final int position = anchorPosition + i;
            if (position >= state.getRecyclerState().getItemCount()) {
                break;
            }

            LayoutState.View view = state.getView(position);
            if (view.getLayoutParams().getTestedFirstPosition() != sd.firstPosition) {
                state.cacheView(position, view.view);
                break;
            }

            if (measureRowItems) {
                measureChild(view, sd);
            } else {
                state.decacheView(i + anchorPosition);
            }
            rowHeight = Math.max(rowHeight, mLayoutManager.getDecoratedMeasuredHeight(view.view));
            views[i] = view;
        }

        boolean directionIsStart = direction == LayoutManager.Direction.START;
        if (directionIsStart) {
            markerLine -= rowHeight;
        }

        for (int i = 0; i < mNumColumns; i++) {
            int selectedView = directionIsStart ? mNumColumns - i - 1 : i;

            int col;
            if (state.isLTR) {
                col = directionIsStart ? mNumColumns - i - 1 : i;
            } else {
                col = directionIsStart ? i : mNumColumns - i - 1;
            }

            if (views[selectedView] == null) {
                continue;
            }
            layoutChild(views[selectedView], markerLine, col, rowHeight, sd, state);
            addView(views[selectedView], selectedView + anchorPosition, direction, state);
        }

        return rowHeight;
    }

    @Deprecated
    public void setColumnWidth(int minimumWidth) {
        mMinimumWidth = minimumWidth;
        mColumnsSpecified = false;
    }

    @Deprecated
    public void setNumColumns(int numColumns) {
        mNumColumns = numColumns;
        mMinimumWidth = 0;
        mColumnsSpecified = true;
    }

    private void calculateColumnWidthValues(SectionData sd) {
        int availableWidth = mLayoutManager.getWidth() - sd.contentStart - sd.contentEnd;
        if (!mColumnsSpecified) {
            if (mMinimumWidth <= 0) {
                mMinimumWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                        mContext.getResources().getDisplayMetrics());
            }
            mNumColumns = availableWidth / Math.abs(mMinimumWidth);
        }
        if (mNumColumns < 1) {
            mNumColumns = 1;
        }
        mColumnWidth = availableWidth / mNumColumns;
        if (mColumnWidth == 0) {
            Log.e("GridSection",
                    "Too many columns (" + mNumColumns + ") for available width" + availableWidth
                            + ".");
        }
    }

    /**
     * Layout out a view for the given column in a row. Views that have a height param of
     * MATCH_PARENT are fixed to the height of the row.
     *
     * @param child     View to lay out.
     * @param top       Line indicating the top edge of the row.
     * @param col       Column view is being placed into.
     * @param rowHeight Height of the row.
     * @param sd        Section data.
     * @param state     Layout state.
     */
    private void layoutChild(LayoutState.View child, int top, int col, int rowHeight,
            SectionData sd, LayoutState state) {
        final int height;
        if (child.getLayoutParams().height == LayoutManager.LayoutParams.MATCH_PARENT) {
            height = rowHeight;
        } else {
            height = mLayoutManager.getDecoratedMeasuredHeight(child.view);
        }
        final int width;

        if (col == mNumColumns - 1) {
            width = mLayoutManager.getDecoratedMeasuredWidth(child.view);
        } else {
            width = Math.min(mColumnWidth, mLayoutManager.getDecoratedMeasuredWidth(child.view));
        }

        final int bottom = top + height;
        final int left = (state.isLTR ? sd.contentStart : sd.contentEnd) + col * mColumnWidth;
        final int right = left + width;

        mLayoutManager.layoutDecorated(child.view, left, top, right, bottom);
    }

    /**
     * Measure view. A view is given an area as wide as a single column with an undefined height.
     *
     * @param child View to measure.
     * @param sd    Section data.
     */
    private void measureChild(LayoutState.View child, SectionData sd) {
        int widthOtherColumns = (mNumColumns - 1) * mColumnWidth;
        mLayoutManager.measureChildWithMargins(child.view,
                sd.marginStart + sd.marginEnd + widthOtherColumns,
                0);
    }

    public static class LayoutParams extends LayoutManager.LayoutParams {

        private int mNumColumns;

        private int mColumnWidth;

        public LayoutParams(int w, int h) {
            super(w, h);
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.superslim_GridSLM);
            mNumColumns = a.getInt(R.styleable.superslim_GridSLM_slm_grid_numColumns, AUTO_FIT);
            mColumnWidth =
                    a.getDimensionPixelSize(R.styleable.superslim_GridSLM_slm_grid_columnWidth, -1);
            a.recycle();
        }

        /**
         * <em>This constructor will be protected in version 0.5.</em>
         * <p>
         * Use {@link #from} instead.
         * </p>
         *
         * @param other Source layout params.
         */
        @Deprecated
        public LayoutParams(ViewGroup.MarginLayoutParams other) {
            super(other);
            init(other);
        }

        /**
         * <em>This constructor will be protected in version 0.5.</em>
         * <p>
         * Use {@link #from} instead as this constructor will not copy the margin params from the
         * source layout.
         * </p>
         *
         * @param other Source layout params.
         */
        @Deprecated
        public LayoutParams(ViewGroup.LayoutParams other) {
            super(other);
            init(other);
        }

        /**
         * Creates a new instance of {@link LayoutParams}.
         *
         * @param other Source layout params.
         * @return New grid layout params.
         */
        public static LayoutParams from(ViewGroup.LayoutParams other) {
            if (other == null) {
                Log.w("SuperSLiM", "Null value passed in call to GridSLM.LayoutParams.from().");
                return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            } else if (other instanceof ViewGroup.MarginLayoutParams) {
                return new LayoutParams((ViewGroup.MarginLayoutParams) other);
            } else {
                return new LayoutParams(other);
            }
        }

        public int getColumnWidth() {
            return mColumnWidth;
        }

        public void setColumnWidth(int columnWidth) {
            mColumnWidth = columnWidth;
        }

        public int getNumColumns() {
            return mNumColumns;
        }

        public void setNumColumns(int numColumns) {
            mNumColumns = numColumns;
        }

        private void init(ViewGroup.LayoutParams other) {
            if (other instanceof LayoutParams) {
                final LayoutParams lp = (LayoutParams) other;
                mNumColumns = lp.mNumColumns;
                mColumnWidth = lp.mColumnWidth;
            } else {
                mNumColumns = AUTO_FIT;
                mColumnWidth = -1;
            }
        }
    }
}
