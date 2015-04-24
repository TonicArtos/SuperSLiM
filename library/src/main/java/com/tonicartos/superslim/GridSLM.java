package com.tonicartos.superslim;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
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

    public static final int ID = LayoutManager.SECTION_MANAGER_GRID;

    private static final int AUTO_FIT = -1;

    private static final int DEFAULT_NUM_COLUMNS = 1;

    private static final String NUM_COLUMNS = "num_columns";

    private static final String COLUMN_WIDTH = "column_width";

    private final Context mContext;

    private int mNumColumns = 0;

    private int mColumnWidth;

    public GridSLM(Context context) {
        mContext = context;
    }

    /**
     * Fill a row.
     *
     * @param markerLine      Line indicating the top edge of the row.
     * @param anchorPosition  Position of the first view in the row.
     * @param direction       Direction of edge to fill towards.
     * @param measureRowItems Measure the row items.
     * @param sd              Section data.
     * @param recycler        Layout recycler.
     * @return The height of the new row.
     */
    public int fillRow(int markerLine, int anchorPosition, @LayoutManager.Direction int direction,
            boolean measureRowItems, SectionData sd, LayoutHelper helper, Recycler recycler,
            RecyclerView.State state) {
        int rowHeight = 0;
        View[] views = new View[mNumColumns];
        for (int i = 0; i < mNumColumns; i++) {
            final int position = anchorPosition + i;
            if (position >= state.getItemCount()) {
                break;
            }

            View view = recycler.getView(position);
            if (!sd.containsItem(helper.getPosition(view))) {
                recycler.cacheView(position, view);
                break;
            }

            if (measureRowItems) {
                measureChild(view, helper);
            } else {
                recycler.decacheView(i + anchorPosition);
            }
            rowHeight = Math.max(rowHeight, helper.getMeasuredHeight(view));
            views[i] = view;
        }

        boolean directionIsStart = direction == LayoutManager.DIRECTION_START;
        if (directionIsStart) {
            markerLine -= rowHeight;
        }

        for (int i = 0; i < mNumColumns; i++) {
            int col = directionIsStart ? mNumColumns - i - 1 : i;
            if (views[col] == null) {
                continue;
            }
            layoutChild(views[col], markerLine, col, rowHeight, helper);
            Log.d("add grid view", "col " + col + "  position " + helper.getPosition(views[col]));
            addView(views[col], directionIsStart ? 0 : -1, helper, recycler);
        }

        return rowHeight;
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(RecyclerView.LayoutParams params) {
        return new LayoutParams(params);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

//    @Override
//    public int finishFillToEnd(View anchor, SectionData sectionData, LayoutHelper helper,
//            Recycler recycler, RecyclerView.State state) {
//        final int anchorPosition = helper.getPosition(anchor);
//        final int markerLine = getLowestEdge(sectionData.firstPosition,
// helper.getChildCount() - 1,
//                helper.getBottom(anchor));
//
//        return fillToEnd(leadingEdge, markerLine, anchorPosition + 1, sectionData, recycler);
//    }

    @Override
    public void getEdgeStates(Rect outRect, View child, SectionData sd, int layoutDirection) {
        LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) child.getLayoutParams();
        final int position = params.getViewPosition();

        int firstContentPosition = (sd.hasHeader ?
                Math.min(sd.firstPosition + 1, sd.lastPosition) :
                sd.firstPosition);
        final int column = (position - firstContentPosition) % mNumColumns;
        final int ltrColumn = adjustColumnForLayoutDirection(column, layoutDirection);

        outRect.left = ltrColumn == 0 ?
                ItemDecorator.EXTERNAL : ItemDecorator.INTERNAL;
        outRect.right = ltrColumn == mNumColumns - 1 ?
                ItemDecorator.EXTERNAL : ItemDecorator.INTERNAL;

        outRect.top = position - column == firstContentPosition ?
                ItemDecorator.EXTERNAL : ItemDecorator.INTERNAL;
        outRect.bottom = position + (mNumColumns - column) > sd.lastPosition ?
                ItemDecorator.EXTERNAL : ItemDecorator.INTERNAL;
    }

    @Override
    public int getLowestEdge(int lastIndex, int altEdge, SectionData sectionData,
            LayoutQueryHelper helper) {
        final boolean isLtr = helper.getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_LTR;
        int bottomMostEdge = altEdge;
        int startPosition = isLtr ? helper.getWidth() : 0;
        boolean foundItems = false;
        // Look from end to find children that are the lowest.
        for (int i = lastIndex; i >= 0; i--) {
            View look = helper.getChildAt(i);
            LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) look.getLayoutParams();
            if (!sectionData.containsItem(params.getViewPosition())) {
                break;
            }

            if (params.isHeader()) {
                continue;
            }

            if (isLtr && look.getLeft() < startPosition) {
                startPosition = look.getLeft();
            } else if (!isLtr && look.getRight() > startPosition) {
                startPosition = look.getRight();
            } else {
                break;
            }

            foundItems = true;
            bottomMostEdge = Math.max(bottomMostEdge, helper.getBottom(look));
        }

        return foundItems ? bottomMostEdge : altEdge;
    }

    @Override
    public int onComputeHeaderOffset(int firstVisiblePosition, SectionData sd, LayoutHelper helper,
            Recycler recycler) {
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
            for (int col = 0; col < mNumColumns; col++) {
                View child = recycler.getView(position + col);
                measureChild(child, helper);
                rowHeight =
                        Math.max(rowHeight, helper.getMeasuredHeight(child));
                recycler.cacheView(position + col, child);
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
    public void onInit(Bundle savedConfig, SectionData sectionData, LayoutQueryHelper helper) {
        if (savedConfig != null) {
            mNumColumns = savedConfig.getInt(NUM_COLUMNS);
            mColumnWidth = savedConfig.getInt(COLUMN_WIDTH);
        } else {
            int mMinimumWidth = 0;
            boolean mColumnsSpecified = false;

            if (sectionData.getSectionParams() instanceof LayoutParams) {
                LayoutParams params = (LayoutParams) sectionData.getSectionParams();
                int columnWidth = params.getColumnWidth();
                int numColumns = params.getNumColumns();
                if (columnWidth < 0 && numColumns < 0) {
                    numColumns = DEFAULT_NUM_COLUMNS;
                }

                if (numColumns == AUTO_FIT) {
                    mMinimumWidth = columnWidth;
                    mColumnsSpecified = false;
                } else {
                    mNumColumns = numColumns;
                    mMinimumWidth = 0;
                    mColumnsSpecified = true;
                }
            }

            int availableWidth = helper.getWidth();
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
                        "Too many columns (" + mNumColumns + ") for available width "
                                + availableWidth
                                + ".");
            }

            // Store new configuration for this section.
            Bundle config = new Bundle();
            config.putInt(NUM_COLUMNS, mNumColumns);
            config.putInt(COLUMN_WIDTH, mColumnWidth);
            saveConfiguration(sectionData, config);
        }
    }

    @Override
    protected int onFillSubsectionsToEnd(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state) {
        return 0;
    }

    @Override
    protected int onFillSubsectionsToStart(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state) {
        return 0;
    }

    @Override
    protected int onFillToEnd(int anchorPosition, SectionData sectionData, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        int markerLine = 0;
        final int leadingEdge = helper.getLeadingEdge();
        if (markerLine >= leadingEdge) {
            return markerLine;
        }

        final int itemCount = state.getItemCount();
        if (anchorPosition >= itemCount) {
            return markerLine;
        }

        View anchor = recycler.getView(anchorPosition);
        recycler.cacheView(anchorPosition, anchor);
        if (!sectionData.containsItem(helper.getPosition(anchor))) {
            return markerLine;
        }

        final int firstContentPosition = sectionData.hasHeader ?
                sectionData.firstPosition + 1 : sectionData.firstPosition;

        // Ensure the anchor is the first item in the row.
        final int col = (anchorPosition - firstContentPosition) % mNumColumns;
        for (int i = 1; i <= col; i++) {
            // Detach and scrap attached items in this row, so we can re-lay them again. The last
            // child view in the index can be the header so we just skip past it if it last.
            for (int j = 1; j <= helper.getChildCount(); j++) {
                View child = helper.getChildAt(helper.getChildCount() - j);
                if (helper.getPosition(child) == anchorPosition - i) {
                    markerLine = helper.getTop(child);
                    helper.detachAndScrapViewAt(j, recycler);
                    break;
                }

                if (!sectionData.containsItem(helper.getPosition(child))) {
                    break;
                }
            }
        }
        anchorPosition = anchorPosition - col;

        // Lay out rows to end.
        for (int i = anchorPosition; i < itemCount; i += mNumColumns) {
            if (markerLine >= leadingEdge) {
                break;
            }

            View view = recycler.getView(i);
            if (!sectionData.containsItem(helper.getPosition(view))) {
                recycler.cacheView(i, view);
                break;
            }
            recycler.cacheView(i, view);

            int rowHeight = fillRow(markerLine, i, LayoutManager.DIRECTION_END, true, sectionData,
                    helper, recycler, state);
            markerLine += rowHeight;
        }

        return markerLine;
    }

    @Override
    protected int onFillToStart(int anchorPosition, SectionData sd, LayoutHelper helper,
            Recycler recycler, RecyclerView.State state) {
        if (anchorPosition == sd.firstPosition && sd.hasHeader) {
            return 0;
        }

        final int leadingEdge = helper.getLeadingEdge();
        int markerLine = 0;
        final int firstContentPosition = sd.hasHeader ? sd.firstPosition + 1 : sd.firstPosition;

        // Check to see if we have to adjust for minimum section height. We don't if there is an
        // attached non-header view in this section.
        boolean applyMinHeight = false;
        for (int i = 0; i < helper.getChildCount(); i++) {
            View check = helper.getChildAt(0);
            LayoutManager.LayoutParams checkParams =
                    (LayoutManager.LayoutParams) check.getLayoutParams();
            if (!sd.containsItem(checkParams.getViewPosition())) {
                applyMinHeight = true;
                break;
            }

            if (!checkParams.isHeader()) {
                applyMinHeight = false;
                break;
            }
        }

        // _ _ ^ a b
        final int col = (anchorPosition - firstContentPosition) % mNumColumns;
        boolean forceFirstRow = col != mNumColumns - 1;
        for (int i = 1; i < mNumColumns - col; i++) {
            // Detach and scrap attached items in this row, so we can re-lay them again. The last
            // child view in the index can be the header so we just skip past it if it last.
            for (int j = 0; j < helper.getChildCount(); j++) {
                View child = helper.getChildAt(j);
                if (!sd.containsItem(helper.getPosition(child))) {
                    break;
                }

                if (helper.getPosition(child) == anchorPosition + i) {
                    markerLine = Math.max(markerLine, helper.getBottom(child));
                    helper.detachAndScrapViewAt(j, recycler);
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
                View check = recycler.getView(i);
                recycler.cacheView(i, check);
                if (!sd.containsItem(helper.getPosition(check))) {
                    break;
                }

                int rowHeight = 0;
                for (int j = 0; j < mNumColumns && i + j <= anchorPosition; j++) {
                    View measure = recycler.getView(i + j);
                    recycler.cacheView(i + j, measure);
                    LayoutManager.LayoutParams measureParams =
                            (LayoutManager.LayoutParams) measure.getLayoutParams();
                    if (!sd.containsItem(measureParams.getViewPosition())) {
                        break;
                    }

                    if (measureParams.isHeader()) {
                        continue;
                    }

                    measureChild(measure, helper);
                    rowHeight = Math.max(rowHeight, helper.getMeasuredHeight(measure));
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
        for (int i = columnAnchorPosition; i >= firstContentPosition; i -= mNumColumns) {
            if (markerLine - minHeightOffset < leadingEdge && !forceFirstRow) {
                break;
            }
            forceFirstRow = false;

            View rowAnchor = recycler.getView(i);
            recycler.cacheView(i, rowAnchor);
            LayoutManager.LayoutParams params =
                    (LayoutManager.LayoutParams) rowAnchor.getLayoutParams();
            if (params.isHeader() || !sd.containsItem(params.getViewPosition())) {
                break;
            }

            boolean measureRowItems = !applyMinHeight || i < measuredPositionsMarker;
            int rowHeight = fillRow(markerLine, i, LayoutManager.DIRECTION_START, measureRowItems,
                    sd, helper, recycler, state);
            markerLine -= rowHeight;
        }

        return markerLine;
    }

    private int adjustColumnForLayoutDirection(int col, int layoutDirection) {
        if (layoutDirection != ViewCompat.LAYOUT_DIRECTION_LTR) {
            col = mNumColumns - 1 - col;
        }
        return col;
    }

    /**
     * Layout out a view for the given column in a row. Views that have a height param of
     * MATCH_PARENT are fixed to the height of the row.
     *
     * @param child     View to lay out.
     * @param top       Line indicating the top edge of the row.
     * @param col       Column view is being placed into.
     * @param rowHeight Height of the row.
     */
    private void layoutChild(View child, int top, int col, int rowHeight, LayoutHelper helper) {
        final int height;
        if (child.getLayoutParams().height == LayoutManager.LayoutParams.MATCH_PARENT) {
            height = rowHeight;
        } else {
            height = helper.getMeasuredHeight(child);
        }
        final int width = helper.getMeasuredWidth(child);

        col = adjustColumnForLayoutDirection(col, helper.getLayoutDirection());

        final int bottom = top + height;
        final int left = col * mColumnWidth;
        final int right = left + width;

        helper.layoutChild(child, left, top, right, bottom);
    }

    /**
     * Measure view. A view is given an area as wide as a single column with an undefined height.
     *
     * @param child  View to measure.
     * @param helper Layout helper.
     */
    private void measureChild(View child, LayoutHelper helper) {
        final int widthOtherColumns = (mNumColumns - 1) * mColumnWidth;
        helper.measureChild(child, widthOtherColumns, 0);
    }

    public static class LayoutParams extends LayoutManager.LayoutParams {

        private int mNumColumns;

        private int mColumnWidth;

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
         * <br/><br/>
         * Use {@link #from} instead.
         */
        @Deprecated
        public LayoutParams(ViewGroup.MarginLayoutParams other) {
            super(other);
            init(other);
        }

        /**
         * <em>This constructor will be protected in version 0.5.</em>
         * <br/><br/>
         * Use {@link #from} instead as this constructor will not copy the margin params from the
         * source layout.
         */
        @Deprecated
        public LayoutParams(ViewGroup.LayoutParams other) {
            super(other);
            init(other);
        }

        /**
         * Creates a new instance of {@link LayoutParams}.
         */
        public static LayoutParams from(@NonNull ViewGroup.LayoutParams other) {
            if (other instanceof ViewGroup.MarginLayoutParams) {
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

    public class SlmConfig extends SectionLayoutManager.SlmConfig {

        int numColumns;

        int columnWidth;

        public SlmConfig(int marginStart, int marginEnd, String sectionManager) {
            super(marginStart, marginEnd, sectionManager);
        }

        public SlmConfig(int marginStart, int marginEnd,
                @LayoutManager.SectionManager int sectionManager) {
            super(marginStart, marginEnd, sectionManager);
        }

        public SlmConfig setColumnWidth(int columnWidth) {
            this.columnWidth = columnWidth;
            return this;
        }

        public SlmConfig setNumColumns(int numColumns) {
            this.numColumns = numColumns;
            return this;
        }
    }
}
