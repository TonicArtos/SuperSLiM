package com.tonicartos.superslim;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import java.util.Stack;

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

    private static final java.lang.String CONFIG_TYPE = "grid_config";

    private final Context mContext;

    private int mNumColumns = 0;

    private int mColumnWidth;

    public GridSLM(Context context) {
        mContext = context;
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(RecyclerView.LayoutParams params) {
        return new LayoutParams(params);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

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
    public int getHighestEdge(int firstIndex, int defaultEdge, SectionData sectionData,
            LayoutQueryHelper helper) {
        // FIXME: find highest edge for subsections.
        return super.getHighestEdge(firstIndex, defaultEdge, sectionData, helper);
    }

    @Override
    public int getLowestEdge(int lastIndex, int altEdge, SectionData sectionData,
            LayoutQueryHelper helper) {
        int bottomMostEdge = altEdge;

        final int childCount = helper.getChildCount();
        if (childCount == 0) {
            return altEdge;
        }

        int startIndex;
        if (sectionData.hasHeader && helper.getPosition(helper.getChildAt(childCount - 1))
                == sectionData.firstPosition) {
            startIndex = 1;
        } else {
            startIndex = 0;
        }

        // Look from end to find children that are the lowest.
        if (sectionData.subsections != null && sectionData.subsections.size() > 0) {
            bottomMostEdge = getSubsectionRowBottom(startIndex, lastIndex, bottomMostEdge,
                    sectionData, helper);
        } else {
            bottomMostEdge = getRowBottom(startIndex, lastIndex, bottomMostEdge, sectionData,
                    helper);
        }

        return bottomMostEdge;
    }

    @Override
    public SectionLayoutManager newInstance() {
        return new GridSLM(mContext);
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
    protected int onFillSubsectionsToEnd(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state) {
        final int leadingEdge = helper.getLeadingEdge();
        int markerLine = 0;
        if (markerLine >= leadingEdge) {
            return markerLine;
        }

        int childCount = helper.getChildCount();
        if (childCount > 0 && sectionData.containsItem(helper.getChildAt(childCount - 1))) {
            // Finish current row.
            markerLine =
                    finishSubsectionRowToEnd(anchorPosition, sectionData, helper, recycler, state);
            if (markerLine >= leadingEdge) {
                return markerLine;
            }
        }

        // Fill remaining rows/area.
        markerLine = fillSubsectionRowsToEnd(markerLine, sectionData, helper, recycler, state);

        return markerLine;
    }

    @Override
    protected int onFillSubsectionsToStart(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state) {
        int markerLine = 0;
        final int leadingEdge = helper.getLeadingEdge();
        if (markerLine <= leadingEdge) {
            return markerLine;
        }

        markerLine = finishSubsectionRowToStart(anchorPosition, sectionData, helper, recycler,
                state);
        if (markerLine <= leadingEdge) {
            return markerLine;
        }

        if (anchorPosition == sectionData.firstPosition && sectionData.hasHeader) {
            return markerLine;
        }

        markerLine = fillSubsectionRowsToStart(markerLine, sectionData, helper, recycler, state);

        return markerLine;
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

    @Override
    public void onInit(Bundle savedConfig, SectionData sectionData, LayoutQueryHelper helper) {
        if (savedConfig != null && savedConfig.getInt(CONFIG_TYPE) == GridSLM.ID) {
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
            config.putInt(CONFIG_TYPE, GridSLM.ID);
            config.putInt(NUM_COLUMNS, mNumColumns);
            config.putInt(COLUMN_WIDTH, mColumnWidth);
            saveConfiguration(sectionData, config);
        }
    }

    @Override
    protected void onPreTrimAtStartEdge(int firstVisibleIndex, SectionData sectionData,
            LayoutTrimHelper helper) {
        super.onPreTrimAtStartEdge(firstVisibleIndex, sectionData, helper);

        if (sectionData.subsections == null || sectionData.subsections.size() == 0) {
            return;
        }

        final int anchorPosition = helper.getPosition(helper.getChildAt(0));
        int anchorSection = -1;
        for (int i = 0; i < sectionData.subsections.size(); i++) {
            if (sectionData.subsections.get(i).containsItem(anchorPosition)) {
                anchorSection = i;
                break;
            }
        }

        if (anchorSection == -1) {
            return;
        }

        // Must sticky subsection content to keep visual consistency with the opposite scroll
        // motion.

        final int stickyEdge = helper.getStickyEdge();

        final SubsectionColumnData[] colData = getRowColumnData(sectionData, helper, anchorSection);

        // Calculate row bottom.
        int rowBottom = 0;
        for (SubsectionColumnData col : colData) {
            View child = helper.getChildAt(col.startIndex + col.childCount - 1);
            col.bottom = helper.getBottom(child);
            rowBottom = Math.max(rowBottom, col.bottom);
        }

        // Try to offset column views to sticky edge, but don't shift below row bottom.
        for (SubsectionColumnData col : colData) {
            int delta2RowBottom = rowBottom - col.bottom;
            int delta2StickyEdge = stickyEdge - helper.getTop(helper.getChildAt(col.startIndex));
            int delta = Math.min(delta2RowBottom, delta2StickyEdge);
            for (int i = 0; i < col.childCount; i++) {
                helper.getChildAt(i + col.startIndex).offsetTopAndBottom(delta);
            }
        }
    }

    private int adjustColumnForLayoutDirection(int col, int layoutDirection) {
        if (layoutDirection != ViewCompat.LAYOUT_DIRECTION_LTR) {
            col = mNumColumns - 1 - col;
        }
        return col;
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
    private int fillRow(int markerLine, int anchorPosition, @LayoutManager.Direction int direction,
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

    private int fillSubsectionRowToEnd(int markerLine, int sectionIndex, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state) {
        final int leadingEdge = helper.getLeadingEdge();
        final int stickyEdge = helper.getStickyEdge();
        final int rowTop = markerLine;
        final int unavailable = (mNumColumns - 1) * mColumnWidth;
        final LayoutHelper subHelper = helper.getSubsectionLayoutHelper();
        for (int i = 0; i < mNumColumns && i < sectionData.subsections.size(); i++) {
            final int columnPosition = i * mColumnWidth;
            final SectionData subSd = sectionData.subsections.get(sectionIndex + i);
            subHelper.setUnavailableWidth(unavailable);
            subSd.init(subHelper, recycler);
            subHelper.init(subSd, columnPosition, unavailable, rowTop, leadingEdge, stickyEdge);
            final SectionLayoutManager slm = helper.getSlm(subSd, subHelper);
            markerLine = Math.max(markerLine,
                    slm.beginFillToEnd(subSd.firstPosition, subSd, subHelper, recycler, state));
        }
        subHelper.recycle();
        return markerLine;
    }

    private int fillSubsectionRowToStart(int markerLine, int anchorSectionIndex,
            SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state) {
        final SubsectionColumnData[] colData = new SubsectionColumnData[mNumColumns];
        final int leadingEdge = helper.getLeadingEdge();
        final int stickyEdge = helper.getStickyEdge();
        final int rowBottom = markerLine;
        final int unavailable = (mNumColumns - 1) * mColumnWidth;

        final LayoutHelper subHelper = helper.getSubsectionLayoutHelper();
        for (int i = 0; i < mNumColumns && i < sectionData.subsections.size(); i++) {
            final int sectionIndex = anchorSectionIndex - i;
            final int column = sectionIndex % mNumColumns;
            final int columnPosition = column * mColumnWidth;

            final SectionData subSd = sectionData.subsections.get(sectionIndex);
            subSd.init(subHelper, recycler);
            subHelper.init(subSd, columnPosition, unavailable, rowBottom, leadingEdge, stickyEdge);
            final SectionLayoutManager slm = helper.getSlm(subSd, subHelper);
            final int countBefore = helper.getChildCount();
            markerLine = Math.max(markerLine,
                    slm.beginFillToStart(subSd.firstPosition, subSd, subHelper, recycler, state));
            final int sectionCount = helper.getChildCount() - countBefore;

            // Update column index caching.
            for (int j = 0; j < i; j++) {
                colData[column + 1 + j].startIndex += sectionCount;
            }
            colData[column] = new SubsectionColumnData(0, sectionCount);
        }
        subHelper.recycle();

        stickySubsections(markerLine, helper, colData, stickyEdge);

        return markerLine;
    }

    private int fillSubsectionRowsToEnd(int markerLine, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state) {
        final int leadingEdge = helper.getLeadingEdge();

        int anchorSection = 0;
        if (helper.getChildCount() > 0) {
            int anchorPosition = helper.getPosition(helper.getChildAt(helper.getChildCount() - 1));
            for (int i = 0; i < sectionData.subsections.size(); i++) {
                final SectionData sd = sectionData.subsections.get(i);
                if (sd.containsItem(anchorPosition)) {
                    anchorSection = i + 1;
                    break;
                }
            }
        }

        // Fill rows.
        for (int i = anchorSection; markerLine < leadingEdge && i < sectionData.subsections.size();
                i += mNumColumns) {
            markerLine =
                    fillSubsectionRowToEnd(markerLine, i, sectionData, helper, recycler, state);
        }

        return markerLine;
    }

    private int fillSubsectionRowsToStart(int markerLine, SectionData sectionData, LayoutHelper
            helper, Recycler recycler, RecyclerView.State state) {
        final int leadingEdge = helper.getLeadingEdge();

        int anchorSection = 0;
        if (helper.getChildCount() > 0) {
            int anchorPosition = helper.getPosition(helper.getChildAt(0));
            for (int i = 0; i < sectionData.subsections.size(); i++) {
                final SectionData sd = sectionData.subsections.get(i);
                if (sd.containsItem(anchorPosition)) {
                    anchorSection = i - 1;
                    break;
                }
            }
        }

        // Fill rows.
        for (int i = anchorSection; markerLine < leadingEdge && i < sectionData.subsections.size();
                i -= mNumColumns) {
            markerLine =
                    fillSubsectionRowToStart(markerLine, i, sectionData, helper, recycler, state);
        }

        return markerLine;
    }

    /**
     * Finish filling the current subsection row to the end.
     *
     * @return Line up to which content was filled.
     */
    private int finishSubsectionRowToEnd(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state) {
        final SparseArray<Stack<View>> detachedViews = new SparseArray<>();
        final int leadingEdge = helper.getLeadingEdge();
        final int stickyEdge = helper.getStickyEdge();
        int markerLine = 0;
        if (helper.getChildCount() == 0) {
            return markerLine;
        }

        // Find out the last filled column position and the matched subsection.
        int lastPosition = helper.getChildCount() - 1;
        View child = helper.getChildAt(lastPosition);
        int anchorSubsection = 0;
        for (int i = 0; i < sectionData.subsections.size(); i++) {
            if (sectionData.subsections.get(i).containsItem(child)) {
                anchorSubsection = i;
            }
        }
        final int anchorColumn = anchorSubsection % mNumColumns;

        // Detach views until the first column is reached.
        for (int i = 0; i < anchorColumn; i++) {
            SectionData columnSubsection = sectionData.subsections.get(anchorSubsection - i);
            detachedViews.put(anchorColumn - i, new Stack<View>());
            while (columnSubsection.containsItem(child)) {
                detachedViews.get(anchorColumn - i).push(child);
                helper.detachView(child);
                lastPosition -= 1;
                child = helper.getChildAt(lastPosition);
            }
        }

        final LayoutHelper subHelper = helper.getSubsectionLayoutHelper();
        final int unavailable = (mNumColumns - 1) * mColumnWidth;

        // Finish fill each column.
        for (int i = 0; i < mNumColumns; i++) {
            // Reattach detached views for columns after first one.
            if (i > 0) {
                Stack<View> views = detachedViews.get(i);
                if (views == null || views.size() == 0) {
                    // No more to be down in finishing filling the row.
                    break;
                }

                while (views.size() > 0) {
                    helper.attachView(views.pop());
                }
            }

            final View last = helper.getChildAt(helper.getChildCount() - 1);
            final int currentSubsection = i - anchorColumn + anchorSubsection;
            final int columnPosition = i * mColumnWidth;
            final SectionData subSd = sectionData.subsections.get(currentSubsection);

            subSd.init(subHelper, recycler);
            subHelper.init(subSd, columnPosition, unavailable, helper.getBottom(last), leadingEdge,
                    stickyEdge);

            final SectionLayoutManager slm = helper.getSlm(subSd, subHelper);
            markerLine = Math.max(markerLine,
                    slm.finishFillToEnd(subSd.firstPosition, subSd, subHelper, recycler, state));
        }

        subHelper.recycle();

        return markerLine;
    }

    private int finishSubsectionRowToStart(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state) {
        //TODO: RTL support.
        final SubsectionColumnData[] colData = new SubsectionColumnData[mNumColumns];
        final SparseArray<Stack<View>> detachedViews = new SparseArray<>();
        final int leadingEdge = helper.getLeadingEdge();
        final int stickyEdge = helper.getStickyEdge();
        int markerLine = 0;
        if (helper.getChildCount() == 0) {
            return markerLine;
        }

        // Find out the first filled column position and the matched subsection.
        View child = helper.getChildAt(0);
        int anchorSubsection = 0;
        for (int i = 0; i < sectionData.subsections.size(); i++) {
            if (sectionData.subsections.get(i).containsItem(child)) {
                anchorSubsection = i;
            }
        }

        // Detach and cache views to last displayed column in row, or last section. Calculate last
        // column to anchor the fill from.
        int anchorColumn = 0;
        for (int i = 0;
                i < mNumColumns && anchorSubsection + i < sectionData.subsections.size();
                i++) {
            anchorColumn = i;
            if (i == mNumColumns - 1
                    || anchorSubsection + i == sectionData.subsections.size() - 1) {
                break;
            }

            final SectionData columnSubsection = sectionData.subsections.get(anchorSubsection + i);
            detachedViews.put(i, new Stack<View>());
            while (columnSubsection.containsItem(child)) {
                detachedViews.get(i).push(child);
                helper.detachView(child);
                child = helper.getChildAt(0);
            }
        }

        // For each column from the end. Reattach any detached views for the column and then finish
        // filling the subsection.
        final LayoutHelper subHelper = helper.getSubsectionLayoutHelper();
        final int unavailable = (mNumColumns - 1) * mColumnWidth;

        // Finish fill each column.
        for (int i = 0; i < mNumColumns && anchorColumn - i >= 0; i++) {
            // Reattach detached views for columns after first one.
            if (i > 0) {
                Stack<View> views = detachedViews.get(i);
                if (views == null || views.size() == 0) {
                    // No more to be down in finishing filling the row.
                    break;
                }

                while (views.size() > 0) {
                    helper.attachView(views.pop(), 0);
                }
            }

            final View first = helper.getChildAt(0);
            final int column = anchorColumn - i;
            final int currentSubsection = column + anchorSubsection;
            final int columnPosition = (column) * mColumnWidth;
            final SectionData subSd = sectionData.subsections.get(currentSubsection);

            subSd.init(subHelper, recycler);
            subHelper.init(subSd, columnPosition, unavailable, helper.getTop(first), leadingEdge,
                    stickyEdge);

            final SectionLayoutManager slm = helper.getSlm(subSd, subHelper);
            final int countBefore = helper.getChildCount();
            markerLine = Math.max(markerLine,
                    slm.finishFillToStart(subSd.firstPosition, subSd, subHelper, recycler, state));
            final int sectionCount = helper.getChildCount() - countBefore;

            // Update column index caching.
            for (int j = 0; j < i; j++) {
                colData[column + 1 + j].startIndex += sectionCount;
            }
            colData[(column)] = new SubsectionColumnData(0, sectionCount);
        }
        subHelper.recycle();

        stickySubsections(markerLine, helper, colData, stickyEdge);

        return markerLine;
    }

    private int getRowBottom(int startIndex, int lastIndex, int bottomMostEdge,
            SectionData sectionData,
            LayoutQueryHelper helper) {
        final boolean isLtr = helper.getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_LTR;
        int startPosition = isLtr ? helper.getWidth() : 0;

        for (int i = startIndex; i < lastIndex + 1; i++) {
            View look = helper.getChildAt(lastIndex - i);
            LayoutManager.LayoutParams params =
                    (LayoutManager.LayoutParams) look.getLayoutParams();
            if (!sectionData.containsItem(params.getViewLayoutPosition())) {
                break;
            }

            // Don't know how many actual columns there are so use the edge position to know
            // when we skip to another row.
            if (isLtr && look.getLeft() < startPosition) {
                startPosition = look.getLeft();
            } else if (!isLtr && look.getRight() > startPosition) {
                startPosition = look.getRight();
            } else {
                break;
            }

            bottomMostEdge = Math.max(bottomMostEdge, helper.getBottom(look));
        }
        return bottomMostEdge;
    }

    private SubsectionColumnData[] getRowColumnData(SectionData sectionData,
            LayoutTrimHelper helper,
            int anchorSection) {
        SubsectionColumnData[] colData = new SubsectionColumnData[mNumColumns];
        for (int i = 0; i < colData.length; i++) {
            colData[i] = new SubsectionColumnData(0, 0);
        }

        final int childCount = helper.getChildCount();
        for (int i = 0, column = 0; i < childCount && column < mNumColumns; i++) {
            final View child = helper.getChildAt(i);
            if (!sectionData.containsItem(child) ||
                    (helper.getPosition(child) == sectionData.firstPosition
                            && sectionData.hasHeader)) {
                // End of section.
                break;
            }

            if (sectionData.subsections.get(anchorSection + column).containsItem(child)) {
                colData[column].childCount += 1;
            } else {
                // End of subsection/column.
                column += 1;
                if (column < mNumColumns) {
                    colData[column].startIndex = i;
                    i -= 1;
                }
            }
        }

        return colData;
    }

    private int getSubsectionRowBottom(int startIndex, int lastIndex, int bottomMostEdge,
            SectionData sectionData,
            LayoutQueryHelper helper) {
        final int checkLimit = mNumColumns; // Must check at most this many subsections.
        final int numSubsections = sectionData.subsections.size();
        final int childCount = helper.getChildCount();

        for (int checkAttempt = 0, i = startIndex; checkAttempt < checkLimit && i < childCount;
                checkAttempt++) {
            // Find last view for next subsection column.
            final int lookIndex = lastIndex - i;
            View look = helper.getChildAt(lookIndex);
            SectionData selectedSubsection = null;
            for (int j = 0; j < numSubsections; j++) {
                SectionData subSd = sectionData.subsections.get(numSubsections - 1 - j);
                if (subSd.containsItem(look)) {
                    selectedSubsection = subSd;
                    break;
                }
            }

            if (selectedSubsection == null) {
                break;
            }

            SectionLayoutManager subSlm = helper.getSlm(selectedSubsection, helper);
            bottomMostEdge = Math.max(bottomMostEdge,
                    subSlm.getLowestEdge(lookIndex, helper.getBottom(look), selectedSubsection,
                            helper));

            // Skip all other attached views in selected subsection.
            for (; i < childCount; i++) {
                final View skip = helper.getChildAt(lastIndex - i);
                if (!selectedSubsection.containsItem(skip)) {
                    i -= 1;
                    break;
                }
            }
        }
        return bottomMostEdge;
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

    /**
     * Sticky subsections within row if the subsection is complete and the top edge of the
     * subsection is below the row's displayed top edge.
     *
     * @param rowTop     Top edge of row.
     * @param helper     Layout helper.
     * @param colData    Column counts and indices.
     * @param stickyEdge Section sticky edge.
     */
    private void stickySubsections(int rowTop, LayoutHelper helper, SubsectionColumnData[] colData,
            int stickyEdge) {
        final int topEdge = rowTop < stickyEdge ? stickyEdge : rowTop;
        for (int i = 0; i < mNumColumns; i++) {
            if (colData[i] == null) {
                break;
            }

            final int childCount = colData[i].childCount;
            if (childCount == 0) {
                break;
            }

            final int colStart = colData[i].startIndex;
            final int subsectionTop = helper.getTop(helper.getChildAt(colStart));
            // The subsection will only be below the top Edge if it has finished early.
            if (subsectionTop > topEdge) {
                final int offset = topEdge - subsectionTop;
                for (int j = colStart; j < childCount; j++) {
                    helper.getChildAt(j).offsetTopAndBottom(offset);
                }
            }
        }
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
        public static LayoutParams from(ViewGroup.LayoutParams other) {
            if (other == null) {
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

    public static class SlmConfig extends SectionLayoutManager.SlmConfig {

        int numColumns = AUTO_FIT;

        int columnWidth;

        public SlmConfig(int marginStart, int marginEnd, String sectionManager) {
            super(marginStart, marginEnd, sectionManager);
        }

        public SlmConfig(int marginStart, int marginEnd,
                @LayoutManager.SectionManager int sectionManager) {
            super(marginStart, marginEnd, sectionManager);
        }

        @Override
        public LayoutManager.LayoutParams processLayoutParams(
                LayoutManager.LayoutParams sectionParams) {
            LayoutParams lp = GridSLM.LayoutParams.from(super.processLayoutParams(sectionParams));

            lp.setNumColumns(numColumns);
            lp.setColumnWidth(columnWidth);

            return lp;
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

    class SubsectionColumnData {

        int childCount;

        int startIndex;

        int bottom;

        public SubsectionColumnData(int childCount, int startIndex) {
            this.childCount = childCount;
            this.startIndex = startIndex;
        }
    }
}
